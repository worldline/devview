package com.worldline.devview.networkmock.core.openapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Minimal OpenAPI 3.x document model — only the subset this library reads.
 *
 * These types are `internal` by design (see #73's pure-seam requirement): nothing outside
 * the `openapi` package and [com.worldline.devview.networkmock.core.repository.MockConfigRepository]
 * may reference them. [OpenApiParser] converts a parsed [OpenApiDocument] into the public
 * [com.worldline.devview.networkmock.core.model.ApiSpec] / [com.worldline.devview.networkmock.core.model.Operation]
 * model before returning.
 *
 * Every `$ref`-capable object ([ParameterObject], [ResponseObject], [ExampleObject]) carries
 * its own nullable `ref` field alongside its regular fields, rather than being wrapped in a
 * polymorphic union. This is deliberate: it lets the exact same data classes decode uniformly
 * whether the source document is JSON (`kotlinx.serialization.json.Json`) or YAML (kaml's
 * `Yaml`), since a tagged/polymorphic wrapper would need format-specific serializer support
 * that kaml does not provide for `kotlinx.serialization.json.JsonElement`-shaped values.
 *
 * Only fields consumed by [OpenApiParser] are modeled. Everything else in a real spec
 * (`deprecated`, `tags`, `security`, request bodies, schemas, …) is silently ignored via
 * lenient/non-strict decoding — this parser mocks, it does not validate.
 */
@Serializable
internal data class OpenApiDocument(
    val info: InfoObject = InfoObject(),
    val servers: List<ServerObject> = emptyList(),
    val paths: Map<String, PathItemObject> = emptyMap(),
    val components: ComponentsObject = ComponentsObject(),
    @SerialName("x-devview") val xDevview: DevViewExtension? = null
)

@Serializable
internal data class InfoObject(val title: String = "")

@Serializable
internal data class ServerObject(val url: String = "")

/**
 * One entry in `paths.<path>`. OpenAPI declares HTTP methods as fixed sibling keys rather
 * than a generic map, so this mirrors that shape directly instead of a `Map<String, OperationObject>`.
 */
@Serializable
internal data class PathItemObject(
    val get: OperationObject? = null,
    val put: OperationObject? = null,
    val post: OperationObject? = null,
    val delete: OperationObject? = null,
    val patch: OperationObject? = null,
    val options: OperationObject? = null,
    val head: OperationObject? = null
) {
    /** The declared operations on this path, paired with their uppercase HTTP method name. */
    fun operationsByMethod(): List<Pair<String, OperationObject>> = listOfNotNull(
        get?.let { "GET" to it },
        put?.let { "PUT" to it },
        post?.let { "POST" to it },
        delete?.let { "DELETE" to it },
        patch?.let { "PATCH" to it },
        options?.let { "OPTIONS" to it },
        head?.let { "HEAD" to it }
    )
}

@Serializable
internal data class OperationObject(
    val operationId: String? = null,
    val summary: String? = null,
    val parameters: List<ParameterObject> = emptyList(),
    val responses: Map<String, ResponseObject> = emptyMap(),
    @SerialName("x-devview") val xDevview: DevViewExtension? = null
)

/**
 * A query/path/header parameter declaration, or a `$ref` to one under `components.parameters`.
 *
 * [example] is the only field this parser reads for request matching, and is deliberately a
 * plain [String] rather than a `schema`-nested, dynamically-typed value — query string values
 * are always text, so a spec author declares the value to match as a quoted string
 * (`example: "user"`), not a nested schema.
 */
@Serializable
internal data class ParameterObject(
    @SerialName("\$ref") val ref: String? = null,
    val name: String = "",
    val `in`: String = "",
    val example: String? = null
)

/** A response declaration for one status code, or a `$ref` to one under `components.responses`. */
@Serializable
internal data class ResponseObject(
    @SerialName("\$ref") val ref: String? = null,
    val content: Map<String, MediaTypeObject> = emptyMap()
)

@Serializable
internal data class MediaTypeObject(val examples: Map<String, ExampleObject> = emptyMap())

/**
 * A named response example, or a `$ref` to one under `components.examples`.
 *
 * Only [externalValue] is modeled — this library sources response bodies exclusively from
 * files on disk (see the epic's decision to keep response bodies as external files). An
 * example declared with an inline `value` instead of `externalValue` is skipped; see
 * [OpenApiParser].
 */
@Serializable
internal data class ExampleObject(
    @SerialName("\$ref") val ref: String? = null,
    val externalValue: String? = null
)

@Serializable
internal data class ComponentsObject(
    val parameters: Map<String, ParameterObject> = emptyMap(),
    val responses: Map<String, ResponseObject> = emptyMap(),
    val examples: Map<String, ExampleObject> = emptyMap()
)

/**
 * The `x-devview` Specification Extension object (see #94). Read at both the document root
 * (spec-wide default delay) and per-operation (overrides the document default).
 */
@Serializable
internal data class DevViewExtension(val delayMs: Long? = null)
