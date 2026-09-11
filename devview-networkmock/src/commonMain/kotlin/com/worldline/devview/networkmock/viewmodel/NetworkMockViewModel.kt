package com.worldline.devview.networkmock.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worldline.devview.networkmock.core.model.MockConfiguration
import com.worldline.devview.networkmock.core.model.MockResponse
import com.worldline.devview.networkmock.core.model.OperationDescriptor
import com.worldline.devview.networkmock.core.model.OperationKey
import com.worldline.devview.networkmock.core.model.OperationMockState
import com.worldline.devview.networkmock.core.repository.MockConfigRepository
import com.worldline.devview.networkmock.core.repository.MockStateRepository
import com.worldline.devview.networkmock.model.ApiSpecUiModel
import com.worldline.devview.networkmock.model.OperationUiModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val WHILE_SUBSCRIBED_TIMEOUT_MS = 5000L

/**
 * ViewModel for the Network Mock screen.
 *
 * This ViewModel manages the state and business logic for the network mocking UI,
 * combining data from the OpenAPI specs and persisted state to provide a
 * complete view of available mocks and their current settings.
 *
 * ## Responsibilities
 * - Load mock configuration from the configured OpenAPI spec files
 * - Observe persisted mock state from DataStore
 * - Combine config and state into UI-friendly models
 * - Handle user actions (toggle mocking, select responses)
 * - Manage loading and error states
 *
 * @property configRepository Repository for loading mock configuration
 * @property stateRepository Repository for managing persisted state
 */
