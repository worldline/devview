package com.worldline.devview.networkmock.core.repository

import com.worldline.devview.networkmock.core.NetworkMockResourceLoader
import com.worldline.devview.networkmock.core.model.MockConfiguration
import com.worldline.devview.networkmock.core.model.MockMatch
import com.worldline.devview.networkmock.core.model.MockResponse
import com.worldline.devview.networkmock.core.model.OperationKey
import com.worldline.devview.networkmock.core.openapi.OpenApiParser
import kotlinx.serialization.SerializationException

/**
 * Repository for loading OpenAPI-based mock configuration and response files from resources.
 *
 * This repository is responsible for:
 * - Loading and parsing OpenAPI 3.x spec files (JSON or YAML), one per configured API group
 * - Discovering the response variants declared for each operation
 * - Loading individual response variant contents
 * - Matching incoming HTTP requests to a configured operation
 *
 * Format parsing is delegated entirely to [OpenApiParser] — this repository never sees
 * OpenAPI-shaped types itself, only the resulting [MockConfiguration] and a plain response
 * index (`specId -> operationId -> statusCode -> exampleName -> file path`) used by
 * [discoverResponseFiles] and [loadMockResponse].
 *
 * This repository is intentionally agnostic of any specific HTTP client implementation — it
 * operates purely on file paths and raw content, making it usable by any HTTP client module
 * (Ktor, Retrofit, OkHttp, etc.).
 *
 * @property specPaths Paths to the OpenAPI spec files, relative to composeResources. One
 *   spec file is one [com.worldline.devview.networkmock.core.model.ApiSpec].
 * @property resourceLoader [NetworkMockResourceLoader] that provides resource bytes from a path
 * @see MockConfiguration
 * @see MockResponse
 * @see MockMatch
 * @see RequestMatcher
 */
