package com.worldline.devview.networkmock.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.worldline.devview.networkmock.model.EndpointMockState
import com.worldline.devview.networkmock.model.NetworkMockState
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okio.IOException

/**
 * Repository for persisting and retrieving network mock state using DataStore.
 *
 * This repository manages the runtime state of the network mock feature, including
 * the global mocking toggle and individual endpoint configurations. All state changes
 * are persisted to DataStore and survive app restarts.
 *
 * ## Responsibilities
 * - Persist [NetworkMockState] to DataStore Preferences
 * - Provide reactive [Flow] of state changes for UI observation
 * - Provide one-time state reads for plugin usage
 * - Update individual endpoint states
 * - Handle reset operations
 *
 * ## DataStore Schema
 * Each piece of state is stored under its own typed preference key:
 * - `network_mock_global_enabled`: Boolean — global mocking toggle
 * - `network_mock_last_modified`: Long — timestamp of last modification
 * - `network_mock_endpoint_{hostId}-{endpointId}`: String — JSON-serialized
 *   [EndpointMockState] for each individual endpoint, stored separately so
 *   that updating one endpoint does not affect others
 *
 * An in-memory registry of known endpoint keys is maintained alongside the
 * DataStore to allow enumeration of all endpoints without scanning all keys.
 *
 * ## Usage
 *
 * ### Creating the Repository
 * ```kotlin
 * val dataStore = createDataStore { ... }
 * val repository = MockStateRepository(dataStore)
 * ```
 *
 * ### Observing State in UI
 * ```kotlin
 * @Composable
 * fun NetworkMockScreen(repository: MockStateRepository) {
 *     val state by repository.observeState()
 *         .collectAsStateWithLifecycle(NetworkMockState())
 *
 *     if (state.globalMockingEnabled) {
 *         Text("Mocking is ENABLED")
 *     }
 * }
 * ```
 *
 * ### Using in Plugin
 * ```kotlin
 * client.plugin(HttpSend).intercept { requestBuilder ->
 *     val currentState = stateRepository.getState()
 *
 *     if (!currentState.globalMockingEnabled) {
 *         return@intercept execute(requestBuilder)
 *     }
 *     // ... check endpoint states
 * }
 * ```
 *
 * ### Updating State
 * ```kotlin
 * // Enable global mocking
 * repository.setGlobalMockingEnabled(true)
 *
 * // Configure endpoint
 * repository.setEndpointMockState(
 *     hostId = "staging",
 *     endpointId = "getUser",
 *     state = EndpointMockState.Mock(responseFile = "getUser-200.json")
 * )
 *
 * // Reset all to network
 * repository.resetKnownEndpointsToNetwork()
 * ```
 *
 * ## Thread Safety
 * All DataStore operations are thread-safe and asynchronous. The repository
 * can be safely accessed from multiple coroutines.
 *
 * @property dataStore The DataStore instance for persisting preferences
 * @see NetworkMockState
 * @see EndpointMockState
 */
