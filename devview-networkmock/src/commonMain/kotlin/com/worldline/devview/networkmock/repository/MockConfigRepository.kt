package com.worldline.devview.networkmock.repository

import com.worldline.devview.networkmock.model.MockConfiguration
import com.worldline.devview.networkmock.model.MockMatch
import com.worldline.devview.networkmock.model.MockResponse
import com.worldline.devview.networkmock.plugin.RequestMatcher
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
 * ## File Organization
 * All mock-related files should be placed in the integrator's project at:
 * ```
 * composeResources/files/networkmocks/
 * ├── mocks.json                          # Main configuration
 * └── responses/                          # Response files organized by endpoint
 *     ├── getUser/
 *     │   ├── getUser-200.json
 *     │   ├── getUser-404-simple.json
 *     │   └── getUser-500.json
 *     └── createUser/
 *         ├── createUser-201.json
 *         └── createUser-400.json
 * ```
 *
 * ## Usage
 *
 * ### Creating the Repository
 * ```kotlin
 * @OptIn(ExperimentalResourceApi::class)
 * val repository = MockConfigRepository(
 *     configPath = "files/networkmocks/mocks.json",
 *     resourceLoader = { path -> Res.readBytes(path) } // Integrator's Res object
 * )
 * ```
 *
 * ### Loading Configuration
 * ```kotlin
 * val configResult = repository.loadConfiguration()
 * configResult.onSuccess { config ->
 *     config.hosts.forEach { host ->
 *         println("Host: ${host.id} - ${host.url}")
 *         host.endpoints.forEach { endpoint ->
 *             println("  ${endpoint.method} ${endpoint.path}")
 *         }
 *     }
 * }
 * ```
 *
 * ### Discovering Response Files
 * ```kotlin
 * val responses = repository.discoverResponseFiles("getUser")
 * responses.forEach { response ->
 *     println("${response.displayName}: ${response.fileName}")
 * }
 * ```
 *
 * ### Matching Requests
 * ```kotlin
 * val match = repository.findMatchingMock(
 *     host = "staging.api.example.com",
 *     path = "/api/v1/user/123",
 *     method = "GET"
 * )
 *
 * match?.let {
 *     println("Matched endpoint: ${it.endpointId} on host ${it.hostId}")
 * }
 * ```
 *
 * ### Loading Response Content
 * ```kotlin
 * val response = repository.loadMockResponse(
 *     endpointId = "getUser",
 *     fileName = "getUser-200.json"
 * )
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
 * - [loadMockResponse] returns `null` if the file cannot be loaded
 * - [discoverResponseFiles] returns empty list if no files are found
 *
 * @property configPath The path to the mocks.json file relative to composeResources
 * @property resourceLoader Function to load resource bytes from a path
 * @see MockConfiguration
 * @see MockResponse
 * @see MockMatch
 */
public class MockConfigRepository(
    private val configPath: String,
    private val resourceLoader: suspend (String) -> ByteArray
) {
    private val json = Json { ignoreUnknownKeys = true }

    // Cache the loaded configuration to avoid re-parsing
    private var cachedConfig: MockConfiguration? = null

    /**
     * Loads and parses the mock configuration from the JSON file.
     *
     * This method reads the configuration file from Compose Resources, parses
     * the JSON, and returns a [MockConfiguration] object containing all hosts
     * and endpoints.
     *
     * The configuration is cached after the first successful load to improve
     * performance on subsequent calls.
     *
     * ## File Format
     * The configuration file should be valid JSON following this structure:
     * ```json
     * {
     *   "hosts": [
     *     {
     *       "id": "staging",
     *       "url": "https://staging.api.example.com",
     *       "endpoints": [
     *         {
     *           "id": "getUser",
     *           "name": "Get User Profile",
     *           "path": "/api/v1/user/{userId}",
     *           "method": "GET"
     *         }
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
     * ## Usage
     * ```kotlin
     * val result = repository.loadConfiguration()
     * result.onSuccess { config ->
     *     println("Loaded ${config.hosts.size} hosts")
     * }.onFailure { error ->
     *     println("Failed to load config: ${error.message}")
     * }
     * ```
     *
     * @return A [Result] containing the [MockConfiguration] on success, or an error on failure
     */
    public suspend fun loadConfiguration(): Result<MockConfiguration> = runCatching {
        // Return cached config if available
        cachedConfig?.let {
            println(
                message = "[NetworkMock][Config] Using cached configuration with ${it.hosts.size} host(s)"
            )
            return@runCatching it
        }

        println(message = "[NetworkMock][Config] Loading configuration from: $configPath")

        // Load from resources using the injected loader
        val configBytes = resourceLoader(configPath)
        val configJson = configBytes.decodeToString()

        val config = json.decodeFromString<MockConfiguration>(string = configJson)
        cachedConfig = config

        println(message = "[NetworkMock][Config] Successfully loaded configuration:")
        config.hosts.forEach { host ->
            println(
                message = "[NetworkMock][Config]   Host: ${host.id} (${host.url}) " +
                    "with ${host.endpoints.size} endpoint(s)"
            )
            host.endpoints.forEach { endpoint ->
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
     * This method performs the following matching steps:
     * 1. Extract hostname from the host parameter
     * 2. Find a [HostConfig] with matching hostname (case-insensitive)
     * 3. For each endpoint in that host, check if the path and method match
     * 4. Use [RequestMatcher] to handle path parameters (e.g., `/users/{userId}`)
     *
     * ## Matching Rules
     * - **Host matching**: Compares hostnames (case-insensitive)
     * - **Path matching**: Uses [RequestMatcher.matchesPath] for parameter support
     * - **Method matching**: Exact match (case-sensitive)
     *
     * ## Examples
     *
     * ### Simple Path Match
     * ```kotlin
     * // Config: { path: "/api/users", method: "GET" }
     * val match = repository.findMatchingMock(
     *     host = "api.example.com",
     *     path = "/api/users",
     *     method = "GET"
     * ) // Returns MockMatch
     * ```
     *
     * ### Path Parameter Match
     * ```kotlin
     * // Config: { path: "/api/users/{userId}", method: "GET" }
     * val match = repository.findMatchingMock(
     *     host = "api.example.com",
     *     path = "/api/users/123",
     *     method = "GET"
     * ) // Returns MockMatch - {userId} matches "123"
     * ```
     *
     * ### No Match
     * ```kotlin
     * val match = repository.findMatchingMock(
     *     host = "different.api.com",  // Wrong host
     *     path = "/api/users",
     *     method = "GET"
     * ) // Returns null
     * ```
     *
     * @param host The request hostname (e.g., "staging.api.example.com")
     * @param path The request path (e.g., "/api/v1/user/123")
     * @param method The HTTP method (e.g., "GET", "POST")
     * @return A [MockMatch] if a matching endpoint is found, or `null` otherwise
     */
    @Suppress("ReturnCount")
    public suspend fun findMatchingMock(host: String, path: String, method: String): MockMatch? {
        println(message = "[NetworkMock][Matching] Looking for match: $method $host$path")

        val config = loadConfiguration().getOrNull()
        if (config == null) {
            println(message = "[NetworkMock][Matching] ERROR: Configuration not loaded")
            return null
        }

        // Find matching host by comparing hostnames
        println(message = "[NetworkMock][Matching] Comparing against ${config.hosts.size} host(s):")
        val matchingHost = config.hosts.firstOrNull { hostConfig ->
            val configHostname = extractHostname(url = hostConfig.url)
            val matches = configHostname.equals(other = host, ignoreCase = true)
            println(
                message = "[NetworkMock][Matching]   Host '${hostConfig.id}': $configHostname vs $host = $matches"
            )
            matches
        }

        if (matchingHost == null) {
            println(message = "[NetworkMock][Matching] ERROR: No matching host found for '$host'")
            return null
        }

        println(
            message = "[NetworkMock][Matching] Host matched: '${matchingHost.id}' - " +
                "checking ${matchingHost.endpoints.size} endpoint(s)"
        )

        // Find matching endpoint within the host
        val matchingEndpoint = matchingHost.endpoints.firstOrNull { endpoint ->
            val pathMatches = RequestMatcher.matchesPath(
                configPath = endpoint.path,
                requestPath = path
            )
            val methodMatches = endpoint.method == method
            println(message = "[NetworkMock][Matching]   Endpoint '${endpoint.id}':")
            println(
                message = "[NetworkMock][Matching]     Path: ${endpoint.path} vs $path = $pathMatches"
            )
            println(
                message = "[NetworkMock][Matching]     Method: ${endpoint.method} vs $method = $methodMatches"
            )
            pathMatches && methodMatches
        }

        if (matchingEndpoint == null) {
            println(
                message = "[NetworkMock][Matching] ERROR: No matching endpoint found for $method $path"
            )
            return null
        }

        println(
            message =
            "[NetworkMock][Matching] SUCCESS: Matched endpoint '${matchingEndpoint.id}' " +
                "on host '${matchingHost.id}'"
        )

        return MockMatch(
            hostId = matchingHost.id,
            endpointId = matchingEndpoint.id,
            config = matchingEndpoint
        )
    }

    /**
     * Discovers available mock response files for a specific endpoint.
     *
     * This method attempts to load response files following the naming convention:
     * - Format: `{endpointId}-{statusCode}[-{suffix}].json`
     * - Location: `files/networkmocks/responses/{endpointId}/`
     *
     * The method tries common status codes and returns all successfully loaded
     * response files as [MockResponse] objects.
     *
     * ## Naming Convention
     * - `getUser-200.json` → Success response
     * - `getUser-404-simple.json` → Not found with simple error
     * - `getUser-404-detailed.json` → Not found with detailed error
     * - `getUser-500.json` → Server error
     *
     * ## Discovery Strategy
     * Since we cannot list directory contents in Compose Resources, we try to
     * load files with common status codes:
     * - 2xx: 200, 201, 202, 204
     * - 4xx: 400, 401, 403, 404, 409, 422, 429
     * - 5xx: 500, 502, 503, 504
     *
     * For each status code, we also try common suffixes like "simple", "detailed",
     * "error", etc.
     *
     * ## Usage
     * ```kotlin
     * val responses = repository.discoverResponseFiles("getUser")
     * responses.forEach { response ->
     *     println("${response.displayName}: ${response.statusCode}")
     * }
     * ```
     *
     * @param endpointId The endpoint identifier (e.g., "getUser", "createPost")
     * @return A list of discovered [MockResponse] objects (may be empty)
     */
    public suspend fun discoverResponseFiles(endpointId: String): List<MockResponse> {
        println(
            message = "[NetworkMock][Discovery] Discovering response files for endpoint: $endpointId"
        )
        val responses = mutableListOf<MockResponse>()
        val basePath = "files/networkmocks/responses/$endpointId"

        // Common status codes to try
        val statusCodesToTry = listOf(
            // 2xx Success
            200, 201, 202, 204,
            // 4xx Client Errors
            400, 401, 403, 404, 409, 422, 429,
            // 5xx Server Errors
            500, 502, 503, 504
        )

        // Common suffixes to try (in addition to no suffix)
        val suffixesToTry = listOf(
            "", // No suffix
            "-simple",
            "-detailed",
            "-error",
            "-success"
        )

        // Try each combination of status code and suffix
        for (statusCode in statusCodesToTry) {
            for (suffix in suffixesToTry) {
                val fileName = "$endpointId-$statusCode$suffix.json"
                val filePath = "$basePath/$fileName"

                val response = loadMockResponseFromPath(filePath = filePath, fileName = fileName)
                if (response != null) {
                    println(
                        message = "[NetworkMock][Discovery]   Found: $fileName (status ${response.statusCode})"
                    )
                    responses.add(element = response)
                }
            }
        }

        println(
            message = "[NetworkMock][Discovery] Discovered ${responses.size} response file(s) for '$endpointId'"
        )
        return responses.sortedBy { it.statusCode }
    }

    /**
     * Loads a specific mock response file by endpoint ID and filename.
     *
     * This method loads a response file from the expected location:
     * `files/networkmocks/responses/{endpointId}/{fileName}`
     *
     * The file content is read and parsed into a [MockResponse] object that
     * includes the status code (extracted from filename), display name, and
     * the raw content string.
     *
     * ## Usage
     * ```kotlin
     * val response = repository.loadMockResponse(
     *     endpointId = "getUser",
     *     fileName = "getUser-200.json"
     * )
     *
     * response?.let {
     *     println("Status: ${it.statusCode}")
     *     println("Content: ${it.content}")
     * }
     * ```
     *
     * ## Error Handling
     * Returns `null` if:
     * - The file doesn't exist
     * - The file cannot be read
     * - The filename format is invalid (cannot extract status code)
     *
     * @param endpointId The endpoint identifier (e.g., "getUser")
     * @param fileName The response filename (e.g., "getUser-200.json")
     * @return A [MockResponse] if successful, or `null` on error
     */
    public suspend fun loadMockResponse(endpointId: String, fileName: String): MockResponse? {
        println(message = "[NetworkMock][Loading] Loading response: $endpointId/$fileName")
        val filePath = "files/networkmocks/responses/$endpointId/$fileName"
        val response = loadMockResponseFromPath(filePath = filePath, fileName = fileName)
        if (response != null) {
            println(
                message = "[NetworkMock][Loading] Successfully loaded: $fileName (status ${response.statusCode})"
            )
        } else {
            println(message = "[NetworkMock][Loading] ERROR: Failed to load: $fileName")
        }
        return response
    }

    /**
     * Loads a mock response from a specific file path.
     *
     * This is an internal helper method that actually performs the file loading
     * and parsing. It's used by both [loadMockResponse] and [discoverResponseFiles].
     *
     * @param filePath The full path to the response file
     * @param fileName The filename (used for parsing and display)
     * @return A [MockResponse] if successful, or `null` on error
     */
    @Suppress("CommentOverPrivateFunction")
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
     * This helper method parses a URL and extracts just the hostname portion,
     * removing the scheme (http/https) and any path/query components.
     *
     * ## Examples
     * ```kotlin
     * extractHostname("https://api.example.com") // "api.example.com"
     * extractHostname("http://staging.api.example.com:8080") // "staging.api.example.com"
     * extractHostname("https://api.example.com/v1") // "api.example.com"
     * ```
     *
     * @param url The full URL string
     * @return The hostname portion, or the original string if parsing fails
     */
    @Suppress("CommentOverPrivateFunction")
    private fun extractHostname(url: String): String {
        // Remove scheme (http:// or https://)
        var hostname = url.removePrefix(prefix = "http://").removePrefix(prefix = "https://")

        // Remove port if present
        hostname = hostname.substringBefore(delimiter = ":")

        // Remove path if present
        hostname = hostname.substringBefore(delimiter = "/")

        return hostname
    }
}
