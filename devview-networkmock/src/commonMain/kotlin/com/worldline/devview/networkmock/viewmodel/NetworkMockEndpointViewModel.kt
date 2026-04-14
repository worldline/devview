package com.worldline.devview.networkmock.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worldline.devview.networkmock.model.EndpointDescriptor
import com.worldline.devview.networkmock.model.EndpointKey
import com.worldline.devview.networkmock.model.EndpointMockState
import com.worldline.devview.networkmock.model.EndpointUiModel
import com.worldline.devview.networkmock.model.effectiveEndpoints
import com.worldline.devview.networkmock.repository.MockConfigRepository
import com.worldline.devview.networkmock.repository.MockStateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val WHILE_SUBSCRIBED_TIMEOUT_MS = 5000L

/**
 * ViewModel for the Network Mock endpoint detail screen.
 *
 * Manages the state and business logic for a single endpoint, combining its
 * discovered mock responses with the live persisted [EndpointMockState] so the
 * UI always reflects the latest selection without any manual lookup.
 *
 * ## Responsibilities
 * - Discover available mock response files for the given [endpointKey]
 * - Observe the live [EndpointMockState] for the endpoint from DataStore
 * - Combine both into a single [uiState] flow
 * - Handle mock state changes triggered by the user
 *
 * @property endpointKey The [EndpointKey] identifying the endpoint this screen represents
 * @property configRepository Repository used to discover available mock response files
 * @property stateRepository Repository used to observe and persist mock state
 */
public class NetworkMockEndpointViewModel(
    private val endpointKey: EndpointKey,
    private val configRepository: MockConfigRepository,
    private val stateRepository: MockStateRepository
) : ViewModel() {
    private val privateDescriptor = MutableStateFlow<EndpointDescriptor?>(value = null)
    private val privateLoadingState =
        MutableStateFlow<EndpointLoadingState>(value = EndpointLoadingState.Loading)

    /**
     * Combined UI state for the endpoint detail screen.
     *
     * Combines the discovered [EndpointDescriptor] (loaded once on init) with the live
     * [EndpointMockState] from DataStore into a single [NetworkMockEndpointUiState]
     * emission. Re-emits whenever either source changes — in practice, [EndpointMockState]
     * changes on every user selection while [EndpointDescriptor] is stable after loading.
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
                    NetworkMockEndpointUiState.Error(message = "Endpoint not found")
                } else {
                    NetworkMockEndpointUiState.Content(
                        endpointUiModel = EndpointUiModel(
                            descriptor = descriptor,
                            currentState = runtimeState.getEndpointState(key = endpointKey)
                                ?: EndpointMockState.Network
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
     * Discovers available mock response files for [endpointKey] and assembles the
     * [EndpointDescriptor]. Called once on init.
     */
    @Suppress("DocumentationOverPrivateFunction")
    private fun loadEndpoint() {
        viewModelScope.launch {
            privateLoadingState.value = EndpointLoadingState.Loading
            runCatching {
                configRepository.discoverResponseFiles(key = endpointKey)
            }.onSuccess { responses ->
                privateDescriptor.value = EndpointDescriptor(
                    key = endpointKey,
                    config = configRepository
                        .loadConfiguration()
                        .getOrNull()
                        ?.apiGroups
                        ?.flatMap { group ->
                            group.environments.flatMap { env ->
                                group.effectiveEndpoints(environment = env).map {
                                    it to
                                        EndpointKey(
                                            groupId = group.id,
                                            environmentId = env.id,
                                            endpointId = it.id
                                        )
                                }
                            }
                        }?.firstOrNull { (_, key) -> key == endpointKey }
                        ?.first
                        ?: run {
                            privateLoadingState.value = EndpointLoadingState.Error(
                                message = "Endpoint configuration not found"
                            )
                            return@onSuccess
                        },
                    availableResponses = responses
                )
                privateLoadingState.value = EndpointLoadingState.Loaded
            }.onFailure { error ->
                privateLoadingState.value = EndpointLoadingState.Error(
                    message = error.message ?: "Failed to load endpoint"
                )
            }
        }
    }

    /**
     * Sets the mock state for this endpoint.
     *
     * Passing `null` reverts the endpoint to [EndpointMockState.Network], effectively
     * disabling mocking for it. Passing a non-null [responseFileName] transitions it
     * to [EndpointMockState.Mock] with the given file.
     *
     * @param responseFileName The response file name to activate, or `null` to use the
     *   actual network
     */
    public fun setMockState(responseFileName: String?) {
        viewModelScope.launch {
            val newState = if (responseFileName != null) {
                EndpointMockState.Mock(responseFile = responseFileName)
            } else {
                EndpointMockState.Network
            }
            stateRepository.setEndpointMockState(key = endpointKey, state = newState)
        }
    }
}

/**
 * UI state for the endpoint detail screen.
 *
 * Emitted by [NetworkMockEndpointViewModel.uiState].
 */
@Immutable
public sealed interface NetworkMockEndpointUiState {
    /** Response file discovery is in progress. */
    @Immutable
    public data object Loading : NetworkMockEndpointUiState

    /**
     * Discovery failed or the endpoint configuration could not be found.
     *
     * @property message Human-readable description of the failure
     */
    @Immutable
    public data class Error(val message: String) : NetworkMockEndpointUiState

    /**
     * Endpoint loaded successfully.
     *
     * @property endpointUiModel The UI model combining the static [EndpointDescriptor] with the live
     * [EndpointMockState] for the endpoint, reflecting the latest persisted selection and available mock responses.
     */
    @Immutable
    public data class Content(val endpointUiModel: EndpointUiModel) : NetworkMockEndpointUiState
}

/**
 * Internal loading state for [NetworkMockEndpointViewModel].
 */
private sealed interface EndpointLoadingState {
    data object Loading : EndpointLoadingState

    data object Loaded : EndpointLoadingState

    data class Error(val message: String) : EndpointLoadingState
}