public class MockStateRepository(private val dataStore: DataStore<Preferences>) {
    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
    }

    /**
     * In-memory registry of known endpoint preference keys.
     *
     * Maps `"{hostId}-{endpointId}"` to its corresponding [Preferences.Key].
     * Populated as endpoints are written to DataStore. Used to enumerate all
     * known endpoints without scanning all DataStore keys.
     */
    @Suppress("DocumentationOverPrivateProperty")
    private val endpointKeys: MutableMap<String, Preferences.Key<String>> = mutableMapOf()

    private companion object {
        val KEY_GLOBAL_ENABLED = booleanPreferencesKey(name = "network_mock_global_enabled")
        val KEY_LAST_MODIFIED = longPreferencesKey(name = "network_mock_last_modified")

        /** Prefix used for all per-endpoint preference keys. */
        const val ENDPOINT_KEY_PREFIX = "network_mock_endpoint_"
    }

    /**
     * Returns the [Preferences.Key] for a specific endpoint, creating and
     * registering it in [endpointKeys] if not already present.
     *
     * @param hostId The host identifier
     * @param endpointId The endpoint identifier
     * @return The [Preferences.Key] for this endpoint's [EndpointMockState]
     */
    @Suppress("DocumentationOverPrivateFunction")
    private fun endpointKey(hostId: String, endpointId: String): Preferences.Key<String> {
        val compositeKey = "$hostId-$endpointId"
        return endpointKeys.getOrPut(key = compositeKey) {
            stringPreferencesKey(name = "$ENDPOINT_KEY_PREFIX$compositeKey")
        }
    }

    /**
     * Observes the network mock state as a reactive [Flow].
     *
     * This flow emits the current state whenever any changes are made to the
     * DataStore preferences. Each endpoint state is read from its own individual
     * preference key and assembled into a [NetworkMockState].
     *
     * The flow handles errors gracefully by catching [IOException] and emitting
     * the default empty state instead of crashing.
     *
     * ## Usage in Compose
     * ```kotlin
     * @Composable
     * fun NetworkMockScreen(repository: MockStateRepository) {
     *     val state by repository.observeState()
     *         .collectAsStateWithLifecycle(NetworkMockState())
     *
     *     Text("Global mocking: ${if (state.globalMockingEnabled) "ON" else "OFF"}")
     * }
     * ```
     *
     * @return A [Flow] that emits [NetworkMockState] whenever the state changes
     */
    public fun observeState(): Flow<NetworkMockState> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(value = emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            // Scan ALL keys stored in DataStore that match our endpoint prefix.
            // This replaces the previous approach of iterating over the in-memory
            // `endpointKeys` registry, which is empty on a cold start because it
            // is only populated when a write operation is performed in the current
            // session. Without this change, all persisted endpoint states were
            // silently ignored on startup and every endpoint appeared as Network.
            val endpointStates = preferences
                .asMap()
                .entries
                .filter { (key, _) -> key.name.startsWith(prefix = ENDPOINT_KEY_PREFIX) }
                .associate { (key, rawValue) ->
                    val compositeKey = key.name.removePrefix(prefix = ENDPOINT_KEY_PREFIX)
                    // Keep the in-memory registry in sync so write-side helpers
                    // (setEndpointMockState, resetKnownEndpointsToNetwork, etc.)
                    // remain consistent for the rest of the session.
                    endpointKeys.getOrPut(key = compositeKey) {
                        stringPreferencesKey(name = key.name)
                    }
                    compositeKey to
                        @Suppress("SwallowedException", "TooGenericExceptionCaught")
                        try {
                            json.decodeFromString<EndpointMockState>(
                                string = rawValue as String
                            )
                        } catch (e: SerializationException) {
                            EndpointMockState.Network
                        } catch (e: IllegalArgumentException) {
                            EndpointMockState.Network
                        }
                }
            NetworkMockState(
                globalMockingEnabled = preferences[KEY_GLOBAL_ENABLED] ?: false,
                endpointStates = endpointStates,
                lastModified = preferences[KEY_LAST_MODIFIED] ?: 0L
            )
        }

    /**
     * Gets the current network mock state as a one-time read.
     *
     * Unlike [observeState], this does not observe changes — it simply reads
     * the current value once. Useful for plugin usage where a single snapshot
     * of the state is needed per request.
     *
     * @return The current [NetworkMockState]
     */
    public suspend fun getState(): NetworkMockState = observeState().first()

    /**
     * Sets the global mocking enabled state.
     *
     * When global mocking is disabled, all network requests will use the actual
     * network regardless of individual endpoint configurations.
     *
     * ```kotlin
     * repository.setGlobalMockingEnabled(true)  // Enable all mocking
     * repository.setGlobalMockingEnabled(false) // Disable all mocking
     * ```
     *
     * @param enabled `true` to enable global mocking, `false` to disable
     */
    public suspend fun setGlobalMockingEnabled(enabled: Boolean) {
        println(message = "[NetworkMock][State] Setting global mocking enabled: $enabled")
        dataStore.edit { preferences ->
            preferences[KEY_GLOBAL_ENABLED] = enabled
            preferences[KEY_LAST_MODIFIED] = Clock.System.now().toEpochMilliseconds()
        }
        println(message = "[NetworkMock][State] Global mocking state saved to DataStore")
    }

    /**
     * Sets the mock state for a specific endpoint.
     *
     * The endpoint state is stored under its own individual preference key
     * (`network_mock_endpoint_{hostId}-{endpointId}`), so updating one endpoint
     * does not affect any other endpoint's stored state.
     *
     * ```kotlin
     * repository.setEndpointMockState(
     *     hostId = "staging",
     *     endpointId = "getUser",
     *     state = EndpointMockState.Mock(responseFile = "getUser-200.json")
     * )
     * ```
     *
     * @param hostId The host identifier (e.g., "staging", "production")
     * @param endpointId The endpoint identifier (e.g., "getUser", "createPost")
     * @param state The new endpoint mock state
     */
    public suspend fun setEndpointMockState(
        hostId: String,
        endpointId: String,
        state: EndpointMockState
    ) {
        val stateDescription = when (state) {
            is EndpointMockState.Network -> "network"
            is EndpointMockState.Mock -> "mock, file=${state.responseFile}, status=${state.statusCode}"
        }
        println(
            message = "[NetworkMock][State] Setting endpoint state: $hostId-$endpointId, $stateDescription"
        )
        val key = endpointKey(hostId = hostId, endpointId = endpointId)
        dataStore.edit { preferences ->
            preferences[key] = json.encodeToString(value = state)
            preferences[KEY_LAST_MODIFIED] = Clock.System.now().toEpochMilliseconds()
        }
    }

    /**
     * Overwrites the stored endpoint states with the provided map.
     *
     * This is used by the ViewModel's reset operation to write a disabled state
     * for **every endpoint in the configuration**, including those that have never
     * been touched by the user and therefore have no existing DataStore entry.
     *
     * Each entry is written to its own individual preference key.
     *
     * ## Behavior
     * - Global mocking state: **Unchanged**
     * - Endpoint states: Each entry written to its own key
     * - Last modified timestamp: Updated to current time
     *
     * @param states Map of `"{hostId}-{endpointId}"` keys to [EndpointMockState] values
     */
    public suspend fun setAllEndpointStates(states: Map<String, EndpointMockState>) {
        println(
            message = "[NetworkMock][State] Setting all endpoint states (${states.size} entries)"
        )
        dataStore.edit { preferences ->
            states.forEach { (compositeKey, state) ->
                val (hostId, endpointId) = compositeKey.split("-", limit = 2)
                val key = endpointKey(hostId = hostId, endpointId = endpointId)
                preferences[key] = json.encodeToString(value = state)
            }
            preferences[KEY_LAST_MODIFIED] = Clock.System.now().toEpochMilliseconds()
        }
    }

    /**
     * Resets all known endpoint mocks to use the actual network.
     *
     * Iterates over all keys in the in-memory [endpointKeys] registry and sets
     * each endpoint's state to [EndpointMockState.Network]. Endpoints that have
     * never been written to DataStore are not affected — they already default to network.
     *
     * For a full reset across all configured endpoints (including untouched ones),
     * use the ViewModel's `resetAllToNetwork()` which drives the reset from the
     * configuration.
     *
     * ## Behavior
     * - Global mocking state: **Unchanged**
     * - All stored endpoint states: Set to [EndpointMockState.Network]
     * - Last modified timestamp: Updated to current time
     */
    public suspend fun resetKnownEndpointsToNetwork() {
        dataStore.edit { preferences ->
            endpointKeys.values.forEach { key ->
                preferences[key] = json.encodeToString(value = EndpointMockState.Network)
            }
            preferences[KEY_LAST_MODIFIED] = Clock.System.now().toEpochMilliseconds()
        }
    }

    /**
     * Pre-registers a set of endpoint identifiers into the in-memory [endpointKeys]
     * registry without performing any DataStore I/O.
     *
     * ## Why this exists
     * The [endpointKeys] registry is normally populated lazily on the first write
     * for each endpoint. If the screen is opened before any write has occurred in
     * the current session (i.e. on every cold start), the registry is empty.
     * Although [observeState] was updated to scan `preferences.asMap()` to work
     * around this, pre-registering keys here is a belt-and-suspenders measure that
     * ensures consistency: once the configuration is loaded, the [endpointKeys]
     * map always reflects the full set of configured endpoints, so all write-side
     * helpers ([resetKnownEndpointsToNetwork], etc.) operate on the complete set
     * rather than only the subset that happened to be written this session.
     *
     * Call this once, immediately after the mock configuration has been loaded
     * (before any writes).
     *
     * @param endpoints List of `(hostId, endpointId)` pairs from the loaded
     *   [com.worldline.devview.networkmock.model.MockConfiguration]
     */
    public fun registerEndpoints(endpoints: List<Pair<String, String>>) {
        endpoints.forEach { (hostId, endpointId) ->
            endpointKey(hostId = hostId, endpointId = endpointId)
        }
    }
}
