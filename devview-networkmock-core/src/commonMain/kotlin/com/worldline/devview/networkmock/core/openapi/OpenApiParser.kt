package com.worldline.devview.networkmock.core.openapi

import com.worldline.devview.networkmock.core.NetworkMockResourceLoader
import com.worldline.devview.networkmock.core.model.ApiSpec
import com.worldline.devview.networkmock.core.model.Operation
import kotlinx.serialization.json.Json

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
 * - Response bodies are sourced only from `examples.<name>.externalValue`; an example
 *   declared with an inline `value` is skipped, since this library keeps response bodies as
 *   external files (see the epic's format decisions).
 * - `$ref` and `externalValue` both resolve relative to the file that declares them, exactly
 *   one level deep, into `#/components/<kind>/<name>` — a `$ref` chain (a component that
 *   itself points at another `$ref`) is not followed.
 * - No schema resolution of any kind — this parser mocks, it does not validate or synthesize
 *   bodies (see #82/#83/#84, explicitly out of scope for 0.2.0).
 */
internal object OpenApiParser {
    /**
     * @property apiSpec The public model built from the document.
     * @property responseIndex `operationId -> statusCode -> exampleName -> resolved
     *   externalValue path`, ready to pass straight to [NetworkMockResourceLoader.load].
     */
    data class ParsedSpec(
        val apiSpec: ApiSpec,
        val responseIndex: Map<String, Map<Int, Map<String, String>>>
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
        val baseDir = specPath.substringBeforeLast(delimiter = "/", missingDelimiterValue = "")
        val context = ParseContext(baseDir = baseDir, resourceLoader = resourceLoader)
        val document = decodeDocument(path = specPath, bytes = resourceLoader.load(path = specPath))

        val operations = mutableListOf<Operation>()
        val responseIndex = mutableMapOf<String, Map<Int, Map<String, String>>>()

        for ((path, pathItem) in document.paths) {
            for ((method, rawOperation) in pathItem.operationsByMethod()) {
                val operationId = rawOperation.operationId?.takeIf { it.isNotBlank() }
                    ?: error(
                        message = "OpenAPI operation $method $path in '$specPath' is missing " +
                            "a required operationId."
                    )

                val queryParameters = rawOperation.parameters
                    .map { context.resolveParameter(raw = it, document = document) }
                    .filter { it.`in` == "query" && it.example != null }
                    .associate { it.name to requireNotNull(value = it.example) }
                    .ifEmpty { null }

                operations += Operation(
                    operationId = operationId,
                    name = rawOperation.summary?.takeIf { it.isNotBlank() } ?: operationId,
                    path = path,
                    method = method,
                    queryParameters = queryParameters,
                    delayMs = rawOperation.xDevview?.delayMs
                )

                responseIndex[operationId] = context.resolveResponseIndex(
                    responses = rawOperation.responses,
                    document = document
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

    /** Per-[parse]-call state: base directory, resource loading, and the external-document cache. */
    private class ParseContext(
        private val baseDir: String,
        private val resourceLoader: NetworkMockResourceLoader
    ) {
        private val externalDocuments = mutableMapOf<String, OpenApiDocument>()

        suspend fun resolveParameter(
            raw: ParameterObject,
            document: OpenApiDocument
        ): ParameterObject {
            val ref = raw.ref ?: return raw
            return resolveRef(ref = ref, document = document) { it.components.parameters }
        }

        suspend fun resolveResponseIndex(
            responses: Map<String, ResponseObject>,
            document: OpenApiDocument
        ): Map<Int, Map<String, String>> {
            val result = mutableMapOf<Int, Map<String, String>>()
            for ((codeText, rawResponse) in responses) {
                val statusCode = codeText.toIntOrNull() ?: continue
                val response = if (rawResponse.ref != null) {
                    resolveRef(
                        ref = rawResponse.ref,
                        document = document
                    ) { it.components.responses }
                } else {
                    rawResponse
                }

                val examplesForCode = mutableMapOf<String, String>()
                for (mediaType in response.content.values) {
                    for ((exampleName, rawExample) in mediaType.examples) {
                        val example = if (rawExample.ref != null) {
                            resolveRef(
                                ref = rawExample.ref,
                                document = document
                            ) { it.components.examples }
                        } else {
                            rawExample
                        }
                        val externalValue = example.externalValue ?: continue
                        examplesForCode[exampleName] = resolvePath(
                            baseDir = baseDir,
                            ref = externalValue
                        )
                    }
                }
                if (examplesForCode.isNotEmpty()) {
                    result[statusCode] = examplesForCode
                }
            }
            return result
        }

        /**
         * Resolves a `$ref` string to its target, either locally (within [document]) or in
         * another file, exactly one level deep — the resolved object's own `$ref` (if any)
         * is not followed further.
         */
        @Suppress("DocumentationOverPrivateFunction")
        private suspend fun <T> resolveRef(
            ref: String,
            document: OpenApiDocument,
            componentsOf: (OpenApiDocument) -> Map<String, T>
        ): T {
            val (targetDocument, fragment) = if (ref.startsWith(prefix = "#/")) {
                document to ref.removePrefix(prefix = "#/")
            } else {
                val filePath = ref.substringBefore(delimiter = "#")
                val fragment = ref
                    .substringAfter(
                        delimiter = "#",
                        missingDelimiterValue = ""
                    ).removePrefix(prefix = "/")
                loadExternalDocument(filePath = filePath) to fragment
            }

            val segments = fragment.split("/")
            val name = segments.lastOrNull()
                ?: error(message = "Unresolvable \$ref '$ref': fragment has no component name.")

            return componentsOf(targetDocument)[name]
                ?: error(message = "Unresolvable \$ref '$ref': no such entry in components.")
        }

        private suspend fun loadExternalDocument(filePath: String): OpenApiDocument {
            val resolvedPath = resolvePath(baseDir = baseDir, ref = filePath)
            return externalDocuments.getOrPut(key = resolvedPath) {
                decodeDocument(
                    path = resolvedPath,
                    bytes = resourceLoader.load(path = resolvedPath)
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
