package com.worldline.devview.networkmock.core.openapi

import com.worldline.devview.networkmock.core.NetworkMockResourceLoader
import com.worldline.devview.networkmock.core.model.ApiSpec
import com.worldline.devview.networkmock.core.model.Operation
import com.worldline.devview.networkmock.core.model.RequestBodyMatch
import kotlinx.serialization.json.Json

/**
 * A resolved response variant's actual content source: either a file on disk (an OpenAPI
 * example's `externalValue`) or an in-memory body synthesized once from a declared `schema`
 * when the status code has no `examples` (see [SchemaSynthesizer]).
 */
internal sealed interface ResponseContent {
    /** Loaded via [NetworkMockResourceLoader.load] at [path]. */
    data class FromFile(val path: String) : ResponseContent

    /** Already-serialized JSON text, produced once by [SchemaSynthesizer] and cached here. */
    data class Synthesized(val json: String) : ResponseContent
}

/**
 * A resolved response variant: its actual [content] (a file to load, or an already-synthesized
 * body), its declared media type, and any headers declared on the enclosing `responses.<code>` —
 * everything [com.worldline.devview.networkmock.core.repository.MockConfigRepository] needs
 * to build a [com.worldline.devview.networkmock.core.model.MockResponse] without touching an
 * OpenAPI-shaped type itself (see #73's pure-seam requirement).
 */
internal data class ResolvedResponse(
    val content: ResponseContent,
    val contentType: String,
    val headers: Map<String, String>
)

/**
 * Parses an OpenAPI 3.x document (JSON or YAML) into DevView's internal model.
 *
 * This is the only format-aware code in the library — see #73's pure-seam requirement.
 * [OpenApiDocument] and its nested types never leave this package; [parse] returns
 * [ParsedSpec], which carries only the public [ApiSpec] model plus a plain-collection
 * response index that [com.worldline.devview.networkmock.core.repository.MockConfigRepository]
 * uses for response-file discovery.
 *
 * ## Scope decisions (deliberate, not oversights)
 * - Query-parameter matching values come from a parameter's top-level `example` field only
 *   (not `schema.example`/`schema.default`) — see [ParameterObject].
 * - Response bodies are sourced from `examples.<name>.externalValue` first; a status code with
 *   declared `examples` never falls back to schema synthesis, even for a media type within that
 *   same status code that has a `schema` but no `examples` of its own. An example declared with
 *   an inline `value` is skipped, since this library keeps response bodies as external files
 *   (see the epic's format decisions).
 * - A status code with **no** `examples` at all but a declared `content.<mediaType>.schema`
 *   synthesizes one placeholder body per such media type instead of being unmockable — see
 *   [SchemaSynthesizer]. Deliberately narrow (not full JSON Schema conformance); see its KDoc.
 * - `$ref` and `externalValue` both resolve relative to the file that declares them, into
 *   `#/components/<section>/<name>` — a `$ref` chain (a component that itself points at
 *   another `$ref`) is followed until a non-ref entry is reached, guarded against cycles.
 *   Each hop's fragment must declare the section the caller expects (e.g. a response `$ref`
 *   must point into `components/responses`), so a same-named entry in a different section
 *   is never silently conflated with the one actually referenced.
 * - `requestBody` matching is deliberately narrow (see #83): only a
 *   `requestBody.content.<mediaType>.schema`'s `required` field list and, optionally, a single
 *   discriminator property's literal value (from that property's own single-value `enum`) are
 *   read into [RequestBodyMatch] — not full JSON Schema validation. An operation whose schema
 *   yields neither a required field nor a usable discriminator value has `requestBodyMatch ==
 *   null` (matches any body), same as an operation declaring no `requestBody` at all. Only one
 *   media type is read per `requestBody` (`application/json` if declared, otherwise whichever
 *   is declared first).
 * - `tags` (see #116) is read verbatim into [Operation.tags], purely a display/filter label for
 *   `devview-networkmock`'s UI (tag filter chips, "Tag" sort option) — it has no effect on
 *   request matching, same as [Operation.version].
 */
internal object OpenApiParser {
    /**
     * @property apiSpec The public model built from the document.
     * @property responseIndex `operationId -> statusCode -> exampleName -> `[ResolvedResponse].
     */
    data class ParsedSpec(
        val apiSpec: ApiSpec,
        val responseIndex: Map<String, Map<Int, Map<String, ResolvedResponse>>>
    )

