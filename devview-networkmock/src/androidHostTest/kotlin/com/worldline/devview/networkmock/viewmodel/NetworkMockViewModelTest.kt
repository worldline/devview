package com.worldline.devview.networkmock.viewmodel

import com.worldline.devview.networkmock.core.model.ApiGroupConfig
import com.worldline.devview.networkmock.core.model.EndpointConfig
import com.worldline.devview.networkmock.core.model.EndpointKey
import com.worldline.devview.networkmock.core.model.EndpointMockState
import com.worldline.devview.networkmock.core.model.EnvironmentConfig
import com.worldline.devview.networkmock.core.model.MockConfiguration
import com.worldline.devview.networkmock.core.model.MockResponse
import com.worldline.devview.networkmock.core.model.NetworkMockState
import com.worldline.devview.networkmock.core.repository.MockConfigRepository
import com.worldline.devview.networkmock.core.repository.MockStateRepository
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest

class NetworkMockViewModelTest : ViewModelTest() {

    @BeforeTest
    override fun setup() {
        super.setup()
    }

    @Test
    fun initialUiState_isLoading_whileConfigIsStillLoading() = runTest {
        val stateFlow = MutableStateFlow(NetworkMockState())
        val configRepository = createConfigRepositoryMock(
            loadResult = Result.success(testConfiguration()),
            loadDelayMs = 500
        )
        val stateRepository = createStateRepositoryMock(stateFlow)

        val viewModel = NetworkMockViewModel(configRepository, stateRepository)

        collectState(viewModel.uiState)

        viewModel.uiState.value shouldBe NetworkMockUiState.Loading
    }

    @Test
    fun emitsContentState_afterSuccessfulConfigurationLoad() = runTest {
        val stateFlow = MutableStateFlow(NetworkMockState())
        val configRepository = createConfigRepositoryMock(loadResult = Result.success(testConfiguration()))
        val stateRepository = createStateRepositoryMock(stateFlow)

        val viewModel = NetworkMockViewModel(configRepository, stateRepository)

        collectState(viewModel.uiState)

        val content = viewModel.uiState.value.shouldBeInstanceOf<NetworkMockUiState.Content>()
        content.groups.shouldHaveSize(2)
        content.groups.first { it.groupId == "user-api" && it.environmentId == "staging" }
            .endpoints.shouldHaveSize(2)
        content.groups.first { it.groupId == "catalog-api" && it.environmentId == "production" }
            .endpoints.shouldHaveSize(1)
    }

    @Test
    fun emitsContent_withEmptyGroups_whenConfigHasNoGroups() = runTest {
        val stateFlow = MutableStateFlow(NetworkMockState())
        val configRepository = createConfigRepositoryMock(
            loadResult = Result.success(MockConfiguration(apiGroups = emptyList()))
        )
        val stateRepository = createStateRepositoryMock(stateFlow)

        val viewModel = NetworkMockViewModel(configRepository, stateRepository)

        collectState(viewModel.uiState)

        viewModel.uiState.value
            .shouldBeInstanceOf<NetworkMockUiState.Content>()
            .groups shouldHaveSize 0
    }

    @Test
    fun emitsErrorState_whenConfigurationLoadFails() = runTest {
        val stateFlow = MutableStateFlow(NetworkMockState())
        val configRepository = createConfigRepositoryMock(
            loadResult = Result.failure(IllegalStateException("config missing"))
        )
        val stateRepository = createStateRepositoryMock(stateFlow)

        val viewModel = NetworkMockViewModel(configRepository, stateRepository)

        collectState(viewModel.uiState)

        val error = viewModel.uiState.value.shouldBeInstanceOf<NetworkMockUiState.Error>()
        error.message shouldBe "config missing"
    }

    @Test
    fun content_reflectsGlobalMockingEnabledState() = runTest {
        val stateFlow = MutableStateFlow(NetworkMockState(globalMockingEnabled = true))
        val configRepository = createConfigRepositoryMock(loadResult = Result.success(testConfiguration()))
        val stateRepository = createStateRepositoryMock(stateFlow)

        val viewModel = NetworkMockViewModel(configRepository, stateRepository)

        collectState(viewModel.uiState)

        viewModel.uiState.value
            .shouldBeInstanceOf<NetworkMockUiState.Content>()
            .globalMockingEnabled shouldBe true
    }

