package com.worldline.devview.networkmock.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Root configuration for network mocking, loaded from `mocks.json`.
 *
 * This data class represents the complete mock configuration that integrators
 * define in their `composeResources/files/networkmocks/mocks.json` file. It
 * contains all API group configurations, each of which defines a set of shared
 * endpoints and the environments (deployment stages) in which those endpoints
 * are reachable.
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
 *         {
 *           "id": "staging",
 *           "name": "Staging",
 *           "url": "https://staging.api.example.com"
 *         },
 *         {
 *           "id": "production",
 *           "name": "Production",
 *           "url": "https://api.example.com"
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
 * config?.apiGroups?.forEach { group ->
 *     println("Group: ${group.id}")
 *     group.environments.forEach { env ->
 *         println("  Environment: ${env.id} - ${env.url}")
 *     }
 *     group.endpoints.forEach { endpoint ->
 *         println("  Endpoint: ${endpoint.method} ${endpoint.path}")
 *     }
 * }
 * ```
 *
 * @property apiGroups List of API group configurations, each representing a named
 *   logical backend with its shared endpoints and environment-specific base URLs
 * @see ApiGroupConfig
 * @see EnvironmentConfig
 * @see EndpointConfig
 * @see com.worldline.devview.networkmock.repository.MockConfigRepository
 */
@Serializable
public data class MockConfiguration(val apiGroups: List<ApiGroupConfig>)

/**
 * Configuration for a named, stable API group (e.g. "my-backend", "jsonplaceholder").
 *
 * An API group is the top-level organisational unit in the mock configuration. It
 * represents a logical backend that the application communicates with, regardless
 * of which environment is active. It defines:
 * - A shared pool of [endpoints] that exist across all environments
 * - A list of [environments], each providing the base URL for this group at a
 *   given deployment stage, along with optional per-environment endpoint overrides
 *   and additions
 *
 * ## Shared Endpoints vs. Environment-Specific Variations
 * Endpoints are defined once in [endpoints] and shared across all environments.
 * When an endpoint differs in a specific environment (different path, name or method),
 * an [EndpointOverride] can be declared inside that [EnvironmentConfig]. Endpoints
 * that only exist in a specific environment are declared in
 * [EnvironmentConfig.additionalEndpoints].
 *
 * ## Example
 * ```json
 * {
 *   "id": "my-backend",
 *   "name": "My Backend",
 *   "endpoints": [
 *     { "id": "getUser", "name": "Get User", "path": "/v1/users/{userId}", "method": "GET" }
 *   ],
 *   "environments": [
 *     { "id": "staging", "name": "Staging", "url": "https://staging.api.example.com" },
 *     { "id": "production", "name": "Production", "url": "https://api.example.com" }
 *   ]
 * }
 * ```
 *
 * @property id Stable unique identifier for this group (e.g. `"my-backend"`, `"jsonplaceholder"`).
 *   Used as the first path segment of mock response files:
 *   `responses/{id}/{endpointId}/` or `responses/{id}/{environmentId}/{endpointId}/`
 * @property name Human-readable name displayed in the UI (e.g. `"My Backend"`)
 * @property endpoints Shared endpoint definitions, available in all environments unless
 *   overridden or omitted via [EnvironmentConfig.endpointOverrides] /
 *   [EnvironmentConfig.additionalEndpoints]
 * @property environments List of deployment stages for this group, each providing
 *   the resolved base URL and optional endpoint customisations
 * @see EnvironmentConfig
 * @see EndpointConfig
 * @see EndpointOverride
 */
@Serializable
public data class ApiGroupConfig(
    val id: String,
    val name: String,
    val endpoints: List<EndpointConfig>,
    val environments: List<EnvironmentConfig>
)

/**
 * Configuration for a single deployment environment within an [ApiGroupConfig].
 *
 * An environment represents one stage in the delivery pipeline (e.g. staging,
 * production, development). It provides the resolved base URL for its parent
 * [ApiGroupConfig] at that stage, and optionally customises the shared endpoint
 * pool via [endpointOverrides] and [additionalEndpoints].
 *
 * ## Endpoint Resolution
 * When the active environment is matched during request interception, the effective
 * endpoint list for this environment is built as follows:
 * 1. Start with [ApiGroupConfig.endpoints] (the shared pool)
 * 2. Apply [endpointOverrides] — each override merges into the matching shared
 *    endpoint by [EndpointOverride.id]; unspecified fields keep their shared value
 * 3. Append [additionalEndpoints] — endpoints that only exist in this environment
 *
 * ## Example
 * ```json
 * {
 *   "id": "production",
 *   "name": "Production",
 *   "url": "https://api.example.com",
 *   "endpointOverrides": [
 *     { "id": "getUser", "path": "/v1/users/{userId}/profile" }
 *   ],
 *   "additionalEndpoints": [
 *     { "id": "getLegacyUser", "name": "Get Legacy User", "path": "/users/{userId}", "method": "GET" }
 *   ]
 * }
 * ```
 *
 * @property id Unique identifier for this environment (e.g. `"staging"`, `"production"`).
 *   Used as the second path segment of environment-specific mock response files:
 *   `responses/{groupId}/{id}/{endpointId}/`
 *   and as part of the DataStore endpoint state key: `"{id}-{endpointId}"`
 * @property name Human-readable name displayed in the UI (e.g. `"Staging"`, `"Production"`)
 * @property url The base URL for the parent API group in this environment
 *   (scheme + hostname, e.g. `"https://staging.api.example.com"`).
 *   The hostname is extracted at runtime and compared against incoming request hosts.
 * @property endpointOverrides Optional list of partial endpoint replacements. Each entry
 *   references a shared endpoint by [EndpointOverride.id] and overrides only the
 *   fields it specifies. Defaults to an empty list.
 * @property additionalEndpoints Optional list of endpoints that only exist in this
 *   environment (e.g. staging-only debug endpoints). Defaults to an empty list.
 * @see ApiGroupConfig
 * @see EndpointOverride
 * @see EndpointConfig
 */
@Serializable
public data class EnvironmentConfig(
    val id: String,
    val name: String,
    val url: String,
    val endpointOverrides: List<EndpointOverride> = emptyList(),
    val additionalEndpoints: List<EndpointConfig> = emptyList()
)

/**
 * Common contract for endpoint definitions in the mock configuration.
 *
 * This sealed interface is implemented by both [EndpointConfig] (a complete,
 * standalone endpoint definition) and [EndpointOverride] (a partial, environment-specific
 * replacement). Both share the same set of fields; the distinction is that all fields
 * are non-null in [EndpointConfig] whereas [EndpointOverride] makes them nullable to
 * express "only override the fields that differ".
 *
 * Having a shared interface allows the effective-endpoint resolution logic to operate
 * uniformly over both types without casting:
 * ```kotlin
 * val effective: EndpointConfig = sharedEndpoint.applyOverride(override)
 * ```
 *
 * @see EndpointConfig
 * @see EndpointOverride
 * @see ApiGroupConfig.effectiveEndpoints
 */
public sealed interface EndpointDefinition {
    /** Endpoint identifier — non-null in [EndpointConfig], non-null in [EndpointOverride] (used as lookup key). */
    public val id: String

    /** Human-readable display name — non-null in [EndpointConfig], nullable in [EndpointOverride]. */
    public val name: String?

    /** API path, may contain `{param}` placeholders — non-null in [EndpointConfig], nullable in [EndpointOverride]. */
    public val path: String?

    /** HTTP method (GET, POST, …) — non-null in [EndpointConfig], nullable in [EndpointOverride]. */
    public val method: String?
}

/**
 * Configuration for a single API endpoint that can be mocked.
 *
 * An endpoint represents a specific API call (combination of HTTP method and path)
 * that can have multiple mock responses. Endpoints are defined in the shared pool
 * of an [ApiGroupConfig] and apply to all environments unless overridden via
 * [EndpointOverride] or supplemented via [EnvironmentConfig.additionalEndpoints].
 *
 * All fields are non-null — this is a complete, self-contained endpoint definition.
 * Use [EndpointOverride] to express a partial replacement for a specific environment.
 *
 * Mock response files for an endpoint should be placed at:
 * ```
 * responses/{groupId}/{environmentId}/{endpointId}/   ← environment-specific (highest priority)
 * responses/{groupId}/{endpointId}/                   ← shared fallback (lowest priority)
 * ```
 * following the naming convention: `{endpointId}-{statusCode}[-{suffix}].json`
 *
 * ## Path Parameters
 * Paths can include parameters using curly braces notation. Parameters will
 * match any value in that position:
 * - Path: `/api/users/{userId}` matches `/api/users/123`, `/api/users/abc`, etc.
 * - Path: `/api/posts/{postId}/comments/{commentId}` matches any values for both IDs
 *
 * ## Response File Convention
 * For an endpoint with `id = "getUser"` in group `"my-backend"`:
 * ```
 * responses/my-backend/getUser/
 *   ├── getUser-200.json           (Shared success response)
 *   └── getUser-404.json           (Shared not found response)
 * responses/my-backend/staging/getUser/
 *   └── getUser-200.json           (Staging-specific success response — overrides shared)
 * ```
 *
 * ## Usage Example
 * ```json
 * {
 *   "id": "getUser",
 *   "name": "Get User Profile",
 *   "path": "/v1/users/{userId}",
 *   "method": "GET"
 * }
 * ```
 *
 * @property id Unique identifier for this endpoint within its [ApiGroupConfig].
 *   Used for state persistence, file discovery, and override matching.
 * @property name Human-readable name displayed in the UI
 * @property path API path with optional `{param}` placeholders (e.g. `"/v1/users/{userId}"`)
 * @property method HTTP method (GET, POST, PUT, DELETE, PATCH, etc.)
 * @see EndpointDefinition
 * @see EndpointOverride
 * @see ApiGroupConfig
 * @see EnvironmentConfig
 * @see MockResponse
 */
@Immutable
@Serializable
public data class EndpointConfig(
    override val id: String,
    override val name: String,
    override val path: String,
    override val method: String
) : EndpointDefinition

/**
 * A partial override for a shared [EndpointConfig] within a specific [EnvironmentConfig].
 *
 * Implements [EndpointDefinition] alongside [EndpointConfig], sharing the same field
 * names. The key difference is that [name], [path] and [method] are nullable here —
 * a `null` value means "keep the shared value from [ApiGroupConfig.endpoints]".
 * Only [id] is non-null, as it is the lookup key that identifies which shared endpoint
 * this override targets.
 *
 * ## Merge Behaviour
 * Given a shared endpoint:
 * ```json
 * { "id": "getUser", "name": "Get User", "path": "/v1/users/{userId}", "method": "GET" }
 * ```
 * And an override:
 * ```json
 * { "id": "getUser", "path": "/v1/users/{userId}/profile" }
 * ```
 * The effective endpoint for this environment becomes:
 * ```
 * id:     getUser
 * name:   Get User                     ← from shared (null in override → keep shared)
 * path:   /v1/users/{userId}/profile   ← from override
 * method: GET                          ← from shared (null in override → keep shared)
 * ```
 *
 * ## Merge Logic
 * The merge is performed by [ApiGroupConfig.effectiveEndpoints], which calls:
 * ```kotlin
 * sharedEndpoint.copy(
 *     name   = override.name   ?: sharedEndpoint.name,
 *     path   = override.path   ?: sharedEndpoint.path,
 *     method = override.method ?: sharedEndpoint.method
 * )
 * ```
 *
 * @property id The [EndpointConfig.id] of the shared endpoint to override.
 *   Must match an entry in [ApiGroupConfig.endpoints]; unrecognised IDs are ignored.
 * @property name Replacement display name, or `null` to keep the shared value
 * @property path Replacement API path, or `null` to keep the shared value
 * @property method Replacement HTTP method, or `null` to keep the shared value
 * @see EndpointDefinition
 * @see EndpointConfig
 * @see EnvironmentConfig
 * @see ApiGroupConfig
 */
@Serializable
public data class EndpointOverride(
    override val id: String,
    override val name: String? = null,
    override val path: String? = null,
    override val method: String? = null
) : EndpointDefinition

/**
 * A stable, value-type identifier for the triple (groupId, environmentId, endpointId).
 *
 * This key uniquely addresses a single endpoint within a specific deployment environment
 * of a specific API group. It is used everywhere the three identifiers travel together —
 * as a map key, as a lookup token, and as the carrier inside [MockMatch] and
 * [EndpointDescriptor] — so that callers always have named access to each component
 * instead of relying on positional destructuring of a raw [Triple].
 *
 * ## Composite string key
 * Use [compositeKey] to obtain the `"{groupId}-{environmentId}-{endpointId}"` string
 * required by DataStore and [com.worldline.devview.networkmock.model.NetworkMockState]:
 * ```kotlin
 * val key = EndpointKey("my-backend", "staging", "getUser").compositeKey
 * // "my-backend-staging-getUser"
 * ```
 *
 * @property groupId The [ApiGroupConfig.id] (e.g. `"my-backend"`)
 * @property environmentId The [EnvironmentConfig.id] (e.g. `"staging"`)
 * @property endpointId The [EndpointConfig.id] (e.g. `"getUser"`)
 * @see MockMatch
 * @see EndpointDescriptor
 * @see com.worldline.devview.networkmock.model.NetworkMockState
 * @see com.worldline.devview.networkmock.repository.MockStateRepository
 * @see com.worldline.devview.networkmock.repository.MockConfigRepository
 */
@Immutable
@Serializable
public data class EndpointKey(
    val groupId: String,
    val environmentId: String,
    val endpointId: String
) {
    /**
     * The canonical `"{groupId}-{environmentId}-{endpointId}"` string used as a
     * DataStore preference key suffix and as the key in
     * [com.worldline.devview.networkmock.model.NetworkMockState.endpointStates].
     */
    public val compositeKey: String get() = "$groupId-$environmentId-$endpointId"

    public companion object
}

/**
 * Represents a matched mock configuration for an incoming HTTP request.
 *
 * When the network mock plugin intercepts a request, it uses the active environment,
 * the request host, path, and method to find a matching endpoint configuration.
 * If found, this data class contains the necessary information to locate and load
 * the appropriate mock response.
 *
 * ## Matching Process
 * 1. Iterate over all [ApiGroupConfig] entries in the configuration
 * 2. For each group, find the [EnvironmentConfig] whose hostname extracted from
 *    [EnvironmentConfig.url] matches the incoming request host
 * 3. Build the effective endpoint list for this group+environment (shared + overrides + additions)
 * 4. Find the [EndpointConfig] matching the request path and method
 * 5. Return a [MockMatch] with the resolved identifiers
 *
 * ## Usage in Plugin
 * ```kotlin
 * val mockMatch = mockRepository.findMatchingMock(
 *     host = "staging.api.example.com",
 *     path = "/v1/users/123",
 *     method = "GET"
 * )
 *
 * mockMatch?.let { match ->
 *     val endpointState = currentState.endpointStates[match.key.compositeKey]
 *     // ...
 * }
 * ```
 *
 * @property key The [EndpointKey] carrying the matched group, environment, and endpoint identifiers
 * @property config The complete effective endpoint configuration after override resolution
 * @see MockConfiguration
 * @see ApiGroupConfig
 * @see EnvironmentConfig
 * @see EndpointConfig
 * @see EndpointKey
 * @see com.worldline.devview.networkmock.repository.MockConfigRepository.findMatchingMock
 */
@Immutable
public data class MockMatch(val key: EndpointKey, val config: EndpointConfig) {
    /** The [ApiGroupConfig.id] of the matched group. Convenience accessor for [EndpointKey.groupId]. */
    public val groupId: String get() = key.groupId

    /** The [EnvironmentConfig.id] of the matched environment. Convenience accessor for [EndpointKey.environmentId]. */
    public val environmentId: String get() = key.environmentId

    /** The [EndpointConfig.id] of the matched endpoint. Convenience accessor for [EndpointKey.endpointId]. */
    public val endpointId: String get() = key.endpointId
}

/**
 * Represents the static descriptor for an available endpoint and its mock responses.
 *
 * This data class combines endpoint configuration and discovered response files
 * to provide a complete, immutable view of an endpoint's mocking capabilities.
 * It is primarily used by the UI layer to display available endpoints and their
 * configurations.
 *
 * Runtime selection state is intentionally excluded from this model. It changes
 * on every user interaction and belongs in the UI layer paired with this descriptor,
 * keeping this class safe to snapshot, store, and pass freely without going stale.
 *
 * ## UI Usage
 * The UI uses this model to display:
 * - Endpoint name and path
 * - List of available mock responses
 *
 * @property key The [EndpointKey] uniquely identifying this endpoint within its group and environment
 * @property config The effective endpoint configuration after override resolution
 * @property availableResponses List of discovered mock response files for this
 *   group + environment + endpoint combination
 * @see MockResponse
 * @see EndpointKey
 * @see EndpointConfig
 * @see ApiGroupConfig
 * @see EnvironmentConfig
 */
@Immutable
public data class EndpointDescriptor(
    val key: EndpointKey,
    val config: EndpointConfig,
    val availableResponses: List<MockResponse>
) {
    /**
     * The [ApiGroupConfig.id] this endpoint belongs to. Convenience accessor for [EndpointKey.groupId].
     */
    public val groupId: String get() = key.groupId

    /**
     * The [EnvironmentConfig.id] this descriptor was resolved for. Convenience accessor for
     * [EndpointKey.environmentId].
     */
    public val environmentId: String get() = key.environmentId

    /**
     * The [EndpointConfig.id] for this endpoint. Convenience accessor for [EndpointKey.endpointId].
     */
    public val endpointId: String get() = key.endpointId

    public companion object
}

/**
 * Builds the effective endpoint list for a given [ApiGroupConfig] and [EnvironmentConfig] pair.
 *
 * This is the single source of truth for endpoint resolution. It combines the group's
 * shared [ApiGroupConfig.endpoints] pool with the environment's [EnvironmentConfig.endpointOverrides]
 * and [EnvironmentConfig.additionalEndpoints] to produce the complete, ready-to-use list of
 * [EndpointConfig] entries that apply to this specific group + environment combination.
 *
 * ## Resolution Steps
 * 1. Start with [ApiGroupConfig.endpoints] (the shared pool)
 * 2. For each shared endpoint, check whether [EnvironmentConfig.endpointOverrides] contains
 *    an entry whose [EndpointOverride.id] matches — if so, merge it: non-null override fields
 *    replace the shared values, null override fields keep the shared values
 * 3. Append [EnvironmentConfig.additionalEndpoints] — these endpoints are unique to this
 *    environment and are added as-is after the resolved shared endpoints
 *
 * ## Override Merge Example
 * Shared endpoint:
 * ```
 * EndpointConfig(id="getUser", name="Get User", path="/v1/users/{userId}", method="GET")
 * ```
 * Override:
 * ```
 * EndpointOverride(id="getUser", path="/v1/users/{userId}/profile")
 * ```
 * Result:
 * ```
 * EndpointConfig(id="getUser", name="Get User", path="/v1/users/{userId}/profile", method="GET")
 * ```
 *
 * ## Usage
 * ```kotlin
 * val effectiveEndpoints = group.effectiveEndpoints(environment)
 * val match = effectiveEndpoints.firstOrNull { endpoint ->
 *     RequestMatcher.matchesPath(endpoint.path, requestPath) && endpoint.method == requestMethod
 * }
 * ```
 *
 * @receiver The [ApiGroupConfig] providing the shared endpoint pool
 * @param environment The [EnvironmentConfig] providing overrides and additions
 * @return The fully resolved list of [EndpointConfig] for this group + environment,
 *   in order: resolved shared endpoints first, then additional endpoints
 * @see EndpointOverride
 * @see EnvironmentConfig.endpointOverrides
 * @see EnvironmentConfig.additionalEndpoints
 */
public fun ApiGroupConfig.effectiveEndpoints(environment: EnvironmentConfig): List<EndpointConfig> {
    val overrideMap = environment.endpointOverrides.associateBy { it.id }
    val resolved = endpoints.map { shared ->
        val override = overrideMap[shared.id]
        if (override == null) {
            shared
        } else {
            shared.copy(
                name = override.name ?: shared.name,
                path = override.path ?: shared.path,
                method = override.method ?: shared.method
            )
        }
    }
    return resolved + environment.additionalEndpoints
}
