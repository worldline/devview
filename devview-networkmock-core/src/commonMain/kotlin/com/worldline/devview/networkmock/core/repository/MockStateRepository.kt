package com.worldline.devview.networkmock.core.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.worldline.devview.networkmock.core.model.NetworkMockState
import com.worldline.devview.networkmock.core.model.NetworkMockStateSnapshot
import com.worldline.devview.networkmock.core.model.OperationKey
import com.worldline.devview.networkmock.core.model.OperationMockState
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okio.IOException

/**
 * Repository for persisting and retrieving network mock state using DataStore.
 *
 * ## DataStore Schema
 * - `network_mock_global_enabled`: Boolean — global mocking toggle
 * - `network_mock_last_modified`: Long — timestamp of last modification
 * - `network_mock_revision`: Long — revision shared by all state writers (defaults to zero)
 * - `network_mock_schema_version`: Int — see [pruneLegacyState]
 * - `network_mock_operation_{specId}-{operationId}`: String — JSON-serialized
 *   [OperationMockState] for each individual operation, stored separately so that updating
 *   one operation does not affect others
 *
 * There is no stored active spec/server selection — a spec's servers are matched at runtime
 * against the incoming request's hostname (see
 * [MockConfigRepository.findMatchingMock]).
 *
 * A thread-safe in-memory registry supplements persisted keys when resetting known operations.
 * State writes are atomic DataStore transactions. Sequence consumers use [getStateSnapshot]
 * and [compareAndAdvanceSequence] to avoid overwriting intervening state changes.
 *
 * @property dataStore The DataStore instance for persisting preferences
 * @see NetworkMockState
 * @see OperationMockState
 */