    /**
     * Parses the OpenAPI document at [specPath].
     *
     * @param specPath Path to the spec file relative to composeResources, used both to load
     *   the document and as the base directory for resolving `$ref`/`externalValue` entries.
     * @param resourceLoader Loads the spec's own bytes and any externally referenced files.
     * @throws IllegalStateException if an operation is missing `operationId`, or a `$ref`
     *   cannot be resolved.
     */
    suspend fun parse(specPath: String, resourceLoader: NetworkMockResourceLoader): ParsedSpec {
        val context = ParseContext(resourceLoader = resourceLoader)
        val root = SourceDocument(
            path = specPath,
            document = decodeDocument(path = specPath, bytes = resourceLoader.load(path = specPath))
        )
        val document = root.document

        val operations = mutableListOf<Operation>()
        val responseIndex = mutableMapOf<String, Map<Int, Map<String, ResolvedResponse>>>()

        for ((path, pathItem) in document.paths) {
            for ((method, rawOperation) in pathItem.operationsByMethod()) {
                val operationId = rawOperation.operationId?.takeIf { it.isNotBlank() }
                    ?: error(
                        message = "OpenAPI operation $method $path in '$specPath' is missing " +
                            "a required operationId."
                    )

                val queryParameters = rawOperation.parameters
                    .map { context.resolveParameter(raw = it, source = root) }
                    .filter { it.`in` == "query" && it.example != null }
                    .associate { it.name to requireNotNull(value = it.example) }
                    .ifEmpty { null }

                operations += Operation(
                    operationId = operationId,
                    name = rawOperation.summary?.takeIf { it.isNotBlank() } ?: operationId,
                    path = path,
                    method = method,
                    queryParameters = queryParameters,
                    delayMs = rawOperation.xDevview?.delayMs,
                    version = versionPattern.find(input = path)?.groupValues?.get(index = 1),
                    failureRate = rawOperation.xDevview?.failureRate,
                    requestBodyMatch = context.buildRequestBodyMatch(
                        raw = rawOperation.requestBody,
                        source = root
                    ),
                    tags = rawOperation.tags
                )

                responseIndex[operationId] = context.resolveResponseIndex(
                    responses = rawOperation.responses,
                    source = root
                )
            }
        }

        val apiSpec = ApiSpec(
            id = slugify(title = document.info.title),
            name = document.info.title,
            servers = document.servers.map { it.url },
            operations = operations,
            delayMs = document.xDevview?.delayMs
        )

        return ParsedSpec(apiSpec = apiSpec, responseIndex = responseIndex)
    }

    private fun slugify(title: String): String = title
        .lowercase()
        .trim()
        .replace(regex = Regex(pattern = "[^a-z0-9]+"), replacement = "-")
        .trim(chars = charArrayOf('-'))

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * The example name a synthesized response body (see [SchemaSynthesizer]) is stored under —
     * `"default"`, matching this codebase's own convention for the primary/original response
     * for a status code (see [com.worldline.devview.networkmock.core.model.MockResponse.exampleName]).
     */
    @Suppress("DocumentationOverPrivateProperty")
    private const val SYNTHESIZED_EXAMPLE_NAME = "default"

    /**
     * The media type [ParseContext.buildRequestBodyMatch] prefers when a `requestBody` declares
     * more than one — this library assumes one dominant request content type per operation,
     * mirroring how [ParameterObject.example] models a single literal value rather than a
     * per-media-type one.
     */
    @Suppress("DocumentationOverPrivateProperty")
    private const val APPLICATION_JSON = "application/json"

    /**
     * Extracts a display-only `v{n}` version tag from a `/v{n}/` path segment (see
     * [Operation.version]). Not currently configurable — see the KDoc there.
     */
    @Suppress("DocumentationOverPrivateProperty")
    private val versionPattern = Regex(pattern = "/(v\\d+)(?=/|$)")

    /** Decodes [bytes] as JSON or YAML, sniffing the format from [path]'s extension. */
    @Suppress("DocumentationOverPrivateFunction")
    private fun decodeDocument(path: String, bytes: ByteArray): OpenApiDocument =
        if (isYaml(path = path, bytes = bytes)) {
            YamlSupport.decode(bytes = bytes)
        } else {
            json.decodeFromString(
                deserializer = OpenApiDocument.serializer(),
                string = bytes.decodeToString()
            )
        }

