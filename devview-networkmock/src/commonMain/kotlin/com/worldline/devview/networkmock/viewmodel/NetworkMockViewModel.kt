package com.worldline.devview.networkmock.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worldline.devview.networkmock.model.AvailableEndpointMock
import com.worldline.devview.networkmock.model.EndpointMockState
import com.worldline.devview.networkmock.model.MockConfiguration
import com.worldline.devview.networkmock.repository.MockConfigRepository
import com.worldline.devview.networkmock.repository.MockStateRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
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
 * combining data from configuration files and persisted state to provide a
 * complete view of available mocks and their current settings.
 *
 * ## Responsibilities
 * - Load mock configuration from resources
 * - Observe persisted mock state from DataStore
 * - Combine config and state into UI-friendly models
 * - Handle user actions (toggle mocking, select responses)
 * - Manage loading and error states
 *
 * ## State Flow
 * The ViewModel exposes a single [uiState] flow that combines:
 * - Mock configuration (from JSON file)
 * - Runtime state (from DataStore)
 * - Discovered response files (from resources)
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
    private val privateEndpointMocks = MutableStateFlow<Map<String, AvailableEndpointMock>>(
        value = emptyMap()
    )

    /**
     * Combined UI state for the Network Mock screen.
     *
     * This flow combines configuration, runtime state, and discovered responses
     * to provide everything the UI needs to render.
     */
    public val uiState: StateFlow<NetworkMockUiState> = combine(
        flow = privateConfiguration,
        flow2 = stateRepository.observeState(),
        flow3 = privateLoadingState,
        flow4 = privateEndpointMocks
    ) { config, runtimeState, loadingState, endpointMocks ->
        when (loadingState) {
            is LoadingState.Loading -> NetworkMockUiState.Loading
            is LoadingState.Error -> NetworkMockUiState.Error(message = loadingState.message)
            is LoadingState.Loaded -> {
                if (config == null) {
                    NetworkMockUiState.Empty
                } else {
                    NetworkMockUiState.Content(
                        globalMockingEnabled = runtimeState.globalMockingEnabled,
                        hosts = config.hosts
                            .map { host ->
                                HostUiModel(
                                    id = host.id,
                                    name = host.id, // Use ID as name for now
                                    url = host.url,
                                    endpoints = host.endpoints
                                        .mapNotNull { endpoint ->
                                            val key = "${host.id}-${endpoint.id}"
                                            endpointMocks[key]?.copy(
                                                currentState = runtimeState.getEndpointState(
                                                    hostId = host.id,
                                                    endpointId = endpoint.id
                                                )
                                                    ?: EndpointMockState()
                                            )
                                        }.toImmutableList()
                                )
                            }.toImmutableList()
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
     * Loads the mock configuration from resources and discovers response files.
     */
    @Suppress("CommentOverPrivateFunction")
    private fun loadConfiguration() {
        viewModelScope.launch {
            privateLoadingState.value = LoadingState.Loading

            configRepository
                .loadConfiguration()
                .onSuccess { config ->
                    privateConfiguration.value = config

                    // Discover response files for all endpoints
                    val mocks = mutableMapOf<String, AvailableEndpointMock>()
                    config.hosts.forEach { host ->
                        host.endpoints.forEach { endpoint ->
                            val key = "${host.id}-${endpoint.id}"
                            val responses = configRepository.discoverResponseFiles(
                                endpointId = endpoint.id
                            )

                            mocks[key] = AvailableEndpointMock(
                                hostId = host.id,
                                endpointId = endpoint.id,
                                config = endpoint,
                                availableResponses = responses,
                                currentState = EndpointMockState() // Will be updated from runtime state
                            )
                        }
                    }

                    privateEndpointMocks.value = mocks
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
     */
    public fun setGlobalMockingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            stateRepository.setGlobalMockingEnabled(enabled = enabled)
        }
    }

    /**
     * Toggles mocking for a specific endpoint.
     */
    public fun setEndpointMockEnabled(hostId: String, endpointId: String, enabled: Boolean) {
        viewModelScope.launch {
            val currentState = stateRepository.getState()
            val endpointState = currentState.getEndpointState(
                hostId = hostId,
                endpointId = endpointId
            )
                ?: EndpointMockState()

            stateRepository.setEndpointMockState(
                hostId = hostId,
                endpointId = endpointId,
                state = endpointState.copy(mockEnabled = enabled)
            )
        }
    }

    /**
     * Selects which response file to use for an endpoint.
     */
    public fun selectResponse(hostId: String, endpointId: String, responseFileName: String) {
        viewModelScope.launch {
            val currentState = stateRepository.getState()
            val endpointState = currentState.getEndpointState(
                hostId = hostId,
                endpointId = endpointId
            )
                ?: EndpointMockState()

            stateRepository.setEndpointMockState(
                hostId = hostId,
                endpointId = endpointId,
                state = endpointState.copy(
                    mockEnabled = true, // Auto-enable when selecting a response
                    selectedResponseFile = responseFileName
                )
            )
        }
    }

    /**
     * Resets all endpoint mocks to use actual network.
     *
     * Builds a fully-disabled state for every endpoint present in the loaded configuration
     * (not just those already stored in DataStore), then persists it in one write. This
     * ensures that endpoints which have never been touched by the user are also explicitly
     * reset, leaving no gaps.
     */
    public fun resetAllToNetwork() {
        viewModelScope.launch {
            val config = privateConfiguration.value
            if (config == null) {
                // Config not loaded yet — fall back to resetting only known stored entries
                stateRepository.resetKnownEndpointsToNetwork()
                return@launch
            }

            // Build a disabled state for every configured endpoint
            val allDisabled = config.hosts
                .flatMap { host ->
                    host.endpoints.map { endpoint ->
                        "${host.id}-${endpoint.id}" to EndpointMockState()
                    }
                }
                .toMap()

            stateRepository.setAllEndpointStates(states = allDisabled)
        }
    }
}

/**
 * UI state for the Network Mock screen.
 */
public sealed interface NetworkMockUiState {
    /**
     * Loading configuration.
     */
    public data object Loading : NetworkMockUiState

    /**
     * Error loading configuration.
     */
    public data class Error(val message: String) : NetworkMockUiState

    /**
     * No mocks configured.
     */
    public data object Empty : NetworkMockUiState

    /**
     * Content loaded successfully.
     */
    public data class Content(
        val globalMockingEnabled: Boolean,
        val hosts: ImmutableList<HostUiModel>
    ) : NetworkMockUiState
}

/**
 * UI model for a host with its endpoints.
 */
public data class HostUiModel(
    val id: String,
    val name: String,
    val url: String,
    val endpoints: ImmutableList<AvailableEndpointMock>
)

/**
 * Internal loading state.
 */
private sealed interface LoadingState {
    data object Loading : LoadingState

    data object Loaded : LoadingState

    data class Error(val message: String) : LoadingState
}