@Suppress("TooManyFunctions")
public class MockStateRepository(private val dataStore: DataStore<Preferences>) {
    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
    }

    private val operationKeys = MutableStateFlow<Set<String>>(value = emptySet())

    private companion object {
        val KEY_GLOBAL_ENABLED = booleanPreferencesKey(name = "network_mock_global_enabled")
        val KEY_LAST_MODIFIED = longPreferencesKey(name = "network_mock_last_modified")
        val KEY_REVISION = longPreferencesKey(name = "network_mock_revision")
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
        const val CURRENT_SCHEMA_VERSION = 2
    }

    private fun operationKey(key: OperationKey): Preferences.Key<String> =
        stringPreferencesKey(name = "$OPERATION_KEY_PREFIX${key.compositeKey}")

    private fun recordModification(preferences: MutablePreferences) {
        val revision = preferences[KEY_REVISION] ?: 0L
        check(
            value = revision in 0 until Long.MAX_VALUE
        ) { "Network mock revision exhausted or invalid" }
        preferences[KEY_REVISION] = revision + 1
        preferences[KEY_LAST_MODIFIED] = Clock.System.now().toEpochMilliseconds()
    }

    /**
     * Removes every DataStore entry written under legacy key/payload shapes, once.
     *
     * - Schema 1: wiped pre-0.2.0 `EndpointKey` entries.
     * - Schema 2: [OperationMockState.Mock] gained `mediaType`, changing the serialized payload
     *   shape from `(statusCode, exampleName)` to `(statusCode, mediaType, exampleName)`.
     *   Pre-schema-2 [OPERATION_KEY_PREFIX] entries fail deserialization under the new shape,
     *   so all existing operation mock selections are wiped once. This is developer-tooling state,
     *   not user data.
     *
     * Gated by [KEY_SCHEMA_VERSION] so this only ever performs the wipe once; safe to call
     * on every entry point since it's a cheap no-op once the version is current.
     * [KEY_GLOBAL_ENABLED] and [KEY_LAST_MODIFIED] are untouched.
     */
    @Suppress("DocumentationOverPrivateFunction")
    private suspend fun pruneLegacyState() {
        val storedVersion = dataStore.data.first()[KEY_SCHEMA_VERSION] ?: 0
        if (storedVersion >= CURRENT_SCHEMA_VERSION) return

        dataStore.edit { preferences ->
            val schemaVersion = preferences[KEY_SCHEMA_VERSION] ?: 0
            if (schemaVersion >= CURRENT_SCHEMA_VERSION) return@edit
            preferences
                .asMap()
                .keys
                .map { it.name }
                .filter { name ->
                    name.startsWith(prefix = LEGACY_ENDPOINT_KEY_PREFIX) ||
                        schemaVersion < 2 && name.startsWith(prefix = OPERATION_KEY_PREFIX)
                }.forEach { name -> preferences.remove(key = stringPreferencesKey(name = name)) }
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
        }.map { preferences -> readState(preferences = preferences) }
        .distinctUntilChanged()

    private fun readState(preferences: Preferences): NetworkMockState = NetworkMockState(
        globalMockingEnabled = preferences[KEY_GLOBAL_ENABLED] ?: false,
        operationStates = preferences
            .asMap()
            .entries
            .filter { (key, _) -> key.name.startsWith(prefix = OPERATION_KEY_PREFIX) }
            .associate { (key, rawValue) ->
                key.name.removePrefix(
                    prefix = OPERATION_KEY_PREFIX
                ) to decodeOperation(rawValue = rawValue)
            },
        lastModified = preferences[KEY_LAST_MODIFIED] ?: 0L
    )

    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    private fun decodeOperation(rawValue: Any?): OperationMockState = try {
        json.decodeFromString<OperationMockState>(string = (rawValue as? String).orEmpty())
    } catch (e: SerializationException) {
        OperationMockState.Network
    } catch (e: IllegalArgumentException) {
        OperationMockState.Network
    }

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
     * Reads state and its revision from the same persisted snapshot.
     *
     * Unlike [getState], I/O failures propagate rather than appearing as default state.
     * Use this snapshot with [compareAndAdvanceSequence] when consuming a sequence response.
     *
     * @return The current state and repository-wide revision
     */
    public suspend fun getStateSnapshot(): NetworkMockStateSnapshot {
        pruneLegacyState()
        val preferences = dataStore.data.first()
        return NetworkMockStateSnapshot(
            state = readState(preferences = preferences),
            revision = preferences[KEY_REVISION] ?: 0L
        )
    }

    /**
     * Atomically validates and consumes the response selected from [expectedSequence].
     *
     * Returns `false` without writing if the revision, global toggle or sequence changed,
     * or there is no current response. Every explicit state write invalidates older snapshots,
     * including equal-value resets and replacements. The last response repeats: it still
     * requires validation, but a successful terminal read does not increment the revision.
     * Persistence failures propagate; `true` is returned only after the transaction succeeds.
     * Order is successful commit order, not request-start or response-completion order.
     *
     * @param key Operation whose sequence response was prepared
     * @param expectedRevision Revision obtained from [getStateSnapshot]
     * @param expectedSequence Sequence used to select the prepared response
     * @return Whether the prepared response may be returned
     */
    public suspend fun compareAndAdvanceSequence(
        key: OperationKey,
        expectedRevision: Long,
        expectedSequence: OperationMockState.Sequence
    ): Boolean {
        pruneLegacyState()
        var accepted = false
        dataStore.edit { preferences ->
            val current = decodeOperation(rawValue = preferences[operationKey(key = key)])
            val currentRevision = preferences[KEY_REVISION] ?: 0L
            if (expectedSequence.currentResponse == null) return@edit
            if (currentRevision != expectedRevision ||
                preferences[KEY_GLOBAL_ENABLED] != true || current != expectedSequence
            ) {
                return@edit
            }

            val nextIndex = (expectedSequence.currentIndex + 1).coerceAtMost(
                maximumValue = expectedSequence.responses.lastIndex
            )
            if (nextIndex != expectedSequence.currentIndex) {
                preferences[operationKey(key = key)] = json.encodeToString<OperationMockState>(
                    value = expectedSequence.copy(currentIndex = nextIndex)
                )
                recordModification(preferences = preferences)
            }
            accepted = true
        }
        return accepted
    }

    /**
     * Resets the current persisted sequence to its first response in one transaction.
     *
     * Preserves the latest response list, invalidates older snapshots even if already at zero,
     * and does nothing when the operation is not currently a sequence. Failures propagate.
     *
     * @param key Operation whose sequence position should be reset
     */
    public suspend fun resetOperationSequencePosition(key: OperationKey) {
        pruneLegacyState()
        dataStore.edit { preferences ->
            val prefKey = operationKey(key = key)
            val current = decodeOperation(rawValue = preferences[prefKey])
            if (current !is OperationMockState.Sequence) return@edit
            preferences[prefKey] = json.encodeToString<OperationMockState>(
                value = current.copy(currentIndex = 0)
            )
            recordModification(preferences = preferences)
        }
    }

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
            recordModification(preferences = preferences)
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
            recordModification(preferences = preferences)
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
            recordModification(preferences = preferences)
        }
    }

    /**
     * Resets all known operation mocks to use the actual network.
     *
     * Resets both persisted entries and pre-registered operations, including entries written
     * by other repository instances. Global mocking state is unchanged. This explicit reset
     * invalidates older snapshots even when all operations already use the network.
     */
    public suspend fun resetKnownOperationsToNetwork() {
        pruneLegacyState()
        dataStore.edit { preferences ->
            val persistedKeys = preferences
                .asMap()
                .keys
                .map { it.name }
                .filter { it.startsWith(prefix = OPERATION_KEY_PREFIX) }
            (operationKeys.value + persistedKeys).forEach { name ->
                preferences[stringPreferencesKey(name = name)] =
                    json.encodeToString<OperationMockState>(value = OperationMockState.Network)
            }
            recordModification(preferences = preferences)
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
        operationKeys.update { registered ->
            registered + operations.map { key -> operationKey(key = key).name }
        }
    }
}
