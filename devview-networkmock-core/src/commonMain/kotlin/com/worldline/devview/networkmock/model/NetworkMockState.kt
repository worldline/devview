package com.worldline.devview.networkmock.model

import androidx.compose.runtime.Immutable
import com.worldline.devview.networkmock.utils.parseStatusCode
import kotlin.time.Clock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

/**
 * Represents the complete state of network mocking, persisted in DataStore.
 *
 * This data class encapsulates all runtime state for the network mock feature,
 * including the global mocking toggle and individual endpoint configurations.
 * The state is persisted using DataStore Preferences to survive app restarts.
 *
 * ## State Structure
 * - **Global Toggle**: Master switch to enable/disable all mocking
 * - **Endpoint States**: Per-endpoint configuration (mock enabled + selected response)
 * - **Metadata**: Timestamp tracking for state changes
 *
 * ## Persistence
 * State is automatically saved to DataStore when changes are made through the
 * [com.worldline.devview.networkmock.repository.MockStateRepository]:
 * ```kotlin
 * val repository = MockStateRepository(dataStore)
 *
 * // Enable global mocking
 * repository.setGlobalMockingEnabled(true)
 *
 * // Configure specific endpoint
 * repository.setEndpointMockState(
 *     key = EndpointKey("my-backend", "staging", "getUser"),
 *     state = EndpointMockState.Mock(responseFile = "getUser-200.json")
 * )
 * ```
 *
 * ## Two-Level Toggle System
 * The plugin uses a two-level check:
 * 1. **Global Level**: If [globalMockingEnabled] is `false`, ALL requests use actual network
 * 2. **Endpoint Level**: If global is `true`, check individual [endpointStates] for each request
 *
 * This allows quick testing with/without mocking while preserving individual configurations.
 *
 * ## Environment Resolution
 * There is no stored "active environment" selection. The environment is derived at runtime
 * by matching the incoming request's hostname against the [com.worldline.devview.networkmock.model.EnvironmentConfig.url]
 * of each environment across all API groups. This allows the app to simultaneously target
 * different environments for different API groups without any manual selection.
 *
 * ## Endpoint State Keys
 * Endpoint states are keyed by `"{groupId}-{environmentId}-{endpointId}"` to guarantee
 * uniqueness across all combinations. This prevents collisions between API groups that
 * happen to share the same environment ID and endpoint ID:
 * ```kotlin
 * val key = "my-backend-staging-getUser"     // my-backend group, staging environment, getUser endpoint
 * val state = networkMockState.endpointStates[key]
 * ```
 *
 * @property globalMockingEnabled Master toggle — when `false`, all mocking is disabled
 * @property endpointStates Map of endpoint states, keyed by `"{groupId}-{environmentId}-{endpointId}"`
 * @property lastModified Timestamp (milliseconds since epoch) of last state modification
 * @see EndpointMockState
 * @see com.worldline.devview.networkmock.repository.MockStateRepository
 */
@Serializable
public data class NetworkMockState(
    val globalMockingEnabled: Boolean = false,
    val endpointStates: Map<String, EndpointMockState> = emptyMap(),
    val lastModified: Long = 0L
) {
    /**
     * Gets the mock state for a specific endpoint identified by an [EndpointKey].
     *
     * @param key The [EndpointKey] identifying the group, environment, and endpoint
     * @return The [EndpointMockState] if configured, or `null` if not set
     */
    public fun getEndpointState(key: EndpointKey): EndpointMockState? =
        endpointStates[key.compositeKey]

    /**
     * Gets the mock state for a specific endpoint in a specific group and environment.
     *
     * Convenience overload of [getEndpointState] that accepts three separate string
     * identifiers instead of an [EndpointKey]. Delegates to the [EndpointKey] overload.
     *
     * @param groupId The [com.worldline.devview.networkmock.model.ApiGroupConfig] identifier
     * @param environmentId The [com.worldline.devview.networkmock.model.EnvironmentConfig] identifier
     * @param endpointId The [com.worldline.devview.networkmock.model.EndpointConfig] identifier
     * @return The [EndpointMockState] if configured, or `null` if not set
     */
    public fun getEndpointState(
        groupId: String,
        environmentId: String,
        endpointId: String
    ): EndpointMockState? = getEndpointState(
        key = EndpointKey(groupId = groupId, environmentId = environmentId, endpointId = endpointId)
    )

    /**
     * Creates a new state with the specified endpoint state updated.
     *
     * @param key The [EndpointKey] identifying the group, environment, and endpoint
     * @param state The new endpoint state
     * @return A new [NetworkMockState] with the updated endpoint state
     */
    public fun withEndpointState(key: EndpointKey, state: EndpointMockState): NetworkMockState =
        copy(
            endpointStates = endpointStates + (key.compositeKey to state),
            lastModified = Clock.System.now().toEpochMilliseconds()
        )

    /**
     * Creates a new state with the specified endpoint state updated.
     *
     * Convenience overload of [withEndpointState] that accepts three separate string
     * identifiers instead of an [EndpointKey]. Delegates to the [EndpointKey] overload.
     *
     * @param groupId The [com.worldline.devview.networkmock.model.ApiGroupConfig] identifier
     * @param environmentId The [com.worldline.devview.networkmock.model.EnvironmentConfig] identifier
     * @param endpointId The [com.worldline.devview.networkmock.model.EndpointConfig] identifier
     * @param state The new endpoint state
     * @return A new [NetworkMockState] with the updated endpoint state
     */
    public fun withEndpointState(
        groupId: String,
        environmentId: String,
        endpointId: String,
        state: EndpointMockState
    ): NetworkMockState = withEndpointState(
        key = EndpointKey(
            groupId = groupId,
            environmentId = environmentId,
            endpointId = endpointId
        ),
        state = state
    )

    /**
     * Creates a new state with all endpoint mocks reset to use the actual network.
     *
     * Each endpoint state is replaced with [EndpointMockState.Network], regardless
     * of its previous value.
     *
     * @return A new [NetworkMockState] with all endpoints set to [EndpointMockState.Network]
     */
    public fun resetAllToNetwork(): NetworkMockState = copy(
        endpointStates = endpointStates.mapValues { EndpointMockState.Network },
        lastModified = Clock.System.now().toEpochMilliseconds()
    )
}

