package com.worldline.devview.networkmock.model

/**
 * Represents a loaded mock response that can be returned by the network mock plugin.
 *
 * This data class encapsulates a mock API response loaded from a JSON file in the
 * `composeResources/files/networkmocks/responses/` directory. It contains the
 * response status code, metadata for display, and the actual response content.
 *
 * ## File Naming Convention
 * Response files follow a strict naming convention to enable automatic discovery:
 * - Format: `{endpointId}-{statusCode}[-{suffix}].json`
 * - Examples:
 *   - `getUser-200.json` → status 200, display name "Success (200)"
 *   - `getUser-404-simple.json` → status 404, display name "Not Found - Simple (404)"
 *   - `getUser-500.json` → status 500, display name "Server Error (500)"
 *
 * ## File Location
 * Files should be organized by endpoint ID:
 * ```
 * composeResources/files/networkmocks/responses/
 * ├── getUser/
 * │   ├── getUser-200.json
 * │   ├── getUser-404-simple.json
 * │   └── getUser-500.json
 * └── createUser/
 *     ├── createUser-201.json
 *     └── createUser-400.json
 * ```
 *
 * ## Response Content Format
 * The content is the raw JSON response body as a String. For MVP, response files
 * contain only the response body (no metadata):
 * ```json
 * {
 *   "id": "user-123",
 *   "name": "John Doe",
 *   "email": "john.doe@example.com"
 * }
 * ```
 *
 * ## Usage Example
 * ```kotlin
 * val repository = MockConfigRepository("files/networkmocks/mocks.json")
 * val response = repository.loadMockResponse(
 *     endpointId = "getUser",
 *     fileName = "getUser-200.json"
 * )
 *
 * response?.let {
 *     println("Status: ${it.statusCode}")
 *     println("Display: ${it.displayName}")
 *     println("Content: ${it.content}")
 * }
 * ```
 *
 * ## Display Name Generation
 * The display name is automatically generated from the file name:
 * - `getUser-200.json` → "Success (200)"
 * - `getUser-404-simple.json` → "Not Found - Simple (404)"
 * - `getUser-404-detailed.json` → "Not Found - Detailed (404)"
 * - `getUser-500.json` → "Server Error (500)"
 *
 * @property statusCode HTTP status code extracted from the file name
 * @property fileName The original file name (e.g., "getUser-200.json")
 * @property displayName Human-readable name for UI display (e.g., "Success (200)")
 * @property content The raw JSON response body as a String
 * @see MockConfiguration
 * @see EndpointConfig
 * @see com.worldline.devview.networkmock.repository.MockConfigRepository
 */
public data class MockResponse(
    val statusCode: Int,
    val fileName: String,
    val displayName: String,
    val content: String
) {
    public companion object {
        /**
         * Creates a [MockResponse] from a file name and content.
         *
         * This factory method parses the file name to extract the status code
         * and generates an appropriate display name for UI presentation.
         *
         * ## File Name Parsing
         * Expected format: `{endpointId}-{statusCode}[-{suffix}].json`
         * - `getUser-200.json` → status = 200, suffix = null
         * - `getUser-404-simple.json` → status = 404, suffix = "simple"
         *
         * @param fileName The response file name
         * @param content The raw JSON content as a String
         * @return A [MockResponse] instance, or null if the file name format is invalid
         */
        public fun fromFile(fileName: String, content: String): MockResponse? {
            // Remove .json extension
            val nameWithoutExtension = fileName.removeSuffix(suffix = ".json")

            // Split by '-' to get parts
            val parts = nameWithoutExtension.split("-")

            // Need at least endpointId and statusCode
            if (parts.size < 2) return null

            // Parse status code (second part)
            val statusCode = parts[1].toIntOrNull() ?: return null

            // Generate display name
            val displayName = generateDisplayName(
                statusCode = statusCode,
                suffixParts = parts.drop(n = 2)
            )

            return MockResponse(
                statusCode = statusCode,
                fileName = fileName,
                displayName = displayName,
                content = content
            )
        }

        /**
         * Generates a human-readable display name for the response.
         *
         * @param statusCode The HTTP status code
         * @param suffixParts Optional suffix parts from the file name (e.g., ["simple"], ["not", "found"])
         * @return A formatted display name
         */
        @Suppress("CommentOverPrivateFunction")
        private fun generateDisplayName(statusCode: Int, suffixParts: List<String>): String {
            val statusText = getStatusText(statusCode = statusCode)
            val suffix = if (suffixParts.isNotEmpty()) {
                " - " + suffixParts.joinToString(separator = " ") {
                    it.replaceFirstChar { c -> c.uppercase() }
                }
            } else {
                ""
            }
            return "$statusText$suffix ($statusCode)"
        }

        /**
         * Gets the standard HTTP status text for a status code.
         *
         * @param statusCode The HTTP status code
         * @return The status text (e.g., "Success", "Not Found", "Server Error")
         */
        @Suppress("CyclomaticComplexMethod", "CommentOverPrivateFunction")
        private fun getStatusText(statusCode: Int): String = when (statusCode) {
            // 2xx Success
            200 -> "Success"
            201 -> "Created"
            202 -> "Accepted"
            204 -> "No Content"

            // 3xx Redirection
            301 -> "Moved Permanently"
            302 -> "Found"
            304 -> "Not Modified"

            // 4xx Client Errors
            400 -> "Bad Request"
            401 -> "Unauthorized"
            403 -> "Forbidden"
            404 -> "Not Found"
            405 -> "Method Not Allowed"
            409 -> "Conflict"
            422 -> "Unprocessable Entity"
            429 -> "Too Many Requests"

            // 5xx Server Errors
            500 -> "Server Error"
            501 -> "Not Implemented"
            502 -> "Bad Gateway"
            503 -> "Service Unavailable"
            504 -> "Gateway Timeout"

            // Unknown
            else -> "HTTP $statusCode"
        }
    }
}

