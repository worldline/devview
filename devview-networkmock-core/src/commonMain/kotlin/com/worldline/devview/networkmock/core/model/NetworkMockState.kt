package com.worldline.devview.networkmock.core.model

import androidx.compose.runtime.Immutable
import kotlin.time.Clock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

/**
 * Represents the complete state of network mocking, persisted in DataStore.
 *
 * ## State Structure
 * - **Global Toggle**: Master switch to enable/disable all mocking
 * - **Operation States**: Per-operation configuration (mock enabled + selected response)
 * - **Metadata**: Timestamp tracking for state changes
 *
 * ## Two-Level Toggle System
 * The plugin uses a two-level check:
 * 1. **Global Level**: If [globalMockingEnabled] is `false`, ALL requests use actual network
 * 2. **Operation Level**: If global is `true`, check individual [operationStates] for each request
 *
 * This allows quick testing with/without mocking while preserving individual configurations.
 *
 * There is no stored "active spec" or "active server" selection — a spec's servers are
 * matched purely from the incoming request's hostname at interception time (see
 * [com.worldline.devview.networkmock.core.repository.MockConfigRepository.findMatchingMock]).
 *
 * @property globalMockingEnabled Master toggle — when `false`, all mocking is disabled
 * @property operationStates Map of operation states, keyed by [OperationKey.compositeKey]
 * @property lastModified Timestamp (milliseconds since epoch) of last state modification
 * @see OperationMockState
 * @see com.worldline.devview.networkmock.core.repository.MockStateRepository
 */
@Serializable
public data class NetworkMockState(
    val globalMockingEnabled: Boolean = false,
    val operationStates: Map<String, OperationMockState> = emptyMap(),
    val lastModified: Long = 0L
) {
    /**
     * Gets the mock state for a specific operation identified by an [OperationKey].
     *
     * @param key The [OperationKey] identifying the spec and operation
     * @return The [OperationMockState] if configured, or `null` if not set
     */
    public fun getOperationState(key: OperationKey): OperationMockState? =
        operationStates[key.compositeKey]

    /**
     * Creates a new state with the specified operation state updated.
     *
     * @param key The [OperationKey] identifying the spec and operation
     * @param state The new operation state
     * @return A new [NetworkMockState] with the updated operation state
     */
    public fun withOperationState(key: OperationKey, state: OperationMockState): NetworkMockState =
        copy(
            operationStates = operationStates + (key.compositeKey to state),
            lastModified = Clock.System.now().toEpochMilliseconds()
        )

    /**
     * Creates a new state with all operation mocks reset to use the actual network.
     *
     * Each operation state is replaced with [OperationMockState.Network], regardless
     * of its previous value.
     *
     * @return A new [NetworkMockState] with all operations set to [OperationMockState.Network]
     */
    public fun resetAllToNetwork(): NetworkMockState = copy(
        operationStates = operationStates.mapValues { OperationMockState.Network },
        lastModified = Clock.System.now().toEpochMilliseconds()
    )
}

/**
 * Represents the mocking state for a single API operation.
 *
 * Each operation is either passing traffic through to the actual network or returning a
 * specific mock response. The two variants are represented as distinct types, eliminating
 * any ambiguous state combinations that existed in a previous boolean-flag approach.
 *
 * ## Variants
 *
 * | Variant | Behavior | [displayName] |
 * |---------|----------|---------------|
 * | [Network] | All requests pass through to the actual network (default) | `"Network"` |
 * | [Mock] | Requests return the selected response variant | `"$statusCode - $exampleName"` |
 *
 * @see NetworkMockState
 */
@Immutable
@Serializable
@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("type")
public sealed interface OperationMockState {
    /**
     * A human-readable display name for this state, suitable for use in UI labels.
     *
     * - [Network]: always `"Network"`
     * - [Mock]: `"$statusCode - $exampleName"` (e.g. `"200 - default"`)
     */
    public val displayName: String

    /**
     * The operation will pass all requests through to the actual network.
     *
     * This is the default state for every operation. No mock response will be
     * loaded or returned.
     */
    @Immutable
    @Serializable
    @SerialName("network")
    public data object Network : OperationMockState {
        override val displayName: String = "Network"
    }

    /**
     * The operation will return the response variant identified by
     * `(statusCode, exampleName)`.
     *
     * This pair — not a file name — is the identity of the selected variant, since the
     * underlying `externalValue` file path is an implementation detail a spec author is free
     * to move. Both fields are required: two different status codes can each declare an
     * example named `"default"`.
     *
     * @property statusCode The HTTP status code of the selected variant
     * @property exampleName The OpenAPI example name of the selected variant
     * @see MockResponse
     */
    @Immutable
    @Serializable
    @SerialName("mock")
    public data class Mock(val statusCode: Int, val exampleName: String) : OperationMockState {
        override val displayName: String get() = "$statusCode - $exampleName"
    }
}