    @Test
    fun selectEndpoint_updatesSelectedEndpointDescriptor() = runTest {
        val stateFlow = MutableStateFlow(NetworkMockState())
        val configRepository = createConfigRepositoryMock(loadResult = Result.success(testConfiguration()))
        val stateRepository = createStateRepositoryMock(stateFlow)

        val viewModel = NetworkMockViewModel(configRepository, stateRepository)

        collectStates(viewModel.uiState, viewModel.selectedEndpointDescriptor)

        viewModel.selectEndpoint(
            key = EndpointKey(
                groupId = "user-api",
                environmentId = "staging",
                endpointId = "getUser"
            )
        )

        val selected = viewModel.selectedEndpointDescriptor.value
        selected?.groupId shouldBe "user-api"
        selected?.environmentId shouldBe "staging"
        selected?.endpointId shouldBe "getUser"
    }

    @Test
    fun clearSelectedEndpoint_resetsSelectedEndpointDescriptor_toNull() = runTest {
        val stateFlow = MutableStateFlow(NetworkMockState())
        val configRepository = createConfigRepositoryMock(loadResult = Result.success(testConfiguration()))
        val stateRepository = createStateRepositoryMock(stateFlow)

        val viewModel = NetworkMockViewModel(configRepository, stateRepository)

        viewModel.selectEndpoint(
            key = EndpointKey(
                groupId = "user-api",
                environmentId = "staging",
                endpointId = "getUser"
            )
        )
        viewModel.clearSelectedEndpoint()

        viewModel.selectedEndpointDescriptor.value shouldBe null
    }

    @Test
    fun selectedEndpointState_reflectsSelectedEndpointMockState() = runTest {
        val stateFlow = MutableStateFlow(NetworkMockState())
        val configRepository = createConfigRepositoryMock(loadResult = Result.success(testConfiguration()))
        val stateRepository = createStateRepositoryMock(stateFlow)

        val viewModel = NetworkMockViewModel(configRepository, stateRepository)

        collectStates(viewModel.uiState, viewModel.selectedEndpointState)

        val key = EndpointKey(groupId = "user-api", environmentId = "staging", endpointId = "getUser")
        viewModel.selectEndpoint(key = key)
        viewModel.setEndpointMockState(
            key = key,
            responseFileName = "getUser-200.json"
        )

        val selected = viewModel.selectedEndpointState.value.shouldBeInstanceOf<EndpointMockState.Mock>()
        selected.responseFile shouldBe "getUser-200.json"
    }

    @Test
    fun setGlobalMockingEnabled_persistsInRepository() = runTest {
        val stateFlow = MutableStateFlow(NetworkMockState())
        val configRepository = createConfigRepositoryMock(loadResult = Result.success(testConfiguration()))
        val stateRepository = createStateRepositoryMock(stateFlow)

        val viewModel = NetworkMockViewModel(configRepository, stateRepository)

        viewModel.setGlobalMockingEnabled(enabled = true)

        stateFlow.value.globalMockingEnabled shouldBe true
        coVerify(exactly = 1) { stateRepository.setGlobalMockingEnabled(enabled = true) }
    }

    @Test
    fun setEndpointMockState_persistsMockAndNetworkTransitions() = runTest {
        val stateFlow = MutableStateFlow(NetworkMockState())
        val configRepository = createConfigRepositoryMock(loadResult = Result.success(testConfiguration()))
        val stateRepository = createStateRepositoryMock(stateFlow)

        val viewModel = NetworkMockViewModel(configRepository, stateRepository)

        val key = EndpointKey(groupId = "user-api", environmentId = "staging", endpointId = "getUser")

        viewModel.setEndpointMockState(key, "getUser-200.json")
        stateFlow.value.getEndpointState(key)
            .shouldBeInstanceOf<EndpointMockState.Mock>()
            .responseFile shouldBe "getUser-200.json"

        viewModel.setEndpointMockState(key, null)
        stateFlow.value.getEndpointState(key) shouldBe EndpointMockState.Network
    }

