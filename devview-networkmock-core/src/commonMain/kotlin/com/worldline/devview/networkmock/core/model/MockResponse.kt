package com.worldline.devview.networkmock.core.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * A loaded mock response variant, sourced from an OpenAPI response example declared in the
 * spec (`responses.<code>.content.<mediaType>.examples.<name>`, see
 * [com.worldline.devview.networkmock.core.openapi.ExampleObject]).
 *
 * The identity of a response variant is the pair `(statusCode, exampleName)`, not a file
 * name — the file path (`externalValue`) is an implementation detail a spec author is free to
 * move without changing which variant a user has selected. See [OperationMockState.Mock].
 *
 * @property statusCode The HTTP status code this variant responds with
 * @property exampleName The OpenAPI example name (e.g. `"default"`, `"detailed"`, `"empty"`).
 *   By convention, the primary/original response for a status code is named `"default"`.
 * @property displayName Human-readable name for UI display (e.g. `"Success (200)"`,
 *   `"Not Found - Detailed (404)"`)
 * @property content The raw response body, read from the example's `externalValue` file
 * @see com.worldline.devview.networkmock.core.repository.MockConfigRepository
 */
@Immutable
@Serializable
public data class MockResponse(
    val statusCode: Int,
    val exampleName: String,
    val displayName: String,
    val content: String
) {
    public companion object {
        /**
         * Creates a [MockResponse], generating a human-readable [MockResponse.displayName]
         * from [statusCode] and [exampleName].
         *
         * The `"default"` example name (by convention, the primary response for a status
         * code) contributes no suffix to the display name; any other example name is
         * title-cased and appended (e.g. `exampleName = "not-found-detailed"` →
         * `"Not Found - Not Found Detailed (404)"` is avoided by keeping example names short
         * and specific, e.g. `"detailed"` → `"Success - Detailed (200)"`).
         *
         * @param statusCode The HTTP status code
         * @param exampleName The OpenAPI example name
         * @param content The raw response body
         * @param statusTextProvider Optional lambda that maps a status code to its display
         *   text. Defaults to the built-in [getStatusText] mapping.
         * @return A [MockResponse] with a generated [MockResponse.displayName]
         */
        public fun create(
            statusCode: Int,
            exampleName: String,
            content: String,
            statusTextProvider: (Int) -> String = ::getStatusText
        ): MockResponse = MockResponse(
            statusCode = statusCode,
            exampleName = exampleName,
            displayName = generateDisplayName(
                statusCode = statusCode,
                exampleName = exampleName,
                statusTextProvider = statusTextProvider
            ),
            content = content
        )

        @Suppress("DocumentationOverPrivateFunction")
        private fun generateDisplayName(
            statusCode: Int,
            exampleName: String,
            statusTextProvider: (Int) -> String
        ): String {
            val statusText = statusTextProvider(statusCode)
            val suffix = if (exampleName.equals(other = "default", ignoreCase = true)) {
                ""
            } else {
                " - " + exampleName
                    .split("-", "_")
                    .joinToString(separator = " ") { it.replaceFirstChar { c -> c.uppercase() } }
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
