package com.worldline.devview.networkmock.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worldline.devview.networkmock.core.model.MockResponse
import com.worldline.devview.networkmock.core.model.OperationDescriptor
import com.worldline.devview.networkmock.core.model.OperationKey
import com.worldline.devview.networkmock.core.model.OperationMockState
import com.worldline.devview.networkmock.core.repository.MockConfigRepository
import com.worldline.devview.networkmock.core.repository.MockStateRepository
import com.worldline.devview.networkmock.model.OperationUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val WHILE_SUBSCRIBED_TIMEOUT_MS = 5000L

/**
 * ViewModel for the Network Mock operation detail screen.
 *
 * Manages the state and business logic for a single operation, combining its
 * discovered mock responses with the live persisted [OperationMockState] so the
 * UI always reflects the latest selection without any manual lookup.
 *
 * ## Responsibilities
 * - Discover available mock response variants for the given [operationKey]
 * - Observe the live [OperationMockState] for the operation from DataStore
 * - Combine both into a single [uiState] flow
 * - Handle mock state changes triggered by the user
 *
 * @property operationKey The [OperationKey] identifying the operation this screen represents
 * @property configRepository Repository used to discover available mock response variants
 * @property stateRepository Repository used to observe and persist mock state
 */
public class NetworkMockEndpointViewModel(
    private val operationKey: OperationKey,
    private val configRepository: MockConfigRepository,
    private val stateRepository: MockStateRepository
) : ViewModel() {
    private val privateDescriptor = MutableStateFlow<OperationDescriptor?>(value = null)
    private val privateLoadingState =
        MutableStateFlow<EndpointLoadingState>(value = EndpointLoadingState.Loading)

    /**
     * Combined UI state for the operation detail screen.
     *
     * Combines the discovered [OperationDescriptor] (loaded once on init) with the live
     * [OperationMockState] from DataStore into a single [NetworkMockEndpointUiState]
     * emission. Re-emits whenever either source changes — in practice, [OperationMockState]
     * changes on every user selection while [OperationDescriptor] is stable after loading.
     *
     * @see NetworkMockEndpointUiState
     */
    public val uiState: StateFlow<NetworkMockEndpointUiState> = combine(
        flow = privateDescriptor,
        flow2 = stateRepository.observeState(),
        flow3 = privateLoadingState
    ) { descriptor, runtimeState, loadingState ->
        when (loadingState) {
            is EndpointLoadingState.Loading -> NetworkMockEndpointUiState.Loading
            is EndpointLoadingState.Error -> NetworkMockEndpointUiState.Error(
                message = loadingState.message
            )
            is EndpointLoadingState.Loaded -> {
                if (descriptor == null) {
                    NetworkMockEndpointUiState.Error(message = "Operation not found")
                } else {
                    NetworkMockEndpointUiState.Content(
                        operationUiModel = OperationUiModel(
                            descriptor = descriptor,
                            currentState = runtimeState.getOperationState(key = operationKey)
                                ?: OperationMockState.Network
                        )
                    )
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = WHILE_SUBSCRIBED_TIMEOUT_MS),
        initialValue = NetworkMockEndpointUiState.Loading
    )

    init {
        loadEndpoint()
    }

    /**
     * Discovers available mock response variants for [operationKey] and assembles the
     * [OperationDescriptor]. Called once on init.
     */
    @Suppress("DocumentationOverPrivateFunction")
    private fun loadEndpoint() {
        viewModelScope.launch {
            privateLoadingState.value = EndpointLoadingState.Loading
            runCatching {
                configRepository.discoverResponseFiles(key = operationKey)
            }.onSuccess { responses ->
                val operation = configRepository
                    .loadConfiguration()
                    .getOrNull()
                    ?.specs
                    ?.firstOrNull { it.id == operationKey.specId }
                    ?.operations
                    ?.firstOrNull { it.operationId == operationKey.operationId }

                if (operation == null) {
                    privateLoadingState.value = EndpointLoadingState.Error(
                        message = "Operation configuration not found"
                    )
                    return@onSuccess
                }

                privateDescriptor.value = OperationDescriptor(
                    key = operationKey,
                    config = operation,
                    availableResponses = responses
                )
                privateLoadingState.value = EndpointLoadingState.Loaded
            }.onFailure { error ->
                privateLoadingState.value = EndpointLoadingState.Error(
                    message = error.message ?: "Failed to load operation"
                )
            }
        }
    }

    /**
     * Sets the mock state for this operation.
     *
     * Passing `null` reverts the operation to [OperationMockState.Network], effectively
     * disabling mocking for it. Passing a non-null [response] transitions it to
     * [OperationMockState.Mock] with that response's `(statusCode, exampleName)`.
     *
     * @param response The response variant to activate, or `null` to use the actual network
     */
    public fun setMockState(response: MockResponse?) {
        viewModelScope.launch {
            val newState = if (response != null) {
                OperationMockState.Mock(
                    statusCode = response.statusCode,
                    exampleName = response.exampleName
                )
            } else {
                OperationMockState.Network
            }
            stateRepository.setOperationMockState(key = operationKey, state = newState)
        }
    }
}

/**
 * UI state for the operation detail screen.
 *
 * Emitted by [NetworkMockEndpointViewModel.uiState].
 */
@Immutable
public sealed interface NetworkMockEndpointUiState {
    /** Response variant discovery is in progress. */
    @Immutable
    public data object Loading : NetworkMockEndpointUiState

    /**
     * Discovery failed or the operation configuration could not be found.
     *
     * @property message Human-readable description of the failure
     */
    @Immutable
    public data class Error(val message: String) : NetworkMockEndpointUiState

    /**
     * Operation loaded successfully.
     *
     * @property operationUiModel The UI model combining the static [OperationDescriptor] with the live
     * [OperationMockState] for the operation, reflecting the latest persisted selection and available
     * mock responses.
     */
    @Immutable
    public data class Content(val operationUiModel: OperationUiModel) : NetworkMockEndpointUiState
}

/**
 * Internal loading state for [NetworkMockEndpointViewModel].
 */
private sealed interface EndpointLoadingState {
    data object Loading : EndpointLoadingState

    data object Loaded : EndpointLoadingState

    data class Error(val message: String) : EndpointLoadingState
}
