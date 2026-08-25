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
    private val privateOperationMocks = MutableStateFlow<Map<OperationKey, OperationDescriptor>>(
        value = emptyMap()
    )

    /**
     * Combined UI state for the Network Mock screen.
     *
     * Combines [MockConfiguration] (loaded once from the configured OpenAPI specs), the live
     * [com.worldline.devview.networkmock.core.model.NetworkMockState] from DataStore, the internal
     * loading state, and the discovered [OperationDescriptor] map into a single
     * [NetworkMockUiState] emission. Re-emits whenever any of the four sources change.
     *
     * Each [com.worldline.devview.networkmock.core.model.ApiSpec] in the configuration becomes
     * one [ApiSpecUiModel] tab. Within each tab, only operations whose [OperationDescriptor]
     * has already been discovered are included.
     *
     * @see NetworkMockUiState
     * @see ApiSpecUiModel
     */
    public val uiState: StateFlow<NetworkMockUiState> = combine(
        flow = privateConfiguration,
        flow2 = stateRepository.observeState(),
        flow3 = privateLoadingState,
        flow4 = privateOperationMocks
    ) { config, runtimeState, loadingState, operationMocks ->
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
                                        .mapNotNull { operation ->
                                            val key = OperationKey(
                                                specId = spec.id,
                                                operationId = operation.operationId
                                            )
                                            operationMocks[key]?.let { descriptor ->
                                                OperationUiModel(
                                                    descriptor = descriptor,
                                                    currentState = runtimeState
                                                        .getOperationState(key = key)
                                                        ?: OperationMockState.Network
                                                )
                                            }
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

    init {
        loadConfiguration()
    }

    /**
     * Loads the mock configuration from the configured OpenAPI specs and discovers response
     * variants for every operation.
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

                    // Discover declared response variants for every operation
                    val mocks = mutableMapOf<OperationKey, OperationDescriptor>()
                    config.specs.forEach { spec ->
                        spec.operations.forEach { operation ->
                            val key = OperationKey(
                                specId = spec.id,
                                operationId = operation.operationId
                            )
                            val responses = configRepository.discoverResponseFiles(key = key)
                            mocks[key] = OperationDescriptor(
                                key = key,
                                config = operation,
                                availableResponses = responses
                            )
                        }
                    }

                    privateOperationMocks.value = mocks
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
