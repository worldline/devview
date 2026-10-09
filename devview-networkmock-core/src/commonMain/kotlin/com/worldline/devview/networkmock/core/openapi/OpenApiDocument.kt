package com.worldline.devview.networkmock.core.openapi

import com.worldline.devview.networkmock.core.model.HttpMethod
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
 * (`deprecated`, `security`, …) is silently ignored via lenient/non-strict decoding — this
 * parser mocks, it does not validate. [SchemaObject] is the one exception, read in two
 * narrow ways: to *synthesize* a response body when a spec declares no `examples` for a status
 * code (see [SchemaSynthesizer]), and to build a [RequestBodyObject]'s match constraints (see
 * [OpenApiParser]'s request-body matching scope decision) — neither is full validation.
 * `tags` (see [OperationObject.tags]) is also read, purely as a display/filter label for
 * `devview-networkmock`'s UI — like [ParameterObject.example], it has no effect on request
 * matching.
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
    /** The declared operations on this path, paired with their [HttpMethod]. */
    fun operationsByMethod(): List<Pair<HttpMethod, OperationObject>> = listOfNotNull(
        get?.let { HttpMethod.Get to it },
        put?.let { HttpMethod.Put to it },
        post?.let { HttpMethod.Post to it },
        delete?.let { HttpMethod.Delete to it },
        patch?.let { HttpMethod.Patch to it },
        options?.let { HttpMethod.Options to it },
        head?.let { HttpMethod.Head to it }
    )
}

@Serializable
internal data class OperationObject(
    val operationId: String? = null,
    val summary: String? = null,
    val tags: List<String> = emptyList(),
    val parameters: List<ParameterObject> = emptyList(),
    val requestBody: RequestBodyObject? = null,
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
    val content: Map<String, MediaTypeObject> = emptyMap(),
    val headers: Map<String, HeaderObject> = emptyMap()
)

/**
 * A response header declaration, or a `$ref` to one under `components.headers`.
 *
 * [example] is the only field this parser reads — the literal value served as the header's
 * value — mirroring how [ParameterObject.example] is read for query parameters rather than a
 * `schema`-nested value.
 */
@Serializable
internal data class HeaderObject(
    @SerialName("\$ref") val ref: String? = null,
    val example: String? = null
)

@Serializable
internal data class MediaTypeObject(
    val examples: Map<String, ExampleObject> = emptyMap(),
    val schema: SchemaObject? = null
)

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

/**
 * A `requestBody` declaration for an operation, or a `$ref` to one under
 * `components.requestBodies`.
 *
 * Only [content] is modeled — read for its `<mediaType>.schema`, and only to build the
 * declaring operation's [com.worldline.devview.networkmock.core.model.RequestBodyMatch] (see
 * [OpenApiParser]'s request-body matching scope decision). No other `requestBody` field
 * (`description`, `required`) is read.
 */
@Serializable
internal data class RequestBodyObject(
    @SerialName("\$ref") val ref: String? = null,
    val content: Map<String, MediaTypeObject> = emptyMap()
)

/**
 * A JSON Schema (OpenAPI's constrained subset of it) declaration, or a `$ref` to one under
 * `components.schemas`. Read in two narrow, non-validating ways: to synthesize a placeholder
 * response body when a `content.<mediaType>` declares a [schema] but no `examples` (see
 * [SchemaSynthesizer]), and to build a [RequestBodyObject]'s
 * [com.worldline.devview.networkmock.core.model.RequestBodyMatch] (see [OpenApiParser]'s
 * request-body matching scope decision).
 *
 * Deliberately not a full JSON Schema model: no `additionalProperties`, `minimum`/`maximum`,
 * string patterns, etc. — anything that would matter for *validation* rather than *synthesizing
 * one plausible value* or checking narrow request-body match constraints.
 *
 * @property type The schema's declared type (`"string"`, `"integer"`, `"number"`, `"boolean"`,
 *   `"object"`, or `"array"`). May be absent when [properties] or [items] alone implies it.
 * @property enum If non-empty, [SchemaSynthesizer] uses the first declared value verbatim
 *   instead of a generic placeholder for [type] `"string"`.
 * @property properties For `type: object` (or when present at all, regardless of [type]):
 *   each property's own schema, synthesized recursively.
 * @property items For `type: array` (or when present at all, regardless of [type]): the
 *   schema of a single array element — [SchemaSynthesizer] produces a one-element array.
 * @property nullable Read but not acted on: [SchemaSynthesizer] always synthesizes a real
 *   value, even for a nullable schema — this library mocks, it does not test null-handling.
 * @property format Read but not currently used by [SchemaSynthesizer] — reserved for a future
 *   format-aware placeholder (e.g. `"date-time"`, `"uuid"`).
 * @property allOf Member schemas merged into one effective object schema — see
 *   [SchemaSynthesizer] for the conflicting-property-definition error case.
 * @property oneOf Alternative schemas; [SchemaSynthesizer] synthesizes the first declared
 *   variant regardless of [discriminator] (see [DiscriminatorObject]'s KDoc for why).
 * @property discriminator Parsed but not currently used to select a `oneOf` variant — there is
 *   no concrete request/response data at spec-parse time to disambiguate against. Read for
 *   request-body matching, though (see [required]): its [DiscriminatorObject.propertyName]
 *   becomes a [com.worldline.devview.networkmock.core.model.RequestBodyMatch.discriminatorField].
 * @property required Property names a request-body schema declares as required, read only to
 *   build [com.worldline.devview.networkmock.core.model.RequestBodyMatch.requiredFields] —
 *   [SchemaSynthesizer] ignores this entirely, a synthesized response always includes every
 *   [properties] entry regardless of whether it's "required".
 */
@Serializable
internal data class SchemaObject(
    @SerialName("\$ref") val ref: String? = null,
    val type: String? = null,
    val enum: List<String>? = null,
    val properties: Map<String, SchemaObject>? = null,
    val items: SchemaObject? = null,
    val nullable: Boolean? = null,
    val format: String? = null,
    val allOf: List<SchemaObject>? = null,
    val oneOf: List<SchemaObject>? = null,
    val discriminator: DiscriminatorObject? = null,
    val required: List<String>? = null
)

/**
 * A `oneOf` discriminator declaration — identifies which property carries the type tag, and
 * optionally maps its values to explicit `components.schemas` names. See [SchemaObject.discriminator].
 */
@Serializable
internal data class DiscriminatorObject(
    val propertyName: String = "",
    val mapping: Map<String, String>? = null
)

@Serializable
internal data class ComponentsObject(
    val parameters: Map<String, ParameterObject> = emptyMap(),
    val responses: Map<String, ResponseObject> = emptyMap(),
    val examples: Map<String, ExampleObject> = emptyMap(),
    val headers: Map<String, HeaderObject> = emptyMap(),
    val schemas: Map<String, SchemaObject> = emptyMap(),
    val requestBodies: Map<String, RequestBodyObject> = emptyMap()
)

/**
 * The `x-devview` Specification Extension object (see #94). [delayMs] is read at both the
 * document root (spec-wide default delay) and per-operation (overrides the document default).
 * [failureRate] is operation-level only (see [com.worldline.devview.networkmock.core.model.Operation.failureRate]) —
 * it is ignored if declared at the document root.
 */
@Serializable
internal data class DevViewExtension(val delayMs: Long? = null, val failureRate: Double? = null)
