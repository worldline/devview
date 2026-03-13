package com.worldline.devview.networkmock.model

import androidx.compose.runtime.Immutable
import com.worldline.devview.networkmock.utils.parseStatusCode
import kotlinx.serialization.Serializable

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
 *   - `get-user-200.json` → status 200, hyphenated endpoint ID supported
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
@Immutable
@Serializable
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
         * - `get-user-200.json` → status = 200 (hyphenated endpoint ID supported)
         *
         * Status code extraction is delegated to
         * [parseStatusCode][com.worldline.devview.networkmock.utils.parseStatusCode], which is the
         * single source of truth for this parsing logic.
         *
         * ## Custom Status Text
         * By default, status codes are mapped to human-readable text using the built-in
         * [getStatusText] function. A custom [statusTextProvider] lambda can be supplied
         * to override this — useful for future HTTP client modules or integrators with
         * non-standard status code conventions:
         * ```kotlin
         * MockResponse.fromFile(
         *     fileName = "getUser-200.json",
         *     content = responseBody,
         *     statusTextProvider = { code -> myHttpClient.statusText(code) }
         * )
         * ```
         *
         * @param fileName The response file name
         * @param content The raw JSON content as a String
         * @param statusTextProvider Optional lambda that maps a status code to its
         * display text. Defaults to the built-in [getStatusText] mapping.
         * @return A [MockResponse] instance, or null if the file name format is invalid
         */
        public fun fromFile(
            fileName: String,
            content: String,
            statusTextProvider: (Int) -> String = ::getStatusText
        ): MockResponse? {
            // Delegate status code extraction to the shared utility — single source of truth.
            val statusCode = fileName.parseStatusCode() ?: return null

            // Derive the optional suffix that follows the status code in the file name
            // (e.g. "simple" from "getUser-404-simple.json") for display name generation.
            val nameWithoutExtension = fileName.removeSuffix(suffix = ".json")
            val suffixRaw = nameWithoutExtension
                .substringAfterLast(delimiter = "-$statusCode", missingDelimiterValue = "")
                .removePrefix(prefix = "-")
            val suffixParts = if (suffixRaw.isNotEmpty()) suffixRaw.split("-") else emptyList()

            // Generate display name
            val displayName = generateDisplayName(
                statusCode = statusCode,
                suffixParts = suffixParts,
                statusTextProvider = statusTextProvider
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
         * @param statusTextProvider Lambda that maps a status code to its display text
         * @return A formatted display name
         */
        @Suppress("DocumentationOverPrivateFunction")
        private fun generateDisplayName(
            statusCode: Int,
            suffixParts: List<String>,
            statusTextProvider: (Int) -> String
        ): String {
            val statusText = statusTextProvider(statusCode)
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
        @Suppress("CyclomaticComplexMethod", "DocumentationOverPrivateFunction")
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

// MockMatch and EndpointDescriptor have been moved to MockConfiguration.kt
