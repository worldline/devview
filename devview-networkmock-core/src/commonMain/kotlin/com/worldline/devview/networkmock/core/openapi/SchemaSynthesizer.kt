package com.worldline.devview.networkmock.core.openapi

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

/**
 * Follows a schema's own `$ref` while remembering which document it was found in, so a `$ref`
 * nested inside the resolved schema is resolved against *that* document rather than the one the
 * caller started from. Implemented by [OpenApiParser]'s `ParseContext`.
 */
internal interface SchemaResolver {
    /**
     * Returns [schema] with its own `$ref` (if any) followed to its target, paired with the
     * resolver to use for every schema nested inside that target. A schema without a `$ref` is
     * returned unchanged, paired with this same resolver.
     */
    suspend fun resolve(schema: SchemaObject): Pair<SchemaObject, SchemaResolver>
}

/**
 * Synthesizes a placeholder response body from a [SchemaObject] — used by [OpenApiParser] when
 * an operation's `responses.<code>.content.<mediaType>` declares a `schema` but no `examples`,
 * so that operation isn't left with zero mockable variants (see #82/#84).
 *
 * This is deliberately narrow, not full JSON Schema synthesis: primitives, `enum`, `object`,
 * `array`, `allOf` (merged), and `oneOf` (first variant) — see [synthesize]'s KDoc for the exact
 * rules per shape. Anything outside that (e.g. a schema with none of `type`/`properties`/`items`/
 * `enum`/`allOf`/`oneOf` declared) is a clear [IllegalStateException], not a guess.
 */
internal object SchemaSynthesizer {
    /**
     * Synthesizes one plausible [JsonElement] for [schema], resolving `$ref`s via [resolver]
     * (typically [OpenApiParser]'s own ref-resolution, reused rather than duplicated — see
     * `ParseContext.schemaResolver`) wherever a nested schema is encountered.
     *
     * Resolution order once `$ref` is resolved: `allOf` (merge), then `oneOf` (first variant),
     * then `enum` (first value), then `object`/`array` shape (by declared `type` or by the mere
     * presence of `properties`/`items`), then primitive `type`s. [SchemaObject.nullable] is
     * ignored — a real value is always produced, never a JSON `null`; this library mocks
     * responses, it doesn't exercise null-handling.
     *
     * @param schema The schema to synthesize a value for.
     * @param resolver Resolves a schema's own `$ref` (if any) to its target, and scopes the
     *   resolution of everything nested inside that target to the document it was found in.
     * @throws IllegalStateException if [schema] declares an `allOf` with conflicting property
     *   definitions across members, an `array` with no `items`, a `oneOf` with no variants, or
     *   a shape this function doesn't recognize (no `type`/`properties`/`items`/`enum`/`allOf`/
     *   `oneOf`, or an unsupported `type` string).
     */
    suspend fun synthesize(schema: SchemaObject, resolver: SchemaResolver): JsonElement {
        val (resolved, scoped) = resolver.resolve(schema = schema)
        return when {
            resolved.allOf != null -> synthesizeAllOf(members = resolved.allOf, resolver = scoped)
            resolved.oneOf != null -> synthesizeOneOf(members = resolved.oneOf, resolver = scoped)
            !resolved.enum.isNullOrEmpty() -> JsonPrimitive(value = resolved.enum.first())
            resolved.type == "object" || resolved.properties != null ->
                synthesizeObject(schema = resolved, resolver = scoped)
            resolved.type == "array" || resolved.items != null ->
                synthesizeArray(schema = resolved, resolver = scoped)
            resolved.type == "string" -> JsonPrimitive(value = "string")
            resolved.type == "integer" || resolved.type == "number" -> JsonPrimitive(value = 0)
            resolved.type == "boolean" -> JsonPrimitive(value = false)
            else -> error(
                message =
                    "Cannot synthesize a response body for schema (type='${resolved.type}'): " +
                        "no enum/object/array/primitive shape declared."
            )
        }
    }

    private suspend fun synthesizeObject(
        schema: SchemaObject,
        resolver: SchemaResolver
    ): JsonElement = buildJsonObject {
        schema.properties?.forEach { (name, propertySchema) ->
            put(
                key = name,
                element = synthesize(schema = propertySchema, resolver = resolver)
            )
        }
    }

    private suspend fun synthesizeArray(
        schema: SchemaObject,
        resolver: SchemaResolver
    ): JsonElement {
        val items = schema.items
            ?: error(
                message = "Cannot synthesize an array response body: schema declares no 'items'."
            )
        return buildJsonArray {
            add(element = synthesize(schema = items, resolver = resolver))
        }
    }

    /**
     * Merges every member's [SchemaObject.properties] into one effective object schema. Two
     * members declaring the *same* property with *different* schemas is a spec authoring error
     * this function refuses to guess through — see the thrown message. Each merged property
     * keeps the resolver of the member it came from, since members may live in different
     * documents.
     */
    @Suppress("DocumentationOverPrivateFunction")
    private suspend fun synthesizeAllOf(
        members: List<SchemaObject>,
        resolver: SchemaResolver
    ): JsonElement {
        val merged = mutableMapOf<String, Pair<SchemaObject, SchemaResolver>>()
        for (member in members) {
            val (resolvedMember, memberResolver) = resolver.resolve(schema = member)
            for ((name, propertySchema) in resolvedMember.properties.orEmpty()) {
                val existing = merged[name]
                if (existing == null) {
                    merged[name] = propertySchema to memberResolver
                } else if (existing.first != propertySchema) {
                    error(
                        message =
                            "Cannot merge allOf: conflicting definitions for property '$name' " +
                                "across members."
                    )
                }
            }
        }
        return buildJsonObject {
            for ((name, entry) in merged) {
                val (propertySchema, propertyResolver) = entry
                put(
                    key = name,
                    element = synthesize(schema = propertySchema, resolver = propertyResolver)
                )
            }
        }
    }

    /**
     * Synthesizes the first declared `oneOf` variant, regardless of [SchemaObject.discriminator]
     * — see [SchemaObject.discriminator]'s KDoc for why a discriminator can't currently steer
     * variant selection at spec-parse time.
     */
    @Suppress("DocumentationOverPrivateFunction")
    private suspend fun synthesizeOneOf(
        members: List<SchemaObject>,
        resolver: SchemaResolver
    ): JsonElement {
        val first = members.firstOrNull()
            ?: error(message = "Cannot synthesize a oneOf response body: no variants declared.")
        return synthesize(schema = first, resolver = resolver)
    }
}
