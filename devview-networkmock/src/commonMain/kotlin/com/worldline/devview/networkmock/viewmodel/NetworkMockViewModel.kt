package com.worldline.devview.networkmock.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worldline.devview.networkmock.core.model.EndpointDescriptor
import com.worldline.devview.networkmock.core.model.EndpointMockState
import com.worldline.devview.networkmock.core.model.MockConfiguration
import com.worldline.devview.networkmock.core.repository.MockConfigRepository
import com.worldline.devview.networkmock.core.repository.MockStateRepository
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
    private val privateEndpointMocks = MutableStateFlow<Map<String, EndpointDescriptor>>(
        value = emptyMap()
    )
    private val selectedEndpointId = MutableStateFlow<Pair<String, String>?>(value = null)

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
                                    name = host.id,
                                    url = host.url,
                                    endpoints = host.endpoints
                                        .mapNotNull { endpoint ->
                                            val key = "${host.id}-${endpoint.id}"
                                            endpointMocks[key]?.let { descriptor ->
                                                EndpointUiModel(
                                                    descriptor = descriptor,
                                                    currentState = runtimeState.getEndpointState(
                                                        hostId = host.id,
                                                        endpointId = endpoint.id
                                                    ) ?: EndpointMockState.Network
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

    /**
     * The [EndpointDescriptor] for the currently selected endpoint, or `null` when no
     * endpoint is selected. Drives bottom sheet visibility — non-null means open.
     */
    public val selectedEndpointDescriptor: StateFlow<EndpointDescriptor?> = combine(
        flow = selectedEndpointId,
        flow2 = privateEndpointMocks
    ) { id, endpointMocks ->
        id?.let { (hostId, endpointId) -> endpointMocks["$hostId-$endpointId"] }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = WHILE_SUBSCRIBED_TIMEOUT_MS),
        initialValue = null
    )

    /**
     * The live [EndpointMockState] for the currently selected endpoint.
     * Updates reactively whenever the user selects a response, keeping the
     * bottom sheet highlight in sync without any UI-side lookup.
     */
    public val selectedEndpointState: StateFlow<EndpointMockState> = combine(
        flow = selectedEndpointId,
        flow2 = stateRepository.observeState()
    ) { id, runtimeState ->
        id?.let { (hostId, endpointId) ->
            runtimeState.getEndpointState(hostId = hostId, endpointId = endpointId)
        } ?: EndpointMockState.Network
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = WHILE_SUBSCRIBED_TIMEOUT_MS),
        initialValue = EndpointMockState.Network
    )

    init {
        loadConfiguration()
    }

    /**
     * Loads the mock configuration from resources and discovers response files.
     */
    @Suppress("DocumentationOverPrivateFunction")
    private fun loadConfiguration() {
        viewModelScope.launch {
            privateLoadingState.value = LoadingState.Loading

            configRepository
                .loadConfiguration()
                .onSuccess { config ->
                    stateRepository.registerEndpoints(
                        endpoints = config.hosts.flatMap { host ->
                            host.endpoints.map { endpoint -> host.id to endpoint.id }
                        }
                    )

                    privateConfiguration.value = config

                    // Discover response files for all endpoints
                    val mocks = mutableMapOf<String, EndpointDescriptor>()
                    config.hosts.forEach { host ->
                        host.endpoints.forEach { endpoint ->
                            val key = "${host.id}-${endpoint.id}"
                            val responses = configRepository.discoverResponseFiles(
                                endpointId = endpoint.id
                            )

                            mocks[key] = EndpointDescriptor(
                                hostId = host.id,
                                endpointId = endpoint.id,
                                config = endpoint,
                                availableResponses = responses
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
     * Sets the mock state for a specific endpoint.
     *
     * When [responseFileName] is `null`, the endpoint state is set to
     * [EndpointMockState.Network], effectively disabling mocking for that endpoint.
     *
     * When [responseFileName] is non-null, the endpoint transitions to
     * [EndpointMockState.Mock] with the given file, replacing any previous state.
     *
     * @param hostId The ID of the host that owns the endpoint.
     * @param endpointId The ID of the endpoint to update.
     * @param responseFileName The response file to use for mocking, or `null` to use
     * the actual network.
     */
    public fun setEndpointMockState(hostId: String, endpointId: String, responseFileName: String?) {
        viewModelScope.launch {
            val newState = if (responseFileName != null) {
                EndpointMockState.Mock(responseFile = responseFileName)
            } else {
                EndpointMockState.Network
            }

            stateRepository.setEndpointMockState(
                hostId = hostId,
                endpointId = endpointId,
                state = newState
            )
        }
    }

    /**
     * Resets all endpoint mocks to use actual network.
     *
     * Builds a [EndpointMockState.Network] state for every endpoint present in
     * the loaded configuration (not just those already stored in DataStore), then
     * persists it in one write. This ensures that endpoints which have never been
     * touched by the user are also explicitly reset, leaving no gaps.
     */
    public fun resetAllToNetwork() {
        viewModelScope.launch {
            val config = privateConfiguration.value
            if (config == null) {
                // Config not loaded yet — fall back to resetting only known stored entries
                stateRepository.resetKnownEndpointsToNetwork()
                return@launch
            }

            // Build a Network state for every configured endpoint
            val allNetwork = config.hosts
                .flatMap { host ->
                    host.endpoints.map { endpoint ->
                        "${host.id}-${endpoint.id}" to EndpointMockState.Network
                    }
                }.toMap()

            stateRepository.setAllEndpointStates(states = allNetwork)
        }
    }

    /**
     * Marks the given endpoint as selected, opening the bottom sheet.
     *
     * Triggers reactive updates to [selectedEndpointDescriptor] and
     * [selectedEndpointState] so the UI requires no manual lookup.
     *
     * @param hostId The ID of the host that owns the endpoint.
     * @param endpointId The ID of the selected endpoint.
     */
    public fun selectEndpoint(hostId: String, endpointId: String) {
        selectedEndpointId.value = hostId to endpointId
    }

    /**
     * Clears the current endpoint selection, closing the bottom sheet.
     */
    public fun clearSelectedEndpoint() {
        selectedEndpointId.value = null
    }
}

/**
 * UI state for the Network Mock screen.
 */
@Immutable
public sealed interface NetworkMockUiState {
    /**
     * Loading configuration.
     */
    @Immutable
    public data object Loading : NetworkMockUiState

    /**
     * Error loading configuration.
     */
    @Immutable
    public data class Error(val message: String) : NetworkMockUiState

    /**
     * No mocks configured.
     */
    @Immutable
    public data object Empty : NetworkMockUiState

    /**
     * Content loaded successfully.
     */
    @Immutable
    public data class Content(
        val globalMockingEnabled: Boolean,
        val hosts: PersistentList<HostUiModel>
    ) : NetworkMockUiState
}

/**
 * UI model pairing a static [EndpointDescriptor] with its live [EndpointMockState].
 *
 * Constructed fresh on every emission of [NetworkMockViewModel.uiState], ensuring
 * that [currentState] always reflects the latest persisted value without any
 * UI-side snapshot or lookup.
 *
 * @property descriptor The immutable endpoint configuration and available responses.
 * @property currentState The current runtime mock state for this endpoint.
 * @see EndpointDescriptor
 * @see EndpointMockState
 */
@Immutable
public data class EndpointUiModel(
    val descriptor: EndpointDescriptor,
    val currentState: EndpointMockState
) {
    public companion object
}

/**
 * UI model for a host with its endpoints.
 */
public data class HostUiModel(
    val id: String,
    val name: String,
    val url: String,
    val endpoints: PersistentList<EndpointUiModel>
) {
    public companion object
}

/**
 * Internal loading state.
 */
private sealed interface LoadingState {
    data object Loading : LoadingState

    data object Loaded : LoadingState

    data class Error(val message: String) : LoadingState
}
