package com.worldline.devview.networkmock.core.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.worldline.devview.networkmock.core.model.NetworkMockState
import com.worldline.devview.networkmock.core.model.OperationKey
import com.worldline.devview.networkmock.core.model.OperationMockState
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okio.IOException

/**
 * Repository for persisting and retrieving network mock state using DataStore.
 *
 * ## DataStore Schema
 * - `network_mock_global_enabled`: Boolean — global mocking toggle
 * - `network_mock_last_modified`: Long — timestamp of last modification
 * - `network_mock_schema_version`: Int — see [pruneLegacyState]
 * - `network_mock_operation_{specId}-{operationId}`: String — JSON-serialized
 *   [OperationMockState] for each individual operation, stored separately so that updating
 *   one operation does not affect others
 *
 * There is no stored active spec/server selection — a spec's servers are matched at runtime
 * against the incoming request's hostname (see
 * [MockConfigRepository.findMatchingMock]).
 *
 * An in-memory registry of known operation keys is maintained alongside DataStore to allow
 * enumeration of all operations without scanning all keys.
 *
 * @property dataStore The DataStore instance for persisting preferences
 * @see NetworkMockState
 * @see OperationMockState
 */
public class MockStateRepository(private val dataStore: DataStore<Preferences>) {
    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
    }

    /**
     * In-memory registry of known operation preference keys, keyed by [OperationKey.compositeKey].
     * Populated as operations are written to DataStore. Used to enumerate all known operations
     * without scanning all DataStore keys.
     */
    @Suppress("DocumentationOverPrivateProperty")
    private val operationKeys: MutableMap<String, Preferences.Key<String>> = mutableMapOf()

    private companion object {
        val KEY_GLOBAL_ENABLED = booleanPreferencesKey(name = "network_mock_global_enabled")
        val KEY_LAST_MODIFIED = longPreferencesKey(name = "network_mock_last_modified")
        val KEY_SCHEMA_VERSION = intPreferencesKey(name = "network_mock_schema_version")

        /** Prefix for all per-operation preference keys, written under the current schema. */
        const val OPERATION_KEY_PREFIX = "network_mock_operation_"

        /**
         * Prefix used by every pre-0.2.0 `EndpointKey(groupId, environmentId, endpointId)`
         * entry. Orphaned by the [OperationKey] key-shape change and the `Mock` payload shape
         * change (file name -> `(statusCode, exampleName)`) — see [pruneLegacyState]. Never
         * written under this prefix again; kept only so the migration knows what to remove.
         */
        const val LEGACY_ENDPOINT_KEY_PREFIX = "network_mock_endpoint_"

        /** Bump when the persisted key or payload shape changes again. */
        const val CURRENT_SCHEMA_VERSION = 1
    }

    /**
     * Returns the [Preferences.Key] for a specific operation identified by an [OperationKey],
     * creating and registering it in [operationKeys] if not already present.
     */
    @Suppress("DocumentationOverPrivateFunction")
    private fun operationKey(key: OperationKey): Preferences.Key<String> =
        operationKeys.getOrPut(key = key.compositeKey) {
            stringPreferencesKey(name = "$OPERATION_KEY_PREFIX${key.compositeKey}")
        }

    /**
     * Removes every DataStore entry written under the pre-0.2.0 key shape, once.
     *
     * The 0.2.0 migration changes both the operation-state key shape (3-tuple `EndpointKey`
     * → 2-tuple [OperationKey]) and the `Mock` payload shape (response file name →
     * `(statusCode, exampleName)`), so any pre-existing `network_mock_endpoint_*` entry is
     * unreadable under the new shape regardless of which change orphaned it. Rather than
     * attempt a best-effort translation of two incompatible shapes at once, every such entry
     * is wiped — this is disabled-by-default developer-tooling state, not user data, so
     * losing previously-selected mocks across the upgrade is an acceptable trade for not
     * carrying a second parser for a one-time migration.
     *
     * Gated by [KEY_SCHEMA_VERSION] so this only ever performs the wipe once; safe to call
     * on every entry point since it's a cheap no-op once the version is current.
     * [KEY_GLOBAL_ENABLED] and [KEY_LAST_MODIFIED] are untouched.
     */
    @Suppress("DocumentationOverPrivateFunction")
    private suspend fun pruneLegacyState() {
        val schemaVersion = dataStore.data.first()[KEY_SCHEMA_VERSION] ?: 0
        if (schemaVersion >= CURRENT_SCHEMA_VERSION) return

        dataStore.edit { preferences ->
            preferences
                .asMap()
                .keys
                .map { it.name }
                .filter { it.startsWith(prefix = LEGACY_ENDPOINT_KEY_PREFIX) }
                .forEach { name -> preferences.remove(key = stringPreferencesKey(name = name)) }
            preferences[KEY_SCHEMA_VERSION] = CURRENT_SCHEMA_VERSION
        }
    }

    /**
     * Observes the network mock state as a reactive [Flow].
     *
     * Emits the current state whenever any changes are made to the DataStore preferences.
     * Each operation state is read from its own individual preference key and assembled into
     * a [NetworkMockState]. Errors are handled gracefully by catching [IOException] and
     * emitting the default empty state instead of crashing.
     *
     * @return A [Flow] that emits [NetworkMockState] whenever the state changes
     */
    public fun observeState(): Flow<NetworkMockState> = dataStore.data
        .onStart { pruneLegacyState() }
        .catch { exception ->
            if (exception is IOException) {
                emit(value = emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            val operationStates = preferences
                .asMap()
                .entries
                .filter { (key, _) -> key.name.startsWith(prefix = OPERATION_KEY_PREFIX) }
                .associate { (key, rawValue) ->
                    val compositeKey = key.name.removePrefix(prefix = OPERATION_KEY_PREFIX)
                    operationKeys.getOrPut(key = compositeKey) {
                        stringPreferencesKey(name = key.name)
                    }
                    compositeKey to
                        @Suppress("SwallowedException", "TooGenericExceptionCaught")
                        try {
                            json.decodeFromString<OperationMockState>(
                                string = rawValue as String
                            )
                        } catch (e: SerializationException) {
                            OperationMockState.Network
                        } catch (e: IllegalArgumentException) {
                            OperationMockState.Network
                        }
                }
            NetworkMockState(
                globalMockingEnabled = preferences[KEY_GLOBAL_ENABLED] ?: false,
                operationStates = operationStates,
                lastModified = preferences[KEY_LAST_MODIFIED] ?: 0L
            )
        }.distinctUntilChanged()

    /**
     * Gets the current network mock state as a one-time read.
     *
     * Unlike [observeState], this does not observe changes — it simply reads the current
     * value once. Useful for plugin usage where a single snapshot of the state is needed per
     * request.
     *
     * @return The current [NetworkMockState]
     */
    public suspend fun getState(): NetworkMockState = observeState().first()

    /**
     * Sets the global mocking enabled state.
     *
     * When global mocking is disabled, all network requests will use the actual network
     * regardless of individual operation configurations.
     *
     * @param enabled `true` to enable global mocking, `false` to disable
     */
    public suspend fun setGlobalMockingEnabled(enabled: Boolean) {
        pruneLegacyState()
        dataStore.edit { preferences ->
            preferences[KEY_GLOBAL_ENABLED] = enabled
            preferences[KEY_LAST_MODIFIED] = Clock.System.now().toEpochMilliseconds()
        }
    }

    /**
     * Sets the mock state for a specific operation identified by an [OperationKey].
     *
     * The operation state is stored under its own individual preference key, so updating one
     * operation does not affect any other operation's stored state.
     *
     * @param key The [OperationKey] identifying the spec and operation
     * @param state The new operation mock state
     */
    public suspend fun setOperationMockState(key: OperationKey, state: OperationMockState) {
        pruneLegacyState()
        val prefKey = operationKey(key = key)
        dataStore.edit { preferences ->
            preferences[prefKey] = json.encodeToString(value = state)
            preferences[KEY_LAST_MODIFIED] = Clock.System.now().toEpochMilliseconds()
        }
    }

    /**
     * Overwrites the stored operation states with the provided map.
     *
     * Used by the ViewModel's reset operation to write a disabled state for **every operation
     * in the configuration**, including those that have never been touched by the user and
     * therefore have no existing DataStore entry. Each entry is written to its own individual
     * preference key. Global mocking state is unchanged.
     *
     * @param states Map of [OperationKey] identifiers to [OperationMockState] values
     */
    public suspend fun setAllOperationStates(states: Map<OperationKey, OperationMockState>) {
        pruneLegacyState()
        dataStore.edit { preferences ->
            states.forEach { (key, state) ->
                preferences[operationKey(key = key)] = json.encodeToString(value = state)
            }
            preferences[KEY_LAST_MODIFIED] = Clock.System.now().toEpochMilliseconds()
        }
    }

    /**
     * Resets all known operation mocks to use the actual network.
     *
     * Iterates over all keys in the in-memory [operationKeys] registry and sets each
     * operation's state to [OperationMockState.Network]. Operations that have never been
     * written to DataStore are not affected — they already default to network. Global
     * mocking state is unchanged.
     */
    public suspend fun resetKnownOperationsToNetwork() {
        pruneLegacyState()
        dataStore.edit { preferences ->
            operationKeys.values.forEach { key ->
                preferences[key] = json.encodeToString(value = OperationMockState.Network)
            }
            preferences[KEY_LAST_MODIFIED] = Clock.System.now().toEpochMilliseconds()
        }
    }

    /**
     * Pre-registers a set of operation identifiers into the in-memory [operationKeys]
     * registry without performing any DataStore I/O.
     *
     * Call this once, immediately after the mock configuration has been loaded (before any
     * writes), so write-side helpers like [resetKnownOperationsToNetwork] operate on the
     * complete set of configured operations rather than only the subset written this session.
     *
     * @param operations List of [OperationKey] values from the loaded
     *   [com.worldline.devview.networkmock.core.model.MockConfiguration]
     */
    public fun registerOperations(operations: List<OperationKey>) {
        operations.forEach { key ->
            operationKey(key = key)
        }
    }
}