    private fun isYaml(path: String, bytes: ByteArray): Boolean = when {
        path.endsWith(suffix = ".yaml") || path.endsWith(suffix = ".yml") -> true
        path.endsWith(suffix = ".json") -> false
        else -> bytes.decodeToString().trimStart().firstOrNull() != '{'
    }

    /**
     * A decoded document together with the [path] it was loaded from. Every `$ref` and
     * `externalValue` inside it is relative to that path's directory ([baseDir]), not to the
     * root spec's.
     */
    private data class SourceDocument(val path: String, val document: OpenApiDocument) {
        val baseDir: String = path.substringBeforeLast(delimiter = "/", missingDelimiterValue = "")
    }

    /** A resolved `$ref` target plus the [source] document it was found in (for nested relative refs). */
    private data class Resolved<T>(val value: T, val source: SourceDocument)

    /** Per-[parse]-call state: resource loading and the external-document cache. */
    private class ParseContext(private val resourceLoader: NetworkMockResourceLoader) {
        private val externalDocuments = mutableMapOf<String, SourceDocument>()

        suspend fun resolveParameter(
            raw: ParameterObject,
            source: SourceDocument
        ): ParameterObject {
            val ref = raw.ref ?: return raw
            return resolveRef(
                ref = ref,
                source = source,
                section = "parameters",
                componentsOf = { it.components.parameters },
                refOf = { it.ref }
            ).value
        }

        suspend fun resolveResponseIndex(
            responses: Map<String, ResponseObject>,
            source: SourceDocument
        ): Map<Int, Map<String, ResolvedResponse>> {
            val result = mutableMapOf<Int, Map<String, ResolvedResponse>>()
            for ((codeText, rawResponse) in responses) {
                val statusCode = codeText.toIntOrNull() ?: continue
                val resolvedResponse = if (rawResponse.ref != null) {
                    resolveRef(
                        ref = rawResponse.ref,
                        source = source,
                        section = "responses",
                        componentsOf = { it.components.responses },
                        refOf = { it.ref }
                    )
                } else {
                    Resolved(value = rawResponse, source = source)
                }
                val response = resolvedResponse.value
                val responseSource = resolvedResponse.source

                val headers = resolveHeaders(raw = response.headers, source = responseSource)
                val examplesForCode = mutableMapOf<String, ResolvedResponse>()
                for ((mediaType, media) in response.content) {
                    examplesForCode += resolveMediaTypeResponses(
                        mediaType = mediaType,
                        media = media,
                        source = responseSource,
                        headers = headers
                    )
                }
                if (examplesForCode.isNotEmpty()) {
                    result[statusCode] = examplesForCode
                }
            }
            return result
        }

        /**
         * Resolves every declared `examples.<name>` for [mediaType], falling back to one
         * schema-synthesized body (see [SchemaSynthesizer], stored under [SYNTHESIZED_EXAMPLE_NAME])
         * when [media] declares no examples of its own but does declare a `schema`.
         */
        @Suppress("DocumentationOverPrivateFunction")
        private suspend fun resolveMediaTypeResponses(
            mediaType: String,
            media: MediaTypeObject,
            source: SourceDocument,
            headers: Map<String, String>
        ): Map<String, ResolvedResponse> {
            val resolved = mutableMapOf<String, ResolvedResponse>()
            for ((exampleName, rawExample) in media.examples) {
                val resolvedExample = if (rawExample.ref != null) {
                    resolveRef(
                        ref = rawExample.ref,
                        source = source,
                        section = "examples",
                        componentsOf = { it.components.examples },
                        refOf = { it.ref }
                    )
                } else {
                    Resolved(value = rawExample, source = source)
                }
                val externalValue = resolvedExample.value.externalValue ?: continue
                resolved[exampleName] = ResolvedResponse(
                    content = ResponseContent.FromFile(
                        path = resolvePath(
                            baseDir = resolvedExample.source.baseDir,
                            ref = externalValue
                        )
                    ),
                    contentType = mediaType,
                    headers = headers
                )
            }

            val schema = media.schema
            if (resolved.isEmpty() && schema != null) {
                val synthesized = SchemaSynthesizer.synthesize(
                    schema = schema,
                    resolver = schemaResolver(source = source)
                )
                resolved[SYNTHESIZED_EXAMPLE_NAME] = ResolvedResponse(
                    content = ResponseContent.Synthesized(json = synthesized.toString()),
                    contentType = mediaType,
                    headers = headers
                )
            }
            return resolved
        }