public class MockConfigRepository(
    private val specPaths: List<String>,
    private val resourceLoader: NetworkMockResourceLoader
) {
    // Cache the loaded configuration to avoid re-parsing every spec on every call.
    private var cachedConfig: MockConfiguration? = null

    /** `specId -> operationId -> statusCode -> exampleName -> resolved response file path`. */
    @Suppress("DocumentationOverPrivateProperty")
    private var responseIndex: Map<String, Map<String, Map<Int, Map<String, String>>>> = emptyMap()

    /**
     * Loads and parses every configured OpenAPI spec.
     *
     * The result is cached after the first successful load — subsequent calls return the
     * cached value without re-reading or re-parsing any spec file.
     *
     * @return A [Result] containing the [MockConfiguration] on success, or an error
     *   (spec not found, malformed JSON/YAML, missing `operationId`, unresolvable `$ref`)
     *   on failure.
     */
    public suspend fun loadConfiguration(): Result<MockConfiguration> {
        // ponytail: explicit try-catch instead of runCatching — K/N's inline expansion of runCatching
        // does not reliably catch exceptions thrown by suspend calls in the generated state machine.
        return try {
            cachedConfig?.let { return Result.success(value = it) }

            val parsed = specPaths.map { specPath ->
                OpenApiParser.parse(specPath = specPath, resourceLoader = resourceLoader)
            }
            val config = MockConfiguration(specs = parsed.map { it.apiSpec })
            cachedConfig = config
            responseIndex = parsed.associate { it.apiSpec.id to it.responseIndex }

            println(
                message = "[NetworkMock][Config] Loaded ${config.specs.size} spec(s): " +
                    config.specs.joinToString { "${it.id} (${it.operations.size} operations)" }
            )
            Result.success(value = config)
        } catch (e: IllegalStateException) {
            println(
                message = "[NetworkMock][Config] ERROR: Failed to load configuration - ${e.message}"
            )
            Result.failure(exception = e)
        } catch (e: SerializationException) {
            println(
                message = "[NetworkMock][Config] ERROR: Failed to load configuration - ${e.message}"
            )
            Result.failure(exception = e)
        }
    }

    /**
     * Finds a matching operation for an incoming HTTP request.
     *
     * Specs are checked in configuration order. Within a spec whose [servers][com.worldline.devview.networkmock.core.model.ApiSpec.servers]
     * include a hostname matching [host], the first operation whose path, method, and query
     * parameters all match wins. If no operation in that spec matches, the next spec is
     * tried — two specs may legitimately share a hostname, and the first spec that actually
     * has a matching operation wins.
     *
     * @param host The request hostname (e.g., `"staging.api.example.com"`)
     * @param path The request path (e.g., `"/v1/users/123"`)
     * @param method The HTTP method (e.g., `"GET"`, `"POST"`)
     * @return A [MockMatch] if a matching operation is found, or `null` otherwise
     */
    public suspend fun findMatchingMock(
        host: String,
        path: String,
        method: String,
        queryParameters: Map<String, List<String>> = emptyMap()
    ): MockMatch? {
        val config = loadConfiguration().getOrNull() ?: return null

        val match = config.specs.firstNotNullOfOrNull { spec ->
            val hostMatches = spec.servers.any {
                extractHostname(url = it).equals(other = host, ignoreCase = true)
            }
            if (!hostMatches) return@firstNotNullOfOrNull null

            val matchingOperation = spec.operations.firstOrNull { operation ->
                RequestMatcher.matchesPath(configPath = operation.path, requestPath = path) &&
                    operation.method == method &&
                    RequestMatcher.matchesQueryParams(
                        configQueryParams = operation.queryParameters,
                        requestQueryParams = queryParameters
                    )
            } ?: return@firstNotNullOfOrNull null

            spec to matchingOperation
        }

        if (match == null) {
            println(message = "[NetworkMock][Matching] No match for $method $host$path")
            return null
        }

        val (spec, matchingOperation) = match
        println(
            message = "[NetworkMock][Matching] Matched $method $path -> " +
                "${spec.id}/${matchingOperation.operationId}"
        )
        return MockMatch(
            key = OperationKey(specId = spec.id, operationId = matchingOperation.operationId),
            config = matchingOperation,
            delayMs = matchingOperation.delayMs ?: spec.delayMs
        )
    }

    /**
     * Discovers the response variants declared for a specific operation.
     *
     * Reads the variants declared in the spec's `responses.<code>.content.*.examples` for
     * this operation — there is no probing, every returned variant corresponds to a
     * `(statusCode, exampleName)` pair the spec author explicitly declared.
     *
     * @param key The [OperationKey] identifying the spec and operation
     * @return The declared response variants, sorted by status code (may be empty if the
     *   spec is not loaded or the operation declares no examples)
     */
    public suspend fun discoverResponseFiles(key: OperationKey): List<MockResponse> {
        loadConfiguration()
        val variantsByStatusCode = responseIndex[key.specId]?.get(
            key = key.operationId
        ) ?: return emptyList()
        return variantsByStatusCode
            .flatMap { (statusCode, examplesByName) ->
                examplesByName.mapNotNull { (exampleName, path) ->
                    loadResponseFromPath(
                        path = path,
                        statusCode = statusCode,
                        exampleName = exampleName
                    )
                }
            }.sortedBy { it.statusCode }
    }

    /**
     * Loads one specific response variant's content for an operation.
     *
     * @param key The [OperationKey] identifying the spec and operation
     * @param statusCode The variant's HTTP status code
     * @param exampleName The variant's OpenAPI example name
     * @return The loaded [MockResponse], or `null` if the variant isn't declared or its file
     *   can't be read
     */
    public suspend fun loadMockResponse(
        key: OperationKey,
        statusCode: Int,
        exampleName: String
    ): MockResponse? {
        loadConfiguration()
        val path = responseIndex[key.specId]
            ?.get(key = key.operationId)
            ?.get(key = statusCode)
            ?.get(key = exampleName)
            ?: return null
        return loadResponseFromPath(path = path, statusCode = statusCode, exampleName = exampleName)
    }

    @Suppress("DocumentationOverPrivateFunction")
    private suspend fun loadResponseFromPath(
        path: String,
        statusCode: Int,
        exampleName: String
    ): MockResponse? = try {
        val content = resourceLoader.load(path = path).decodeToString()
        MockResponse.create(statusCode = statusCode, exampleName = exampleName, content = content)
    } catch (@Suppress("SwallowedException") e: IllegalStateException) {
        null
    }

    /**
     * Extracts the hostname from a URL string.
     *
     * ## Examples
     * ```kotlin
     * extractHostname("https://api.example.com")              // "api.example.com"
     * extractHostname("http://staging.api.example.com:8080")  // "staging.api.example.com"
     * extractHostname("https://api.example.com/v1")           // "api.example.com"
     * ```
     *
     * @param url The full URL string
     * @return The hostname portion, or the original string if parsing fails
     */
    @Suppress("DocumentationOverPrivateFunction")
    private fun extractHostname(url: String): String {
        var hostname = url.removePrefix(prefix = "http://").removePrefix(prefix = "https://")
        hostname = hostname.substringBefore(delimiter = ":")
        hostname = hostname.substringBefore(delimiter = "/")
        return hostname
    }
}
