package com.worldline.devview.networkmock.repository

import com.worldline.devview.networkmock.model.ApiGroupConfig
import com.worldline.devview.networkmock.model.EndpointKey
import com.worldline.devview.networkmock.model.MockConfiguration
import com.worldline.devview.networkmock.model.MockMatch
import com.worldline.devview.networkmock.model.MockResponse
import com.worldline.devview.networkmock.model.effectiveEndpoints
import kotlinx.serialization.json.Json

/**
 * Repository for loading mock configuration and response files from resources.
 *
 * This repository is responsible for:
 * - Loading and parsing the `mocks.json` configuration file
 * - Discovering available mock response files based on naming convention
 * - Loading individual response file contents
 * - Matching incoming HTTP requests to configured endpoints
 *
 * This repository is intentionally agnostic of any specific HTTP client
 * implementation — it operates purely on file paths and raw content, making
 * it usable by any HTTP client module (Ktor, Retrofit, OkHttp, etc.).
 *
 * ## File Organization
 * All mock-related files should be placed in the integrator's project at:
 * ```
 * composeResources/files/networkmocks/
 * ├── mocks.json                              # Main configuration
 * └── responses/
 *     └── {groupId}/
 *         ├── {environmentId}/                # Environment-specific responses (highest priority)
 *         │   └── {endpointId}/
 *         │       └── {endpointId}-200.json
 *         └── {endpointId}/                   # Shared fallback responses (lowest priority)
 *             └── {endpointId}-200.json
 * ```
 *
 * ## Usage
 *
 * ### Creating the Repository
 * ```kotlin
 * @OptIn(ExperimentalResourceApi::class)
 * val repository = MockConfigRepository(
 *     configPath = "files/networkmocks/mocks.json",
 *     resourceLoader = { path -> Res.readBytes(path) }
 * )
 * ```
 *
 * ### Loading Configuration
 * ```kotlin
 * val configResult = repository.loadConfiguration()
 * configResult.onSuccess { config ->
 *     config.apiGroups.forEach { group ->
 *         println("Group: ${group.id}")
 *         group.environments.forEach { env ->
 *             println("  Environment: ${env.id} - ${env.url}")
 *         }
 *         group.endpoints.forEach { endpoint ->
 *             println("  ${endpoint.method} ${endpoint.path}")
 *         }
 *     }
 * }
 * ```
 *
 * ### Discovering Response Files
 * ```kotlin
 * val key = EndpointKey(groupId = "my-backend", environmentId = "staging", endpointId = "getUser")
 * val responses = repository.discoverResponseFiles(key = key)
 * responses.forEach { response ->
 *     println("${response.displayName}: ${response.fileName}")
 * }
 * ```
 *
 * ### Matching Requests
 * ```kotlin
 * val match = repository.findMatchingMock(
 *     host = "staging.api.example.com",
 *     path = "/v1/users/123",
 *     method = "GET"
 * )
 *
 * match?.let {
 *     println("Matched: ${it.key.compositeKey}")
 * }
 * ```
 *
 * ### Loading Response Content
 * ```kotlin
 * val key = EndpointKey(groupId = "my-backend", environmentId = "staging", endpointId = "getUser")
 * val response = repository.loadMockResponse(key = key, fileName = "getUser-200.json")
 *
 * response?.let {
 *     println("Status: ${it.statusCode}")
 *     println("Content: ${it.content}")
 * }
 * ```
 *
 * ## Error Handling
 * - [loadConfiguration] returns a [Result] that can be success or failure
 * - [findMatchingMock] returns `null` if no match is found
 * - [loadMockResponse] returns `null` if the file cannot be loaded in either location
 * - [discoverResponseFiles] returns empty list if no files are found
 *
 * @property configPath The path to the mocks.json file relative to composeResources
 * @property resourceLoader Function to load resource bytes from a path
 * @property statusCodesToDiscover The list of HTTP status codes to probe when
 *   discovering response files. Defaults to [DEFAULT_STATUS_CODES]. Override this
 *   to include non-standard status codes used by your API.
 * @see MockConfiguration
 * @see MockResponse
 * @see MockMatch
 * @see RequestMatcher
 */
