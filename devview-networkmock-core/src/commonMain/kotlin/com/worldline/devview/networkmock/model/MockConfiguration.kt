package com.worldline.devview.networkmock.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Root configuration for network mocking, loaded from `mocks.json`.
 *
 * This data class represents the complete mock configuration that integrators
 * define in their `composeResources/files/networkmocks/mocks.json` file. It
 * contains all host configurations and their associated API endpoints that
 * can be mocked during development and testing.
 *
 * ## File Location
 * The configuration file should be placed at:
 * ```
 * composeResources/files/networkmocks/mocks.json
 * ```
 *
 * ## JSON Structure
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
 * ## Usage Example
 * ```kotlin
 * val repository = MockConfigRepository("files/networkmocks/mocks.json")
 * val config = repository.loadConfiguration().getOrNull()
 *
 * config?.hosts?.forEach { host ->
 *     println("Host: ${host.id} - ${host.url}")
 *     host.endpoints.forEach { endpoint ->
 *         println("  - ${endpoint.method} ${endpoint.path}")
 *     }
 * }
 * ```
 *
 * @property hosts List of host configurations, each containing API endpoints
 * @see HostConfig
 * @see EndpointConfig
 * @see com.worldline.devview.networkmock.repository.MockConfigRepository
 */
@Serializable
public data class MockConfiguration(val hosts: List<HostConfig>)

/**
 * Configuration for a specific API host and its endpoints.
 *
 * A host represents a backend server (e.g., staging, production, development)
 * that the application communicates with. Each host can have multiple API
 * endpoints that can be individually mocked.
 *
 * ## Usage in Configuration
 * Multiple hosts can be defined to support different environments:
 * ```json
 * {
 *   "hosts": [
 *     {
 *       "id": "staging",
 *       "url": "https://staging.api.example.com",
 *       "endpoints": [...]
 *     },
 *     {
 *       "id": "production",
 *       "url": "https://api.example.com",
 *       "endpoints": [...]
 *     }
 *   ]
 * }
 * ```
 *
 * ## Host Matching
 * When a network request is made, the plugin extracts the hostname from the
 * request URL and compares it against configured hosts. Only endpoints from
 * the matching host will be considered for mocking.
 *
 * @property id Unique identifier for this host (used in UI and state persistence)
 * @property url The base URL of the API host (scheme + hostname, e.g., "https://api.example.com")
 * @property endpoints List of API endpoints available on this host
 * @see EndpointConfig
 */
@Serializable
public data class HostConfig(val id: String, val url: String, val endpoints: List<EndpointConfig>)

/**
 * Configuration for a single API endpoint that can be mocked.
 *
 * An endpoint represents a specific API call (combination of HTTP method and path)
 * that can have multiple mock responses. The actual mock response files should
 * be placed in `composeResources/files/networkmocks/responses/{endpointId}/`
 * following the naming convention: `{endpointId}-{statusCode}[-{suffix}].json`
 *
 * ## Path Parameters
 * Paths can include parameters using curly braces notation. Parameters will
 * match any value in that position:
 * - Path: `/api/users/{userId}` matches `/api/users/123`, `/api/users/abc`, etc.
 * - Path: `/api/posts/{postId}/comments/{commentId}` matches any values for both IDs
 *
 * ## Response File Convention
 * For an endpoint with `id = "getUser"`, response files should be named:
 * ```
 * responses/getUser/
 *   ├── getUser-200.json           (Success response)
 *   ├── getUser-404-simple.json    (Not found - simple error)
 *   ├── getUser-404-detailed.json  (Not found - detailed error)
 *   └── getUser-500.json           (Server error)
 * ```
 *
 * ## Usage Example
 * ```json
 * {
 *   "id": "getUser",
 *   "name": "Get User Profile",
 *   "path": "/api/v1/user/{userId}",
 *   "method": "GET"
 * }
 * ```
 *
 * @property id Unique identifier for this endpoint within its host (used for file discovery and state persistence)
 * @property name Human-readable name displayed in the UI
 * @property path API path with optional parameters in curly braces (e.g., "/api/users/{userId}")
 * @property method HTTP method (GET, POST, PUT, DELETE, PATCH, etc.)
 * @see HostConfig
 * @see MockResponse
 */
@Immutable
@Serializable
public data class EndpointConfig(
    val id: String,
    val name: String,
    val path: String,
    val method: String
)