        /**
         * A [SchemaResolver] scoped to [source]: follows a schema's `$ref` via [resolveRef]
         * against `components.schemas`, then re-scopes to the document the target was found in so
         * `$ref`s nested inside it resolve against that document.
         */
        @Suppress("DocumentationOverPrivateFunction")
        private fun schemaResolver(source: SourceDocument): SchemaResolver =
            object : SchemaResolver {
                override suspend fun resolve(
                    schema: SchemaObject
                ): Pair<SchemaObject, SchemaResolver> {
                    val ref = schema.ref ?: return schema to this
                    val resolved = resolveRef(
                        ref = ref,
                        source = source,
                        section = "schemas",
                        componentsOf = { it.components.schemas },
                        refOf = { it.ref }
                    )
                    return resolved.value to schemaResolver(source = resolved.source)
                }
            }

        /** Resolves a `requestBody`'s own `$ref` (if any) via [resolveRef] against `components.requestBodies`. */
        @Suppress("DocumentationOverPrivateFunction")
        private suspend fun resolveRequestBody(
            raw: RequestBodyObject,
            source: SourceDocument
        ): Resolved<RequestBodyObject> {
            val ref = raw.ref ?: return Resolved(value = raw, source = source)
            return resolveRef(
                ref = ref,
                source = source,
                section = "requestBodies",
                componentsOf = { it.components.requestBodies },
                refOf = { it.ref }
            )
        }

        /**
         * Builds the [RequestBodyMatch] for an operation's [RequestBodyObject], or `null` if
         * the operation declares no `requestBody`, its chosen media type (see [APPLICATION_JSON])
         * has no `schema`, or the resolved schema yields nothing to check — no `required` fields
         * and no usable discriminator (see [RequestBodyMatch]'s KDoc for what "usable" means).
         */
        @Suppress("DocumentationOverPrivateFunction")
        suspend fun buildRequestBodyMatch(
            raw: RequestBodyObject?,
            source: SourceDocument
        ): RequestBodyMatch? {
            val resolvedBody = raw?.let { resolveRequestBody(raw = it, source = source) }
                ?: return null
            val requestBody = resolvedBody.value
            val rawSchema =
                (requestBody.content[APPLICATION_JSON] ?: requestBody.content.values.firstOrNull())
                    ?.schema
                    ?: return null
            // The schema's own $ref resolves against the document the requestBody was found in.
            val (schema, _) = schemaResolver(source = resolvedBody.source)
                .resolve(schema = rawSchema)

            val requiredFields = schema.required.orEmpty()
            val discriminatorField = schema.discriminator?.propertyName?.takeIf { it.isNotBlank() }
            val discriminatorValue = discriminatorField
                ?.let { field ->
                    schema.properties
                        ?.get(key = field)
                        ?.enum
                        ?.firstOrNull()
                }

            return if (requiredFields.isEmpty() && discriminatorField == null) {
                null
            } else {
                RequestBodyMatch(
                    requiredFields = requiredFields,
                    discriminatorField = discriminatorField,
                    discriminatorValue = discriminatorValue
                )
            }
        }

        /** Resolves each declared header's `$ref` (if any) down to its literal `example` value. */
        @Suppress("DocumentationOverPrivateFunction")
        private suspend fun resolveHeaders(
            raw: Map<String, HeaderObject>,
            source: SourceDocument
        ): Map<String, String> = raw
            .mapNotNull { (name, rawHeader) ->
                val header = if (rawHeader.ref != null) {
                    resolveRef(
                        ref = rawHeader.ref,
                        source = source,
                        section = "headers",
                        componentsOf = { it.components.headers },
                        refOf = { it.ref }
                    ).value
                } else {
                    rawHeader
                }
                header.example?.let { name to it }
            }.toMap()

