package com.worldline.devview.networkmock.model

import kotlin.time.Clock
import kotlinx.serialization.Serializable

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
 *     hostId = "staging",
 *     endpointId = "getUser",
 *     state = EndpointMockState(
 *         mockEnabled = true,
 *         selectedResponseFile = "getUser-200.json"
 *     )
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
 * ## Endpoint State Keys
 * Endpoint states are keyed by `"{hostId}-{endpointId}"` to ensure uniqueness
 * across multiple hosts that might have endpoints with the same ID:
 * ```kotlin
 * val key = "staging-getUser"  // For staging host's getUser endpoint
 * val state = networkMockState.endpointStates[key]
 * ```
 *
 * @property globalMockingEnabled Master toggle - when false, all mocking is disabled
 * @property endpointStates Map of endpoint states, keyed by "{hostId}-{endpointId}"
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
     * Gets the state for a specific endpoint.
     *
     * @param hostId The host identifier
     * @param endpointId The endpoint identifier
     * @return The [EndpointMockState] if configured, or null if not set
     */
    public fun getEndpointState(hostId: String, endpointId: String): EndpointMockState? {
        val key = "$hostId-$endpointId"
        return endpointStates[key]
    }

    /**
     * Creates a new state with the specified endpoint state updated.
     *
     * @param hostId The host identifier
     * @param endpointId The endpoint identifier
     * @param state The new endpoint state
     * @return A new [NetworkMockState] with the updated endpoint state
     */
    public fun withEndpointState(
        hostId: String,
        endpointId: String,
        state: EndpointMockState
    ): NetworkMockState {
        val key = "$hostId-$endpointId"
        return copy(
            endpointStates = endpointStates + (key to state),
            lastModified = Clock.System.now().toEpochMilliseconds()
        )
    }

    /**
     * Creates a new state with all endpoint mocks disabled (reset to network).
     *
     * @return A new [NetworkMockState] with all endpoints set to use actual network
     */
    public fun resetAllToNetwork(): NetworkMockState = copy(
        endpointStates = endpointStates.mapValues { (_, state) ->
            state.copy(mockEnabled = false, selectedResponseFile = null)
        },
        lastModified = Clock.System.now().toEpochMilliseconds()
    )
}

/**
 * Represents the mocking state for a single API endpoint.
 *
 * Each endpoint can be individually configured to either use the actual network
 * or return a specific mock response. This allows granular control over which
 * APIs are mocked during development and testing.
 *
 * ## State Combinations
 *
 * | mockEnabled | selectedResponseFile | Behavior |
 * |-------------|---------------------|----------|
 * | `false`     | `null`              | Use actual network (default) |
 * | `false`     | `"getUser-200.json"`| Use actual network (selection ignored) |
 * | `true`      | `null`              | Use actual network (no response selected) |
 * | `true`      | `"getUser-200.json"`| Return mock response from file |
 *
 * ## Usage Example
 * ```kotlin
 * // Configure endpoint to use mock
 * val state = EndpointMockState(
 *     mockEnabled = true,
 *     selectedResponseFile = "getUser-200.json"
 * )
 *
 * // Configure endpoint to use actual network
 * val state = EndpointMockState(
 *     mockEnabled = false,
 *     selectedResponseFile = null
 * )
 * ```
 *
 * ## Response File Selection
 * The [selectedResponseFile] should match one of the available response files
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
 * @property mockEnabled Whether to use mock response (true) or actual network (false)
 * @property selectedResponseFile The response file name to use when mocking, or null for network
 * @see NetworkMockState
 * @see MockResponse
 */
@Serializable
public data class EndpointMockState(
    val mockEnabled: Boolean = false,
    val selectedResponseFile: String? = null
) {
    /**
     * Checks if this endpoint should return a mock response.
     *
     * For mocking to be active, both [mockEnabled] must be true AND
     * [selectedResponseFile] must be non-null.
     *
     * @return true if a mock response should be returned, false otherwise
     */
    public fun shouldUseMock(): Boolean = mockEnabled && selectedResponseFile != null

    /**
     * Creates a new state with mocking disabled (reset to network).
     *
     * @return A new [EndpointMockState] configured to use actual network
     */
    public fun resetToNetwork(): EndpointMockState = EndpointMockState(
        mockEnabled = false,
        selectedResponseFile = null
    )
}