public class MockConfigRepository(
    private val configPath: String,
    private val resourceLoader: suspend (String) -> ByteArray,
    private val statusCodesToDiscover: List<Int> = DEFAULT_STATUS_CODES
) {
    private val json = Json { ignoreUnknownKeys = true }

    // Cache the loaded configuration to avoid re-parsing
    private var cachedConfig: MockConfiguration? = null

    public companion object {
        /**
         * The default set of HTTP status codes probed during response file discovery.
         *
         * Covers the most common success, client error, and server error codes.
         * Pass a custom list to [MockConfigRepository] if your API uses additional
         * or non-standard status codes.
         */
        public val DEFAULT_STATUS_CODES: List<Int> = listOf(
            // 2xx Success
            200, 201, 202, 204,
            // 4xx Client Errors
            400, 401, 403, 404, 409, 422, 429,
            // 5xx Server Errors
            500, 502, 503, 504
        )
    }

    /**
     * Loads and parses the mock configuration from the JSON file.
     *
     * This method reads the configuration file from Compose Resources, parses
     * the JSON, and returns a [MockConfiguration] object containing all API groups,
     * their environments, and shared endpoint definitions.
     *
     * The configuration is cached after the first successful load to improve
     * performance on subsequent calls.
     *
     * ## File Format
     * The configuration file should be valid JSON following this structure:
     * ```json
     * {
     *   "apiGroups": [
     *     {
     *       "id": "my-backend",
     *       "name": "My Backend",
     *       "endpoints": [
     *         {
     *           "id": "getUser",
     *           "name": "Get User",
     *           "path": "/v1/users/{userId}",
     *           "method": "GET"
     *         }
     *       ],
     *       "environments": [
     *         { "id": "staging", "name": "Staging", "url": "https://staging.api.example.com" },
     *         { "id": "production", "name": "Production", "url": "https://api.example.com" }
     *       ]
     *     }
     *   ]
     * }
     * ```
     *
     * ## Error Handling
     * Returns a [Result] that will be:
     * - **Success**: Configuration loaded and parsed successfully
     * - **Failure**: File not found, invalid JSON, or I/O error
     *
     * @return A [Result] containing the [MockConfiguration] on success, or an error on failure
     */
    public suspend fun loadConfiguration(): Result<MockConfiguration> = runCatching {
        cachedConfig?.let {
            println(
                message = "[NetworkMock][Config] Using cached configuration with ${it.apiGroups.size} group(s)"
            )
            return@runCatching it
        }

        println(message = "[NetworkMock][Config] Loading configuration from: $configPath")

        val configBytes = resourceLoader(configPath)
        val configJson = configBytes.decodeToString()

        val config = json.decodeFromString<MockConfiguration>(string = configJson)
        cachedConfig = config

        println(message = "[NetworkMock][Config] Successfully loaded configuration:")
        config.apiGroups.forEach { group ->
            println(
                message = "[NetworkMock][Config]   Group: ${group.id} " +
                    "with ${group.endpoints.size} shared endpoint(s) " +
                    "and ${group.environments.size} environment(s)"
            )
            group.environments.forEach { env ->
                println(
                    message = "[NetworkMock][Config]     Environment: ${env.id} (${env.url})"
                )
            }
            group.endpoints.forEach { endpoint ->
                println(
                    message = "[NetworkMock][Config]     - ${endpoint.method} ${endpoint.path} (${endpoint.id})"
                )
            }
        }

        config
    }.onFailure { error ->
        println(
            message = "[NetworkMock][Config] ERROR: Failed to load configuration - ${error.message}"
        )
        error.printStackTrace()
    }

    /**
     * Finds a matching endpoint configuration for an incoming HTTP request.
     *
     * The environment is derived entirely from the request URL — there is no stored
     * active environment selection. This allows the app to simultaneously target
     * different environments for different API groups without any manual switching.
     *
     * ## Matching Steps
     * 1. Iterate over all [com.worldline.devview.networkmock.model.ApiGroupConfig] entries
     * 2. For each group, iterate over its [com.worldline.devview.networkmock.model.EnvironmentConfig] entries
     * 3. Extract the hostname from [com.worldline.devview.networkmock.model.EnvironmentConfig.url]
     *    and compare against the request host (case-insensitive)
     * 4. On a hostname match, build the effective endpoint list for that group+environment
     *    via [ApiGroupConfig.effectiveEndpoints] (shared pool + overrides + additions)
     * 5. Match the request path and method against the effective endpoint list
     * 6. Return a [MockMatch] carrying [MockMatch.groupId], [MockMatch.environmentId],
     *    [MockMatch.endpointId], and the resolved [MockMatch.config]
     *
     * ## Matching Rules
     * - **Host matching**: Compares hostnames extracted from URLs (case-insensitive)
     * - **Path matching**: Uses [RequestMatcher.matchesPath] for path parameter support
     * - **Method matching**: Exact match (case-sensitive)
     *
     * @param host The request hostname (e.g., `"staging.api.example.com"`)
     * @param path The request path (e.g., `"/v1/users/123"`)
     * @param method The HTTP method (e.g., `"GET"`, `"POST"`)
     * @return A [MockMatch] if a matching endpoint is found, or `null` otherwise
     */
    @Suppress("ReturnCount", "LongMethod")
    public suspend fun findMatchingMock(host: String, path: String, method: String): MockMatch? {
        println(message = "[NetworkMock][Matching] Looking for match: $method $host$path")

        val config = loadConfiguration().getOrNull()
        if (config == null) {
            println(message = "[NetworkMock][Matching] ERROR: Configuration not loaded")
            return null
        }

        println(
            message = "[NetworkMock][Matching] Comparing against ${config.apiGroups.size} group(s):"
        )

        for (group in config.apiGroups) {
            println(message = "[NetworkMock][Matching]   Group '${group.id}':")
            for (environment in group.environments) {
                val configHostname = extractHostname(url = environment.url)
                val hostMatches = configHostname.equals(other = host, ignoreCase = true)
                println(
                    message = "[NetworkMock][Matching]     Environment '${environment.id}': " +
                        "$configHostname vs $host = $hostMatches"
                )

                if (!hostMatches) continue

                println(
                    message = "[NetworkMock][Matching]     Host matched — " +
                        "resolving effective endpoints for '${group.id}/${environment.id}'"
                )

                val effectiveEndpoints = group.effectiveEndpoints(environment = environment)
                println(
                    message = "[NetworkMock][Matching]     Checking ${effectiveEndpoints.size} effective endpoint(s)"
                )

                val matchingEndpoint = effectiveEndpoints.firstOrNull { endpoint ->
                    val pathMatches = RequestMatcher.matchesPath(
                        configPath = endpoint.path,
                        requestPath = path
                    )
                    val methodMatches = endpoint.method == method
                    println(message = "[NetworkMock][Matching]       Endpoint '${endpoint.id}':")
                    println(
                        message = "[NetworkMock][Matching]         Path: ${endpoint.path} vs " +
                            "$path = $pathMatches"
                    )
                    println(
                        message = "[NetworkMock][Matching]         Method: ${endpoint.method} vs " +
                            "$method = $methodMatches"
                    )
                    pathMatches && methodMatches
                }

                if (matchingEndpoint != null) {
                    println(
                        message = "[NetworkMock][Matching] SUCCESS: Matched endpoint " +
                            "'${matchingEndpoint.id}' in group '${group.id}', " +
                            "environment '${environment.id}'"
                    )
                    return MockMatch(
                        key = EndpointKey(
                            groupId = group.id,
                            environmentId = environment.id,
                            endpointId = matchingEndpoint.id
                        ),
                        config = matchingEndpoint
                    )
                }

                println(
                    message = "[NetworkMock][Matching]     ERROR: No matching endpoint found " +
                        "for $method $path in group '${group.id}', environment '${environment.id}'"
                )
            }
        }

        println(message = "[NetworkMock][Matching] ERROR: No match found for $method $host$path")
        return null
    }

    /**
     * Discovers available mock response files for a specific group, environment, and endpoint.
     *
     * Uses a two-tier resolution strategy — environment-specific files take priority over
     * shared fallback files. For each status code in [statusCodesToDiscover] and each
     * known suffix, the method first tries the environment-specific path, then the shared
     * path. Results are merged and deduplicated by file name, with environment-specific
     * files winning on any conflict.
     *
     * ## Resolution Order
     * For each candidate file name:
     * 1. `files/networkmocks/responses/{groupId}/{environmentId}/{endpointId}/{fileName}` ← tried first
     * 2. `files/networkmocks/responses/{groupId}/{endpointId}/{fileName}` ← fallback
     *
     * ## Naming Convention
     * - `getUser-200.json` → Success response
     * - `getUser-404-simple.json` → Not found with simple error body
     * - `getUser-404-detailed.json` → Not found with detailed error body
     * - `getUser-500.json` → Server error
     *
     * @param key The [EndpointKey] identifying the group, environment, and endpoint
     * @return A deduplicated, status-code-sorted list of discovered [MockResponse] objects
     *   (may be empty if no files are found in either location)
     */
    public suspend fun discoverResponseFiles(key: EndpointKey): List<MockResponse> =
        discoverResponseFiles(
            groupId = key.groupId,
            environmentId = key.environmentId,
            endpointId = key.endpointId
        )

    /**
     * Discovers available mock response files for a specific group, environment, and endpoint.
     *
     * Uses a two-tier resolution strategy — environment-specific files take priority over
     * shared fallback files. For each status code in [statusCodesToDiscover] and each
     * known suffix, the method first tries the environment-specific path, then the shared
     * path. Results are merged and deduplicated by file name, with environment-specific
     * files winning on any conflict.
     *
     * ## Resolution Order
     * For each candidate file name:
     * 1. `files/networkmocks/responses/{groupId}/{environmentId}/{endpointId}/{fileName}` ← tried first
     * 2. `files/networkmocks/responses/{groupId}/{endpointId}/{fileName}` ← fallback
     *
     * ## Naming Convention
     * - `getUser-200.json` → Success response
     * - `getUser-404-simple.json` → Not found with simple error body
     * - `getUser-404-detailed.json` → Not found with detailed error body
     * - `getUser-500.json` → Server error
     *
     * @param groupId The [com.worldline.devview.networkmock.model.ApiGroupConfig] identifier
     * @param environmentId The [com.worldline.devview.networkmock.model.EnvironmentConfig] identifier
     * @param endpointId The [com.worldline.devview.networkmock.model.EndpointConfig] identifier
     * @return A deduplicated, status-code-sorted list of discovered [MockResponse] objects
     *   (may be empty if no files are found in either location)
     */
    public suspend fun discoverResponseFiles(
        groupId: String,
        environmentId: String,
        endpointId: String
    ): List<MockResponse> {
        println(
            message = "[NetworkMock][Discovery] Discovering response files for " +
                "$groupId/$environmentId/$endpointId"
        )

        val environmentPath = "files/networkmocks/responses/$groupId/$environmentId/$endpointId"
        val sharedPath = "files/networkmocks/responses/$groupId/$endpointId"

        // TODO - Consider making suffixes configurable if needed by integrators
        val suffixesToTry = listOf("", "-simple", "-detailed", "-error", "-success")

        // Use a LinkedHashMap keyed by fileName so that environment-specific entries
        // automatically win over shared ones when both exist for the same file name.
        val discovered = linkedMapOf<String, MockResponse>()

        for (statusCode in statusCodesToDiscover) {
            for (suffix in suffixesToTry) {
                val fileName = "$endpointId-$statusCode$suffix.json"

                // Tier 1 — environment-specific
                val envResponse = loadMockResponseFromPath(
                    filePath = "$environmentPath/$fileName",
                    fileName = fileName
                )
                if (envResponse != null) {
                    println(
                        message = "[NetworkMock][Discovery]   Found (env-specific): $fileName " +
                            "(status ${envResponse.statusCode})"
                    )
                    discovered[fileName] = envResponse
                    continue
                }

                // Tier 2 — shared fallback
                val sharedResponse = loadMockResponseFromPath(
                    filePath = "$sharedPath/$fileName",
                    fileName = fileName
                )
                if (sharedResponse != null) {
                    println(
                        message = "[NetworkMock][Discovery]   Found (shared): $fileName " +
                            "(status ${sharedResponse.statusCode})"
                    )
                    discovered[fileName] = sharedResponse
                }
            }
        }

        println(
            message = "[NetworkMock][Discovery] Discovered ${discovered.size} response file(s) " +
                "for '$groupId/$environmentId/$endpointId'"
        )
        return discovered.values.sortedBy { it.statusCode }
    }

    /**
     * Loads a specific mock response file for a given endpoint key.
     *
     * Convenience overload of [loadMockResponse] that accepts an [EndpointKey] instead of
     * three separate string identifiers. Delegates directly to the three-param overload.
     *
     * @param key The [EndpointKey] identifying the group, environment, and endpoint
     * @param fileName The response filename (e.g., `"getUser-200.json"`)
     * @return A [MockResponse] if the file is found in either location, or `null` on error
     */
    public suspend fun loadMockResponse(key: EndpointKey, fileName: String): MockResponse? =
        loadMockResponse(
            groupId = key.groupId,
            environmentId = key.environmentId,
            endpointId = key.endpointId,
            fileName = fileName
        )

    /**
     * Loads a specific mock response file for a given group, environment, and endpoint.
     *
     * Uses the same two-tier resolution strategy as [discoverResponseFiles]:
     * the environment-specific path is tried first, and the shared fallback path
     * is used if the file is not found there.
     *
     * ## Resolution Order
     * 1. `files/networkmocks/responses/{groupId}/{environmentId}/{endpointId}/{fileName}` ← tried first
     * 2. `files/networkmocks/responses/{groupId}/{endpointId}/{fileName}` ← fallback
     *
     * @param groupId The [com.worldline.devview.networkmock.model.ApiGroupConfig] identifier
     * @param environmentId The [com.worldline.devview.networkmock.model.EnvironmentConfig] identifier
     * @param endpointId The [com.worldline.devview.networkmock.model.EndpointConfig] identifier
     * @param fileName The response filename (e.g., `"getUser-200.json"`)
     * @return A [MockResponse] if the file is found in either location, or `null` on error
     */
    public suspend fun loadMockResponse(
        groupId: String,
        environmentId: String,
        endpointId: String,
        fileName: String
    ): MockResponse? {
        println(
            message = "[NetworkMock][Loading] Loading response: $groupId/$environmentId/$endpointId/$fileName"
        )

        // Tier 1 — environment-specific
        val envPath = "files/networkmocks/responses/$groupId/$environmentId/$endpointId/$fileName"
        val envResponse = loadMockResponseFromPath(filePath = envPath, fileName = fileName)
        if (envResponse != null) {
            println(
                message = "[NetworkMock][Loading] Successfully loaded (env-specific): $fileName " +
                    "(status ${envResponse.statusCode})"
            )
            return envResponse
        }

        // Tier 2 — shared fallback
        val sharedPath = "files/networkmocks/responses/$groupId/$endpointId/$fileName"
        val sharedResponse = loadMockResponseFromPath(filePath = sharedPath, fileName = fileName)
        if (sharedResponse != null) {
            println(
                message = "[NetworkMock][Loading] Successfully loaded (shared): $fileName " +
                    "(status ${sharedResponse.statusCode})"
            )
            return sharedResponse
        }

        println(
            message = "[NetworkMock][Loading] ERROR: Failed to load '$fileName' from " +
                "either '$envPath' or '$sharedPath'"
        )
        return null
    }

    /**
     * Loads a mock response from a specific file path.
     *
     * Internal helper used by both [loadMockResponse] and [discoverResponseFiles].
     * Returns `null` silently on any I/O error — callers treat `null` as "file not found"
     * and fall through to the next resolution tier.
     *
     * @param filePath The full path to the response file relative to composeResources
     * @param fileName The filename (used for status code parsing and display name generation)
     * @return A [MockResponse] if the file exists and parses successfully, or `null` otherwise
     */
    @Suppress("DocumentationOverPrivateFunction")
    private suspend fun loadMockResponseFromPath(
        filePath: String,
        fileName: String
    ): MockResponse? = runCatching {
        val responseBytes = resourceLoader(filePath)
        val content = responseBytes.decodeToString()
        MockResponse.fromFile(fileName = fileName, content = content)
    }.getOrNull()

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
