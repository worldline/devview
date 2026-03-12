package com.worldline.devview.networkmock.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worldline.devview.networkmock.model.EndpointDescriptor
import com.worldline.devview.networkmock.model.EndpointKey
import com.worldline.devview.networkmock.model.EndpointMockState
import com.worldline.devview.networkmock.model.MockConfiguration
import com.worldline.devview.networkmock.model.effectiveEndpoints
import com.worldline.devview.networkmock.repository.MockConfigRepository
import com.worldline.devview.networkmock.repository.MockStateRepository
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
    private val privateEndpointMocks = MutableStateFlow<Map<EndpointKey, EndpointDescriptor>>(
        value = emptyMap()
    )

    /**
     * The [EndpointKey] of the currently selected endpoint, or `null` when nothing is selected.
     * Drives bottom sheet visibility — non-null means open.
     */
    @Suppress("DocumentationOverPrivateProperty")
    private val selectedEndpointKey = MutableStateFlow<EndpointKey?>(value = null)

    /**
     * Combined UI state for the Network Mock screen.
     *
     * Combines [MockConfiguration] (loaded once from `mocks.json`), the live
     * [com.worldline.devview.networkmock.model.NetworkMockState] from DataStore, the internal
     * loading state, and the discovered [EndpointDescriptor] map into a single
     * [NetworkMockUiState] emission. Re-emits whenever any of the four sources change.
     *
     * Each API group + environment pair in the configuration becomes one
     * [GroupEnvironmentUiModel] tab. Within each tab, only endpoints whose
     * [EndpointDescriptor] has already been discovered are included.
     *
     * @see NetworkMockUiState
     * @see GroupEnvironmentUiModel
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
                        groups = config.apiGroups
                            .flatMap { group ->
                                group.environments.map { environment ->
                                    val effectiveEndpoints = group.effectiveEndpoints(
                                        environment = environment
                                    )
                                    GroupEnvironmentUiModel(
                                        groupId = group.id,
                                        environmentId = environment.id,
                                        name = "${group.name} — ${environment.name}",
                                        url = environment.url,
                                        endpoints = effectiveEndpoints
                                            .mapNotNull { endpoint ->
                                                val key = EndpointKey(
                                                    groupId = group.id,
                                                    environmentId = environment.id,
                                                    endpointId = endpoint.id
                                                )
                                                endpointMocks[key]?.let { descriptor ->
                                                    EndpointUiModel(
                                                        descriptor = descriptor,
                                                        currentState = runtimeState
                                                            .getEndpointState(key = key)
                                                            ?: EndpointMockState.Network
                                                    )
                                                }
                                            }.toPersistentList()
                                    )
                                }
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
        flow = selectedEndpointKey,
        flow2 = privateEndpointMocks
    ) { key, endpointMocks ->
        key?.let { endpointMocks[it] }
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
        flow = selectedEndpointKey,
        flow2 = stateRepository.observeState()
    ) { key, runtimeState ->
        key?.let { runtimeState.getEndpointState(key = it) } ?: EndpointMockState.Network
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = WHILE_SUBSCRIBED_TIMEOUT_MS),
        initialValue = EndpointMockState.Network
    )

    init {
        loadConfiguration()
    }

    /**
     * Loads the mock configuration from resources and discovers response files for every
     * group + environment + endpoint combination.
     */
    @Suppress("DocumentationOverPrivateFunction")
    private fun loadConfiguration() {
        viewModelScope.launch {
            privateLoadingState.value = LoadingState.Loading

            configRepository
                .loadConfiguration()
                .onSuccess { config ->
                    // Pre-register every EndpointKey so write-side helpers have the full set
                    val allKeys = config.apiGroups.flatMap { group ->
                        group.environments.flatMap { environment ->
                            group.effectiveEndpoints(environment = environment).map { endpoint ->
                                EndpointKey(
                                    groupId = group.id,
                                    environmentId = environment.id,
                                    endpointId = endpoint.id
                                )
                            }
                        }
                    }
                    stateRepository.registerEndpoints(endpoints = allKeys)

                    privateConfiguration.value = config

                    // Discover response files for every group + environment + endpoint
                    val mocks = mutableMapOf<EndpointKey, EndpointDescriptor>()
                    config.apiGroups.forEach { group ->
                        group.environments.forEach { environment ->
                            group
                                .effectiveEndpoints(environment = environment)
                                .forEach { endpoint ->
                                    val key = EndpointKey(
                                        groupId = group.id,
                                        environmentId = environment.id,
                                        endpointId = endpoint.id
                                    )
                                    val responses = configRepository.discoverResponseFiles(
                                        key = key
                                    )
                                    mocks[key] = EndpointDescriptor(
                                        key = key,
                                        config = endpoint,
                                        availableResponses = responses
                                    )
                                }
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
     *
     * When disabled, every HTTP request passes through to the actual network
     * regardless of individual endpoint configurations. Persisted immediately
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
     * Sets the mock state for a specific endpoint identified by an [EndpointKey].
     *
     * When [responseFileName] is `null`, the endpoint state is set to
     * [EndpointMockState.Network], effectively disabling mocking for that endpoint.
     *
     * When [responseFileName] is non-null, the endpoint transitions to
     * [EndpointMockState.Mock] with the given file, replacing any previous state.
     *
     * @param key The [EndpointKey] identifying the group, environment, and endpoint
     * @param responseFileName The response file to use for mocking, or `null` to use
     *   the actual network
     */
    public fun setEndpointMockState(key: EndpointKey, responseFileName: String?) {
        viewModelScope.launch {
            val newState = if (responseFileName != null) {
                EndpointMockState.Mock(responseFile = responseFileName)
            } else {
                EndpointMockState.Network
            }
            stateRepository.setEndpointMockState(key = key, state = newState)
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

            // Build a Network state for every configured group + environment + endpoint
            val allNetwork = config.apiGroups
                .flatMap { group ->
                    group.environments.flatMap { environment ->
                        group.effectiveEndpoints(environment = environment).map { endpoint ->
                            EndpointKey(
                                groupId = group.id,
                                environmentId = environment.id,
                                endpointId = endpoint.id
                            ) to EndpointMockState.Network
                        }
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
     * @param key The [EndpointKey] identifying the group, environment, and endpoint
     */
    public fun selectEndpoint(key: EndpointKey) {
        selectedEndpointKey.value = key
    }

    /**
     * Clears the current endpoint selection, closing the bottom sheet.
     */
    public fun clearSelectedEndpoint() {
        selectedEndpointKey.value = null
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

    /** Configuration loaded successfully but contains no API groups. */
    @Immutable
    public data object Empty : NetworkMockUiState

    /**
     * Configuration loaded successfully and at least one group is available.
     *
     * @property globalMockingEnabled Whether the global mocking master switch is on
     * @property groups One entry per API group + environment combination, each
     *   rendered as a tab in the UI
     */
    @Immutable
    public data class Content(
        val globalMockingEnabled: Boolean,
        val groups: PersistentList<GroupEnvironmentUiModel>
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
 * UI model for a single API group + environment combination.
 *
 * Each tab in the Network Mock screen represents one [GroupEnvironmentUiModel],
 * showing the resolved endpoints for that specific group and environment.
 *
 * @property groupId The [com.worldline.devview.networkmock.model.ApiGroupConfig] identifier
 * @property environmentId The [com.worldline.devview.networkmock.model.EnvironmentConfig] identifier
 * @property name Human-readable display name, e.g. `"My Backend — Staging"`
 * @property url The base URL for this group in this environment
 * @property endpoints The resolved endpoints with their current mock states
 */
@Immutable
public data class GroupEnvironmentUiModel(
    val groupId: String,
    val environmentId: String,
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
