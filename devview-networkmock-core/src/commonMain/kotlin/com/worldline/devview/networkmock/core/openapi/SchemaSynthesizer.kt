package com.worldline.devview.networkmock.core.openapi

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

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
     * Synthesizes one plausible [JsonElement] for [schema], resolving `$ref`s via [resolveSchema]
     * (typically [OpenApiParser]'s own ref-resolution, reused rather than duplicated — see
     * `ParseContext.resolveSchema`) wherever a nested schema is encountered.
     *
     * Resolution order once `$ref` is resolved: `allOf` (merge), then `oneOf` (first variant),
     * then `enum` (first value), then `object`/`array` shape (by declared `type` or by the mere
     * presence of `properties`/`items`), then primitive `type`s. [SchemaObject.nullable] is
     * ignored — a real value is always produced, never a JSON `null`; this library mocks
     * responses, it doesn't exercise null-handling.
     *
     * @param schema The schema to synthesize a value for.
     * @param resolveSchema Resolves a schema's own `$ref` (if any) to its target; returns the
     *   schema unchanged when it has none.
     * @throws IllegalStateException if [schema] declares an `allOf` with conflicting property
     *   definitions across members, an `array` with no `items`, a `oneOf` with no variants, or
     *   a shape this function doesn't recognize (no `type`/`properties`/`items`/`enum`/`allOf`/
     *   `oneOf`, or an unsupported `type` string).
     *
     * `NamedArguments` is suppressed below because [resolveSchema] is a function-type parameter —
     * invoking it (`resolveSchema(schema)`) calls `Function1.invoke`, whose single parameter has
     * no name to reference, the same class of exception as `NetworkMockPlugin`'s `IOException` one.
     */
    @Suppress("NamedArguments")
    suspend fun synthesize(
        schema: SchemaObject,
        resolveSchema: suspend (SchemaObject) -> SchemaObject
    ): JsonElement {
        val resolved = resolveSchema(schema)
        return when {
            resolved.allOf != null -> synthesizeAllOf(
                members = resolved.allOf,
                resolveSchema = resolveSchema
            )
            resolved.oneOf != null -> synthesizeOneOf(
                members = resolved.oneOf,
                resolveSchema = resolveSchema
            )
            !resolved.enum.isNullOrEmpty() -> JsonPrimitive(resolved.enum.first())
            resolved.type == "object" || resolved.properties != null ->
                synthesizeObject(schema = resolved, resolveSchema = resolveSchema)
            resolved.type == "array" || resolved.items != null ->
                synthesizeArray(schema = resolved, resolveSchema = resolveSchema)
            resolved.type == "string" -> JsonPrimitive("string")
            resolved.type == "integer" || resolved.type == "number" -> JsonPrimitive(0)
            resolved.type == "boolean" -> JsonPrimitive(false)
            else -> error(
                message =
                    "Cannot synthesize a response body for schema (type='${resolved.type}'): " +
                        "no enum/object/array/primitive shape declared."
            )
        }
    }

    private suspend fun synthesizeObject(
        schema: SchemaObject,
        resolveSchema: suspend (SchemaObject) -> SchemaObject
    ): JsonElement = buildJsonObject {
        schema.properties?.forEach { (name, propertySchema) ->
            put(
                key = name,
                element = synthesize(schema = propertySchema, resolveSchema = resolveSchema)
            )
        }
    }

    private suspend fun synthesizeArray(
        schema: SchemaObject,
        resolveSchema: suspend (SchemaObject) -> SchemaObject
    ): JsonElement {
        val items = schema.items
            ?: error(
                message = "Cannot synthesize an array response body: schema declares no 'items'."
            )
        return buildJsonArray {
            add(element = synthesize(schema = items, resolveSchema = resolveSchema))
        }
    }

    /**
     * Merges every member's [SchemaObject.properties] into one effective object schema. Two
     * members declaring the *same* property with *different* schemas is a spec authoring error
     * this function refuses to guess through — see the thrown message.
     */
    @Suppress("DocumentationOverPrivateFunction", "NamedArguments")
    private suspend fun synthesizeAllOf(
        members: List<SchemaObject>,
        resolveSchema: suspend (SchemaObject) -> SchemaObject
    ): JsonElement {
        val merged = mutableMapOf<String, SchemaObject>()
        for (member in members) {
            val resolvedMember = resolveSchema(member)
            for ((name, propertySchema) in resolvedMember.properties.orEmpty()) {
                val existing = merged[name]
                if (existing != null && existing != propertySchema) {
                    error(
                        message =
                            "Cannot merge allOf: conflicting definitions for property '$name' " +
                                "across members."
                    )
                }
                merged[name] = propertySchema
            }
        }
        return buildJsonObject {
            for ((name, propertySchema) in merged) {
                put(
                    key = name,
                    element = synthesize(schema = propertySchema, resolveSchema = resolveSchema)
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
        resolveSchema: suspend (SchemaObject) -> SchemaObject
    ): JsonElement {
        val first = members.firstOrNull()
            ?: error(message = "Cannot synthesize a oneOf response body: no variants declared.")
        return synthesize(schema = first, resolveSchema = resolveSchema)
    }
}