/**
 * Represents a matched mock configuration for an incoming HTTP request.
 *
 * When the network mock plugin intercepts a request, it uses the request's
 * host, path, and method to find a matching endpoint configuration. If found,
 * this data class contains the necessary information to locate and load the
 * appropriate mock response.
 *
 * ## Matching Process
 * 1. Extract hostname from request URL
 * 2. Find matching [HostConfig] by comparing hostnames
 * 3. Find matching [EndpointConfig] by comparing path and method
 * 4. Return [MockMatch] containing host ID, endpoint ID, and config
 *
 * ## Usage in Plugin
 * ```kotlin
 * val mockMatch = mockRepository.findMatchingMock(
 *     host = "staging.api.example.com",
 *     path = "/api/v1/user/123",
 *     method = "GET"
 * )
 *
 * mockMatch?.let { match ->
 *     val endpointKey = "${match.hostId}-${match.endpointId}"
 *     val endpointState = currentState.endpointStates[endpointKey]
 *
 *     if (endpointState?.shouldUseMock() == true) {
 *         val response = mockRepository.loadMockResponse(
 *             endpointId = match.endpointId,
 *             fileName = endpointState.selectedResponseFile!!
 *         )
 *         // Return mock response
 *     }
 * }
 * ```
 *
 * @property hostId The identifier of the matched host (e.g., "staging")
 * @property endpointId The identifier of the matched endpoint (e.g., "getUser")
 * @property config The complete endpoint configuration
 * @see MockConfiguration
 * @see EndpointConfig
 * @see com.worldline.devview.networkmock.repository.MockConfigRepository.findMatchingMock
 */
public data class MockMatch(val hostId: String, val endpointId: String, val config: EndpointConfig)

/**
 * Represents an available endpoint with its mock responses and current state.
 *
 * This data class combines endpoint configuration, discovered response files,
 * and current runtime state to provide a complete view of an endpoint's
 * mocking capabilities. It's primarily used by the UI layer to display
 * available endpoints and their configurations.
 *
 * ## UI Usage
 * The UI uses this model to display:
 * - Endpoint name and path
 * - List of available mock responses (dropdown)
 * - Current state (network vs mock, which response selected)
 * - Toggle controls
 *
 * ## Usage Example
 * ```kotlin
 * @Composable
 * fun EndpointCard(endpoint: AvailableEndpointMock) {
 *     Card {
 *         Text("${endpoint.config.method} ${endpoint.config.path}")
 *         Text(endpoint.config.name)
 *
 *         // Toggle between network and mock
 *         Switch(
 *             checked = endpoint.currentState.mockEnabled,
 *             onCheckedChange = { enabled ->
 *                 viewModel.setEndpointMockEnabled(endpoint, enabled)
 *             }
 *         )
 *
 *         // Response selector (only when mock enabled)
 *         if (endpoint.currentState.mockEnabled) {
 *             DropdownMenu {
 *                 endpoint.availableResponses.forEach { response ->
 *                     DropdownMenuItem(
 *                         text = { Text(response.displayName) },
 *                         onClick = {
 *                             viewModel.selectResponse(endpoint, response.fileName)
 *                         }
 *                     )
 *                 }
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * @property hostId The host identifier this endpoint belongs to
 * @property endpointId The endpoint identifier
 * @property config The endpoint configuration from mocks.json
 * @property availableResponses List of discovered mock response files
 * @property currentState The current runtime state for this endpoint
 * @see MockResponse
 * @see EndpointConfig
 * @see EndpointMockState
 */
public data class AvailableEndpointMock(
    val hostId: String,
    val endpointId: String,
    val config: EndpointConfig,
    val availableResponses: List<MockResponse>,
    val currentState: EndpointMockState
)