/**
 * Represents the mocking state for a single API endpoint.
 *
 * Each endpoint is either passing traffic through to the actual network or
 * returning a specific mock response. The two variants are represented as
 * distinct types, eliminating any ambiguous state combinations that existed
 * in the previous boolean-flag approach.
 *
 * ## Variants
 *
 * | Variant | Behavior | [displayName] |
 * |---------|----------|---------------|
 * | [Network] | All requests pass through to the actual network (default) | `"Network"` |
 * | [Mock] | Requests return the mock response loaded from [Mock.responseFile] | [Mock.responseFile] without `.json` |
 *
 * ## Usage Example
 * ```kotlin
 * // Configure endpoint to use a mock response
 * val state = EndpointMockState.Mock(responseFile = "getUser-200.json")
 *
 * // Configure endpoint to use the actual network
 * val state = EndpointMockState.Network
 * ```
 *
 * ## Checking the state
 * ```kotlin
 * when (state) {
 *     is EndpointMockState.Network -> { /* use real network */ }
 *     is EndpointMockState.Mock    -> { /* load mock from state.responseFile */ }
 * }
 * ```
 *
 * ## Response File Naming Convention
 * The [Mock.responseFile] should match one of the available response files
 * for the endpoint, following the naming convention:
 * - `{endpointId}-{statusCode}.json`
 * - `{endpointId}-{statusCode}-{suffix}.json`
 *
 * Example files for a `getUser` endpoint:
 * - `getUser-200.json`
 * - `getUser-404-simple.json`
 * - `getUser-404-detailed.json`
 * - `getUser-500.json`
 *
 * @see NetworkMockState
 */
@Immutable
@Serializable
@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("type")
public sealed interface EndpointMockState {
    /**
     * A human-readable display name for this state, suitable for use in UI labels.
     *
     * - [Network]: always `"Network"`
     * - [Mock]: the [Mock.responseFile] name without its `.json` extension
     *   (e.g. `"getUser-200"` for `"getUser-200.json"`)
     */
    public val displayName: String

    /**
     * The endpoint will pass all requests through to the actual network.
     *
     * This is the default state for every endpoint. No mock response will be
     * loaded or returned.
     */
    @Immutable
    @Serializable
    @SerialName("network")
    public data object Network : EndpointMockState {
        override val displayName: String = "Network"
    }

    /**
     * The endpoint will return a mock response loaded from [responseFile].
     *
     * @property responseFile The file name of the selected mock response
     *   (e.g. `"getUser-200.json"`). Used as the key to load the response via
     *   [com.worldline.devview.networkmock.repository.MockConfigRepository.loadMockResponse].
     *   The status code and display name are derived from this name at runtime —
     *   they are not stored here to avoid redundancy with [MockResponse].
     */
    @Immutable
    @Serializable
    @SerialName("mock")
    public data class Mock(val responseFile: String) : EndpointMockState {
        /**
         * The response file name without its `.json` extension, used as a
         * concise UI label (e.g. `"getUser-200"` for `"getUser-200.json"`).
         */
        override val displayName: String get() = responseFile.removeSuffix(suffix = ".json")

        /**
         * The HTTP status code parsed from [responseFile], or `null` if the file
         * name does not match the expected `{endpointId}-{statusCode}[-{suffix}].json`
         * format.
         *
         * Computed on each access by delegating to
         * [com.worldline.devview.networkmock.utils.parseStatusCode] — the single
         * source of truth for status-code extraction from response file names.
         *
         * ```kotlin
         * val state = EndpointMockState.Mock(responseFile = "getUser-404-simple.json")
         * println(state.statusCode) // 404
         * ```
         */
        public val statusCode: Int? get() = responseFile.parseStatusCode()
    }
}
