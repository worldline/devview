package com.worldline.devview.networkmock.viewmodel

import com.worldline.devview.networkmock.core.model.EndpointConfig
import com.worldline.devview.networkmock.core.model.EndpointMockState
import com.worldline.devview.networkmock.core.model.HostConfig
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
        content.hosts.shouldHaveSize(2)
        content.hosts.first { it.id == "staging" }.endpoints.shouldHaveSize(2)
        content.hosts.first { it.id == "production" }.endpoints.shouldHaveSize(1)
    }

    @Test
    fun emitsContent_withEmptyHosts_whenConfigHasNoHosts() = runTest {
        val stateFlow = MutableStateFlow(NetworkMockState())
        val configRepository = createConfigRepositoryMock(
            loadResult = Result.success(MockConfiguration(hosts = emptyList()))
        )
        val stateRepository = createStateRepositoryMock(stateFlow)

        val viewModel = NetworkMockViewModel(configRepository, stateRepository)

        collectState(viewModel.uiState)

        val content = viewModel.uiState.value.shouldBeInstanceOf<NetworkMockUiState.Content>()
        content.hosts shouldHaveSize 0
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

        viewModel.selectEndpoint(hostId = "staging", endpointId = "getUser")

        val selected = viewModel.selectedEndpointDescriptor.value
        selected?.hostId shouldBe "staging"
        selected?.endpointId shouldBe "getUser"
    }

    @Test
    fun clearSelectedEndpoint_resetsSelectedEndpointDescriptor_toNull() = runTest {
        val stateFlow = MutableStateFlow(NetworkMockState())
        val configRepository = createConfigRepositoryMock(loadResult = Result.success(testConfiguration()))
        val stateRepository = createStateRepositoryMock(stateFlow)

        val viewModel = NetworkMockViewModel(configRepository, stateRepository)

        viewModel.selectEndpoint(hostId = "staging", endpointId = "getUser")
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

        viewModel.selectEndpoint(hostId = "staging", endpointId = "getUser")
        viewModel.setEndpointMockState(
            hostId = "staging",
            endpointId = "getUser",
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

        viewModel.setEndpointMockState("staging", "getUser", "getUser-200.json")
        stateFlow.value.getEndpointState("staging", "getUser")
            .shouldBeInstanceOf<EndpointMockState.Mock>()
            .responseFile shouldBe "getUser-200.json"

        viewModel.setEndpointMockState("staging", "getUser", null)
        stateFlow.value.getEndpointState("staging", "getUser") shouldBe EndpointMockState.Network
    }

    @Test
    fun resetAllToNetwork_resetsEveryConfiguredEndpoint_toNetwork() = runTest {
        val stateFlow = MutableStateFlow(NetworkMockState())
        val configRepository = createConfigRepositoryMock(loadResult = Result.success(testConfiguration()))
        val stateRepository = createStateRepositoryMock(stateFlow)

        val viewModel = NetworkMockViewModel(configRepository, stateRepository)

        viewModel.setEndpointMockState("staging", "getUser", "getUser-200.json")
        viewModel.setEndpointMockState("production", "getProduct", "getProduct-200.json")

        val statesSlot = slot<Map<String, EndpointMockState>>()
        coEvery { stateRepository.setAllEndpointStates(states = capture(statesSlot)) } coAnswers {
            stateFlow.value = stateFlow.value.copy(endpointStates = statesSlot.captured)
        }

        viewModel.resetAllToNetwork()

        val allNetwork = statesSlot.captured
        allNetwork["staging-getUser"] shouldBe EndpointMockState.Network
        allNetwork["staging-createUser"] shouldBe EndpointMockState.Network
        allNetwork["production-getProduct"] shouldBe EndpointMockState.Network
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

        coEvery { repository.discoverResponseFiles("getUser") } returns listOf(
            MockResponse(200, "getUser-200.json", "Success (200)", "{}")
        )
        coEvery { repository.discoverResponseFiles("createUser") } returns listOf(
            MockResponse(201, "createUser-201.json", "Created (201)", "{}")
        )
        coEvery { repository.discoverResponseFiles("getProduct") } returns listOf(
            MockResponse(200, "getProduct-200.json", "Success (200)", "{}")
        )
        coEvery { repository.discoverResponseFiles(any()) } returns emptyList()

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
        coEvery { repository.setEndpointMockState(any(), any(), any()) } coAnswers {
            val hostId = firstArg<String>()
            val endpointId = secondArg<String>()
            val state = thirdArg<EndpointMockState>()
            stateFlow.value = stateFlow.value.withEndpointState(hostId, endpointId, state)
        }
        coEvery { repository.setAllEndpointStates(any()) } coAnswers {
            stateFlow.value = stateFlow.value.copy(endpointStates = firstArg())
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
        hosts = listOf(
            HostConfig(
                id = "staging",
                url = "https://staging.api.example.com",
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
                )
            ),
            HostConfig(
                id = "production",
                url = "https://api.example.com",
                endpoints = listOf(
                    EndpointConfig(
                        id = "getProduct",
                        name = "Get Product",
                        path = "/api/products/{productId}",
                        method = "GET"
                    )
                )
            )
        )
    )
}