        /**
         * Resolves a `$ref` string to its target, either locally (within [source]) or in
         * another file, following a chain of `$ref`s — an entry that itself declares a `$ref`
         * is resolved again — until a non-ref entry is reached. Each hop's file path is resolved
         * relative to the document holding that `$ref`, and the returned [Resolved.source] is the
         * document the final entry lives in.
         *
         * Every fragment must be exactly `components/<section>/<name>`, where [section] is the
         * `components.<section>` key (e.g. `"responses"`); a fragment with a different structure
         * or naming an unexpected section is rejected, so a same-named entry in a different
         * section is never silently conflated with the one actually referenced.
         * [componentsOf] selects the matching `components.<section>` map from a document, and
         * [refOf] extracts a resolved entry's own `$ref` (if any) so the chain can continue.
         *
         * @throws IllegalStateException if a `$ref` cannot be resolved, has an invalid fragment
         *   structure, names an unexpected section, or the chain revisits a `(path, fragment)`
         *   pair already seen (a cycle).
         */
        @Suppress("DocumentationOverPrivateFunction")
        private suspend fun <T> resolveRef(
            ref: String,
            source: SourceDocument,
            section: String,
            componentsOf: (OpenApiDocument) -> Map<String, T>,
            refOf: (T) -> String?
        ): Resolved<T> {
            val visited = mutableSetOf<Pair<String, String>>()
            var currentSource = source
            var currentRef = ref

            while (true) {
                val (target, fragment) = locate(ref = currentRef, source = currentSource)

                if (!visited.add(element = target.path to fragment)) {
                    error(
                        message = "Unresolvable \$ref '$ref': cyclic reference detected — " +
                            "'$currentRef' revisits an already-resolved fragment."
                    )
                }

                val segments = fragment.split("/")
                if (segments.size != 3 || segments[0] != "components") {
                    error(
                        message =
                            "Unresolvable \$ref '$currentRef': expected a fragment of the form " +
                                "'components/$section/<name>' but got '$fragment'."
                    )
                }
                val actualSection = segments[1]
                val name = segments[2]
                if (actualSection != section) {
                    error(
                        message = "Unresolvable \$ref '$currentRef': expected a '$section' entry " +
                            "but the fragment points into '$actualSection'."
                    )
                }

                val entry = componentsOf(target.document)[name]
                    ?: error(
                        message = "Unresolvable \$ref '$currentRef': no such entry in components.$section."
                    )

                val nestedRef = refOf(entry) ?: return Resolved(value = entry, source = target)
                currentSource = target
                currentRef = nestedRef
            }
        }

        /** Splits [ref] into the document it targets and its fragment, loading an external file if needed. */
        @Suppress("DocumentationOverPrivateFunction")
        private suspend fun locate(
            ref: String,
            source: SourceDocument
        ): Pair<SourceDocument, String> = if (ref.startsWith(prefix = "#/")) {
            source to ref.removePrefix(prefix = "#/")
        } else {
            val filePath = ref.substringBefore(delimiter = "#")
            val fragment = ref
                .substringAfter(delimiter = "#", missingDelimiterValue = "")
                .removePrefix(prefix = "/")
            loadExternalDocument(from = source, filePath = filePath) to fragment
        }

        /** Loads [filePath] relative to the directory of [from], the document that references it. */
        @Suppress("DocumentationOverPrivateFunction")
        private suspend fun loadExternalDocument(
            from: SourceDocument,
            filePath: String
        ): SourceDocument {
            val resolvedPath = resolvePath(baseDir = from.baseDir, ref = filePath)
            return externalDocuments.getOrPut(key = resolvedPath) {
                SourceDocument(
                    path = resolvedPath,
                    document = decodeDocument(
                        path = resolvedPath,
                        bytes = resourceLoader.load(path = resolvedPath)
                    )
                )
            }
        }
    }
}

/**
 * Resolves a `$ref`/`externalValue` reference relative to [baseDir], normalizing `.`/`..`
 * segments. A reference starting with `/` is root-relative (relative to composeResources);
 * anything else is joined against [baseDir], matching how `$ref` and `externalValue` are
 * resolved relative to their containing document in standard OpenAPI tooling.
 */
internal fun resolvePath(baseDir: String, ref: String): String {
    val combined = when {
        ref.startsWith(prefix = "/") -> ref.removePrefix(prefix = "/")
        baseDir.isEmpty() -> ref
        else -> "$baseDir/$ref"
    }
    val segments = mutableListOf<String>()
    for (segment in combined.split("/")) {
        when (segment) {
            "", "." -> Unit
            ".." -> if (segments.isNotEmpty()) segments.removeAt(index = segments.size - 1)
            else -> segments.add(element = segment)
        }
    }
    return segments.joinToString(separator = "/")
}