public class NetworkMockViewModel(
    private val configRepository: MockConfigRepository,
    private val stateRepository: MockStateRepository
) : ViewModel() {
    private val privateConfiguration = MutableStateFlow<MockConfiguration?>(value = null)
    private val privateLoadingState = MutableStateFlow<LoadingState>(value = LoadingState.Loading)

    private val privateOpenOperationKey = MutableStateFlow<OperationKey?>(value = null)
    private val privateLoadedOperation = MutableStateFlow<LoadedOperation?>(value = null)
    private val privateSheetError = MutableStateFlow<String?>(value = null)

    /**
     * Combined UI state for the Network Mock screen.
     *
     * Combines [MockConfiguration] (loaded once from the configured OpenAPI specs), the live
     * [com.worldline.devview.networkmock.core.model.NetworkMockState] from DataStore, and the
     * internal loading state into a single [NetworkMockUiState] emission. Re-emits whenever
     * any of the three sources change.
     *
     * Each [com.worldline.devview.networkmock.core.model.ApiSpec] in the configuration becomes
     * one [ApiSpecUiModel] tab. Response variants are **not** loaded here — this state is
     * built from spec metadata only. They're discovered lazily by [openOperation], once per
     * operation, when its picker/preview sheet is actually opened — see [sheetState].
     *
     * @see NetworkMockUiState
     * @see ApiSpecUiModel
     */
    public val uiState: StateFlow<NetworkMockUiState> = combine(
        flow = privateConfiguration,
        flow2 = stateRepository.observeState(),
        flow3 = privateLoadingState
    ) { config, runtimeState, loadingState ->
        when (loadingState) {
            is LoadingState.Loading -> NetworkMockUiState.Loading
            is LoadingState.Error -> NetworkMockUiState.Error(message = loadingState.message)
            is LoadingState.Loaded -> {
                if (config == null) {
                    NetworkMockUiState.Empty
                } else {
                    NetworkMockUiState.Content(
                        globalMockingEnabled = runtimeState.globalMockingEnabled,
                        specs = config.specs
                            .map { spec ->
                                ApiSpecUiModel(
                                    specId = spec.id,
                                    name = spec.name,
                                    operations = spec.operations
                                        .map { operation ->
                                            val key = OperationKey(
                                                specId = spec.id,
                                                operationId = operation.operationId
                                            )
                                            OperationUiModel(
                                                descriptor = OperationDescriptor(
                                                    key = key,
                                                    config = operation
                                                ),
                                                currentState = runtimeState
                                                    .getOperationState(key = key)
                                                    ?: OperationMockState.Network
                                            )
                                        }.toPersistentList()
                                )
                            }.toPersistentList()
                    )
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = WHILE_SUBSCRIBED_TIMEOUT_MS),
        initialValue = NetworkMockUiState.Loading
    )

    /**
     * State for the operation picker/preview bottom sheet, driven by [openOperation] and
     * [closeSheet].
     *
     * Combines the currently-opened [OperationKey] (or `null` if the sheet is closed), the
     * discovered [LoadedOperation] for that key (response variant discovery is per-operation
     * I/O, so it only happens once an operation is actually opened — see [openOperation]), any
     * discovery error, and the live [com.worldline.devview.networkmock.core.model.NetworkMockState]
     * so the sheet's selection always reflects the latest persisted choice.
     *
     * [LoadedOperation.key] is compared against the currently-open key rather than trusting
     * [privateLoadedOperation] alone — closing one operation's sheet and immediately opening
     * another's should never flash the previous operation's stale content while the new one is
     * still loading.
     *
     * @see openOperation
     * @see closeSheet
     */
    public val sheetState: StateFlow<OperationSheetState> = combine(
        flow = privateOpenOperationKey,
        flow2 = privateLoadedOperation,
        flow3 = privateSheetError,
        flow4 = stateRepository.observeState()
    ) { openKey, loaded, error, runtimeState ->
        when {
            openKey == null -> OperationSheetState.Hidden
            error != null -> OperationSheetState.Error(message = error)
            loaded == null || loaded.key != openKey -> OperationSheetState.Loading
            else -> OperationSheetState.Content(
                operationUiModel = OperationUiModel(
                    descriptor = loaded.descriptor,
                    currentState = runtimeState.getOperationState(key = openKey)
                        ?: OperationMockState.Network
                ),
                responses = loaded.responses
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = WHILE_SUBSCRIBED_TIMEOUT_MS),
        initialValue = OperationSheetState.Hidden
    )

    init {
        loadConfiguration()
    }

    /**
     * Opens the operation picker/preview sheet for [key], discovering its mock response
     * variants. This is the only place response bodies are read — see [uiState]'s KDoc — so
     * it happens lazily, once per sheet open, rather than eagerly for every operation in
     * every spec.
     *
     * @param key The [OperationKey] identifying the spec and operation to open
     */
    public fun openOperation(key: OperationKey) {
        privateOpenOperationKey.value = key
        privateLoadedOperation.value = null
        privateSheetError.value = null
        viewModelScope.launch {
            runCatching {
                configRepository.discoverResponseFiles(key = key)
            }.onSuccess { responses ->
                val operation = privateConfiguration.value
                    ?.specs
                    ?.firstOrNull { it.id == key.specId }
                    ?.operations
                    ?.firstOrNull { it.operationId == key.operationId }

                if (operation == null) {
                    privateSheetError.value = "Operation configuration not found"
                    return@onSuccess
                }

                privateLoadedOperation.value = LoadedOperation(
                    key = key,
                    descriptor = OperationDescriptor(key = key, config = operation),
                    responses = responses.toPersistentList()
                )
            }.onFailure { error ->
                privateSheetError.value = error.message ?: "Failed to load operation"
            }
        }
    }

    /**
     * Closes the operation picker/preview sheet, resetting [sheetState] to
     * [OperationSheetState.Hidden].
     */
    public fun closeSheet() {
        privateOpenOperationKey.value = null
        privateLoadedOperation.value = null
        privateSheetError.value = null
    }

    /**
     * Loads the mock configuration from the configured OpenAPI specs.
     *
     * This only parses spec metadata — no response body is read or decoded here. See #98:
     * that work is deferred to [openOperation], which discovers response variants for exactly
     * one operation when its picker/preview sheet is opened.
     */
    @Suppress("DocumentationOverPrivateFunction")
    private fun loadConfiguration() {
        viewModelScope.launch {
            privateLoadingState.value = LoadingState.Loading

            configRepository
                .loadConfiguration()
                .onSuccess { config ->
                    // Pre-register every OperationKey so write-side helpers have the full set
                    val allKeys = config.specs.flatMap { spec ->
                        spec.operations.map { operation ->
                            OperationKey(specId = spec.id, operationId = operation.operationId)
                        }
                    }
                    stateRepository.registerOperations(operations = allKeys)

                    privateConfiguration.value = config
                    privateLoadingState.value = LoadingState.Loaded
                }.onFailure { error ->
                    privateLoadingState.value = LoadingState.Error(
                        message = error.message ?: "Failed to load configuration"
                    )
                }
        }
    }

    /**
     * Toggles global mocking on/off.
     *
     * When disabled, every HTTP request passes through to the actual network
     * regardless of individual operation configurations. Persisted immediately
     * to DataStore so the setting survives app restarts.
     *
     * @param enabled `true` to enable global mocking, `false` to disable
     */
    public fun setGlobalMockingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            stateRepository.setGlobalMockingEnabled(enabled = enabled)
        }
    }

    /**
     * Sets the mock state for a specific operation identified by an [OperationKey].
     *
     * When [response] is `null`, the operation state is set to
     * [OperationMockState.Network], effectively disabling mocking for that operation.
     *
     * When [response] is non-null, the operation transitions to [OperationMockState.Mock]
     * with the given response's `(statusCode, exampleName)`, replacing any previous state.
     *
     * @param key The [OperationKey] identifying the spec and operation
     * @param response The response variant to activate, or `null` to use the actual network
     */
    public fun setOperationMockState(key: OperationKey, response: MockResponse?) {
        viewModelScope.launch {
            val newState = if (response != null) {
                OperationMockState.Mock(
                    statusCode = response.statusCode,
                    exampleName = response.exampleName
                )
            } else {
                OperationMockState.Network
            }
            stateRepository.setOperationMockState(key = key, state = newState)
        }
    }

    /**
     * Resets all operation mocks to use actual network.
     *
     * Builds a [OperationMockState.Network] state for every operation present in
     * the loaded configuration (not just those already stored in DataStore), then
     * persists it in one write. This ensures that operations which have never been
     * touched by the user are also explicitly reset, leaving no gaps.
     */
    public fun resetAllToNetwork() {
        viewModelScope.launch {
            val config = privateConfiguration.value
            if (config == null) {
                // Config not loaded yet — fall back to resetting only known stored entries
                stateRepository.resetKnownOperationsToNetwork()
                return@launch
            }

            // Build a Network state for every configured spec + operation
            val allNetwork = config.specs
                .flatMap { spec ->
                    spec.operations.map { operation ->
                        OperationKey(specId = spec.id, operationId = operation.operationId) to
                            OperationMockState.Network
                    }
                }.toMap()

            stateRepository.setAllOperationStates(states = allNetwork)
        }
    }
}

/**
 * UI state for the Network Mock screen.
 *
 * Emitted by [NetworkMockViewModel.uiState]. The UI renders different layouts
 * depending on which variant is active.
 */
@Immutable
public sealed interface NetworkMockUiState {
    /** Configuration is being loaded from resources. */
    @Immutable
    public data object Loading : NetworkMockUiState

    /**
     * Configuration failed to load.
     *
     * @property message Human-readable description of the failure
     */
    @Immutable
    public data class Error(val message: String) : NetworkMockUiState

    /** Configuration loaded successfully but contains no specs. */
    @Immutable
    public data object Empty : NetworkMockUiState

    /**
     * Configuration loaded successfully and at least one spec is available.
     *
     * @property globalMockingEnabled Whether the global mocking master switch is on
     * @property specs One entry per [com.worldline.devview.networkmock.core.model.ApiSpec],
     *   each rendered as a tab in the UI
     */
    @Immutable
    public data class Content(
        val globalMockingEnabled: Boolean,
        val specs: PersistentList<ApiSpecUiModel>
    ) : NetworkMockUiState
}

/**
 * Internal loading state.
 */
private sealed interface LoadingState {
    data object Loading : LoadingState

    data object Loaded : LoadingState

    data class Error(val message: String) : LoadingState
}

/**
 * State for the operation picker/preview bottom sheet.
 *
 * Emitted by [NetworkMockViewModel.sheetState]. [Hidden] means no sheet is shown; the other
 * three variants mirror the discovery lifecycle of a single opened operation.
 */
@Immutable
public sealed interface OperationSheetState {
    /** No operation is open — the sheet is not shown. */
    @Immutable
    public data object Hidden : OperationSheetState

    /** Response variant discovery is in progress for the opened operation. */
    @Immutable
    public data object Loading : OperationSheetState

    /**
     * Discovery failed or the operation configuration could not be found.
     *
     * @property message Human-readable description of the failure
     */
    @Immutable
    public data class Error(val message: String) : OperationSheetState

    /**
     * The opened operation's response variants were discovered successfully.
     *
     * @property operationUiModel The UI model combining the static `OperationDescriptor` with
     * the live [OperationMockState] for the operation, reflecting the latest persisted selection.
     * @property responses The response variants discovered for this operation.
     */
    @Immutable
    public data class Content(
        val operationUiModel: OperationUiModel,
        val responses: PersistentList<MockResponse>
    ) : OperationSheetState
}

/**
 * Internal record of a successfully-discovered operation, keyed so [NetworkMockViewModel.sheetState]
 * can tell whether it still corresponds to the currently-open operation.
 */
private data class LoadedOperation(
    val key: OperationKey,
    val descriptor: OperationDescriptor,
    val responses: PersistentList<MockResponse>
)