    @Test
    fun resetAllToNetwork_resetsEveryConfiguredEndpoint_toNetwork() = runTest {
        val stateFlow = MutableStateFlow(NetworkMockState())
        val configRepository = createConfigRepositoryMock(loadResult = Result.success(testConfiguration()))
        val stateRepository = createStateRepositoryMock(stateFlow)

        val viewModel = NetworkMockViewModel(configRepository, stateRepository)

        viewModel.setEndpointMockState(
            key = EndpointKey(groupId = "user-api", environmentId = "staging", endpointId = "getUser"),
            responseFileName = "getUser-200.json"
        )
        viewModel.setEndpointMockState(
            key = EndpointKey(groupId = "catalog-api", environmentId = "production", endpointId = "getProduct"),
            responseFileName = "getProduct-200.json"
        )

        val statesSlot = slot<Map<EndpointKey, EndpointMockState>>()
        coEvery { stateRepository.setAllEndpointStates(states = capture(statesSlot)) } coAnswers {
            stateFlow.value = stateFlow.value.copy(
                endpointStates = statesSlot.captured.mapKeys { (key, _) -> key.compositeKey }
            )
        }

        viewModel.resetAllToNetwork()

        val allNetwork = statesSlot.captured
        allNetwork[EndpointKey("user-api", "staging", "getUser")] shouldBe EndpointMockState.Network
        allNetwork[EndpointKey("user-api", "staging", "createUser")] shouldBe EndpointMockState.Network
        allNetwork[EndpointKey("catalog-api", "production", "getProduct")] shouldBe EndpointMockState.Network
    }

    private fun createConfigRepositoryMock(
        loadResult: Result<MockConfiguration>,
        loadDelayMs: Long = 0L
    ): MockConfigRepository {
        val repository = mockk<MockConfigRepository>()

        coEvery { repository.loadConfiguration() } coAnswers {
            if (loadDelayMs > 0) {
                delay(loadDelayMs)
            }
            loadResult
        }

        coEvery { repository.discoverResponseFiles(any<EndpointKey>()) } coAnswers {
            when (firstArg<EndpointKey>().endpointId) {
                "getUser" -> listOf(MockResponse(200, "getUser-200.json", "Success (200)", "{}"))
                "createUser" -> listOf(MockResponse(201, "createUser-201.json", "Created (201)", "{}"))
                "getProduct" -> listOf(MockResponse(200, "getProduct-200.json", "Success (200)", "{}"))
                else -> emptyList()
            }
        }

        return repository
    }

    private fun createStateRepositoryMock(
        stateFlow: MutableStateFlow<NetworkMockState>
    ): MockStateRepository {
        val repository = mockk<MockStateRepository>()

        coEvery { repository.setGlobalMockingEnabled(any()) } coAnswers {
            val enabled = firstArg<Boolean>()
            stateFlow.value = stateFlow.value.copy(globalMockingEnabled = enabled)
        }
        coEvery { repository.setEndpointMockState(any<EndpointKey>(), any()) } coAnswers {
            val key = firstArg<EndpointKey>()
            val state = secondArg<EndpointMockState>()
            stateFlow.value = stateFlow.value.withEndpointState(key, state)
        }
        coEvery { repository.setAllEndpointStates(any()) } coAnswers {
            val states = firstArg<Map<EndpointKey, EndpointMockState>>()
            stateFlow.value = stateFlow.value.copy(
                endpointStates = states.mapKeys { (key, _) -> key.compositeKey }
            )
        }
        coEvery { repository.resetKnownEndpointsToNetwork() } coAnswers {
            stateFlow.value = stateFlow.value.resetAllToNetwork()
        }

        every { repository.observeState() } returns stateFlow
        every { repository.registerEndpoints(any()) } just Runs

        coEvery { repository.getState() } coAnswers { stateFlow.value }

        return repository
    }

    private fun testConfiguration(): MockConfiguration = MockConfiguration(
        apiGroups = listOf(
            ApiGroupConfig(
                id = "user-api",
                name = "User API",
                endpoints = listOf(
                    EndpointConfig(
                        id = "getUser",
                        name = "Get User",
                        path = "/api/users/{userId}",
                        method = "GET"
                    ),
                    EndpointConfig(
                        id = "createUser",
                        name = "Create User",
                        path = "/api/users",
                        method = "POST"
                    )
                ),
                environments = listOf(
                    EnvironmentConfig(
                        id = "staging",
                        name = "Staging",
                        url = "https://staging.api.example.com"
                    )
                )
            ),
            ApiGroupConfig(
                id = "catalog-api",
                name = "Catalog API",
                endpoints = listOf(
                    EndpointConfig(
                        id = "getProduct",
                        name = "Get Product",
                        path = "/api/products/{productId}",
                        method = "GET"
                    )
                ),
                environments = listOf(
                    EnvironmentConfig(
                        id = "production",
                        name = "Production",
                        url = "https://api.example.com"
                    )
                )
            )
        )
    )
}


