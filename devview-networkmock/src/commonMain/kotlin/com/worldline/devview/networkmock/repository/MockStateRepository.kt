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
 * The state is stored using the following preference keys:
 * - `network_mock_global_enabled`: Boolean - global mocking toggle
 * - `network_mock_endpoints`: String - JSON-serialized map of endpoint states
 * - `network_mock_last_modified`: Long - timestamp of last modification
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
 *     // Use state to render UI
 *     if (state.globalMockingEnabled) {
 *         Text("Mocking is ENABLED")
 *     }
 * }
 * ```
 *
 * ### Using in Plugin (with runBlocking)
 * ```kotlin
 * client.plugin(HttpSend).intercept { requestBuilder ->
 *     val currentState = runBlocking {
 *         stateRepository.getState()
 *     }
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
 *     state = EndpointMockState(
 *         mockEnabled = true,
 *         selectedResponseFile = "getUser-200.json"
 *     )
 * )
 *
 * // Reset all to network
 * repository.resetAllToNetwork()
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
    private val json = Json { ignoreUnknownKeys = true }

    private companion object {
        val KEY_GLOBAL_ENABLED = booleanPreferencesKey(name = "network_mock_global_enabled")
        val KEY_ENDPOINTS = stringPreferencesKey(name = "network_mock_endpoints")
        val KEY_LAST_MODIFIED = longPreferencesKey(name = "network_mock_last_modified")
    }

    /**
     * Observes the network mock state as a reactive [Flow].
     *
     * This flow emits the current state whenever any changes are made to the
     * DataStore preferences. It's designed for use in UI layers where you want
     * to reactively update the display when state changes.
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
     *
     *     state.endpointStates.forEach { (key, endpointState) ->
     *         EndpointCard(key, endpointState)
     *     }
     * }
     * ```
     *
     * ## Error Handling
     * If DataStore read fails (e.g., due to disk I/O error), the flow will
     * emit an empty [NetworkMockState] with default values instead of throwing.
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
            NetworkMockState(
                globalMockingEnabled = preferences[KEY_GLOBAL_ENABLED] ?: false,
                endpointStates = preferences[KEY_ENDPOINTS]
                    ?.let { json ->
                        @Suppress("SwallowedException", "TooGenericExceptionCaught")
                        try {
                            this.json.decodeFromString<Map<String, EndpointMockState>>(
                                string = json
                            )
                        } catch (e: Exception) {
                            emptyMap()
                        }
                    }.orEmpty(),
                lastModified = preferences[KEY_LAST_MODIFIED] ?: 0L
            )
        }

    /**
     * Gets the current network mock state as a one-time read.
     *
     * This is a suspend function that reads the current state from DataStore
     * and returns it. Unlike [observeState], this does not observe changes;
     * it simply reads the current value once.
     *
     * This method reads directly from the DataStore preferences to ensure
     * we get the most up-to-date persisted value.
     *
     * ## Usage in Plugin
     * ```kotlin
     * scope.plugin(HttpSend).intercept { requestBuilder ->
     *     val currentState = stateRepository.getState()
     *     if (!currentState.globalMockingEnabled) {
     *         return@intercept execute(requestBuilder)
     *     }
     *     // ... proceed with mock logic
     * }
     * ```
     *
     * @return The current [NetworkMockState]
     */
    public suspend fun getState(): NetworkMockState = observeState().first()

    /**
     * Sets the global mocking enabled state.
     *
     * When global mocking is disabled, all network requests will use the actual
     * network regardless of individual endpoint configurations. This provides
     * a master toggle for quickly enabling/disabling all mocking.
     *
     * The change is persisted to DataStore and will be reflected in all
     * observers of [observeState].
     *
     * ## Usage
     * ```kotlin
     * // Enable all mocking
     * repository.setGlobalMockingEnabled(true)
     *
     * // Disable all mocking (ignores individual endpoint settings)
     * repository.setGlobalMockingEnabled(false)
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
     * This updates the configuration for an individual endpoint, controlling
     * whether it uses a mock response and which response file to use.
     *
     * The endpoint is identified by the combination of host ID and endpoint ID,
     * stored as a key in the format: `"{hostId}-{endpointId}"`
     *
     * ## Usage
     * ```kotlin
     * // Enable mock for an endpoint
     * repository.setEndpointMockState(
     *     hostId = "staging",
     *     endpointId = "getUser",
     *     state = EndpointMockState(
     *         mockEnabled = true,
     *         selectedResponseFile = "getUser-200.json"
     *     )
     * )
     *
     * // Disable mock for an endpoint (use real network)
     * repository.setEndpointMockState(
     *     hostId = "staging",
     *     endpointId = "getUser",
     *     state = EndpointMockState(
     *         mockEnabled = false,
     *         selectedResponseFile = null
     *     )
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
        println(
            message = "[NetworkMock][State] Setting endpoint state: $hostId-$endpointId, " +
                "enabled=${state.mockEnabled}, file=${state.selectedResponseFile}"
        )
        dataStore.edit { preferences ->
            val currentEndpoints = preferences[KEY_ENDPOINTS]?.let {
                @Suppress("SwallowedException", "TooGenericExceptionCaught")
                try {
                    json.decodeFromString<MutableMap<String, EndpointMockState>>(string = it)
                } catch (e: Exception) {
                    mutableMapOf()
                }
            } ?: mutableMapOf()

            val key = "$hostId-$endpointId"
            currentEndpoints[key] = state

            preferences[KEY_ENDPOINTS] = json.encodeToString(value = currentEndpoints)
            preferences[KEY_LAST_MODIFIED] = Clock.System.now().toEpochMilliseconds()
        }
    }

    /**
     * Resets all endpoint mocks to use the actual network.
     *
     * This sets all endpoint states to disabled (mockEnabled = false) while
     * preserving the global mocking toggle state. It's useful for quickly
     * disabling all mocks without losing the configuration of which responses
     * were selected.
     *
     * Note: The selected response files are cleared (`selectedResponseFile = null`).
     * Users will need to re-select responses if they re-enable mocking.
     *
     * ## Usage
     * ```kotlin
     * // Reset button in UI
     * Button(onClick = {
     *     scope.launch {
     *         repository.resetAllToNetwork()
     *     }
     * }) {
     *     Text("Reset All to Network")
     * }
     * ```
     *
     * ## Behavior
     * - Global mocking state: **Unchanged**
     * - All endpoint `mockEnabled`: Set to `false`
     * - All endpoint `selectedResponseFile`: Set to `null`
     * - Last modified timestamp: Updated to current time
     */
    public suspend fun resetAllToNetwork() {
        dataStore.edit { preferences ->
            val currentEndpoints = preferences[KEY_ENDPOINTS]?.let {
                @Suppress("SwallowedException", "TooGenericExceptionCaught")
                try {
                    json.decodeFromString<MutableMap<String, EndpointMockState>>(string = it)
                } catch (e: Exception) {
                    mutableMapOf()
                }
            } ?: mutableMapOf()

            // Reset all endpoints to network
            val resetEndpoints = currentEndpoints.mapValues { (_, state) ->
                state.resetToNetwork()
            }

            preferences[KEY_ENDPOINTS] = json.encodeToString(value = resetEndpoints)
            preferences[KEY_LAST_MODIFIED] = Clock.System.now().toEpochMilliseconds()
        }
    }
}
