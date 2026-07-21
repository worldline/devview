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
import com.worldline.devview.test.ViewModelTest
import com.worldline.devview.test.collectState
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest

class NetworkMockEndpointViewModelTest : ViewModelTest() {

    private val testKey = EndpointKey(
        groupId = "user-api",
        environmentId = "staging",
        endpointId = "getUser"
    )

    @BeforeTest
    override fun setup() {
        super.setup()
    }

    @AfterTest
    override fun tearDown() {
        super.tearDown()
    }

    @Test
    fun initialUiState_isLoading_whileConfigIsStillLoading() = runTest {
        val stateFlow = MutableStateFlow(value = NetworkMockState())
        val configRepository = createConfigRepositoryMock(
            loadResult = Result.success(value = testConfiguration()),
            loadDelayMs = 500
        )
        val stateRepository = createStateRepositoryMock(stateFlow = stateFlow)

        val viewModel = NetworkMockEndpointViewModel(
            endpointKey = testKey,
            configRepository = configRepository,
            stateRepository = stateRepository
        )

        collectState(stateFlow = viewModel.uiState)

        viewModel.uiState.value shouldBe NetworkMockEndpointUiState.Loading
    }

    @Test
    fun emitsContentState_afterSuccessfulLoad() = runTest {
        val stateFlow = MutableStateFlow(value = NetworkMockState())
        val configRepository = createConfigRepositoryMock(
            loadResult = Result.success(value = testConfiguration())
        )
        val stateRepository = createStateRepositoryMock(stateFlow = stateFlow)

        val viewModel = NetworkMockEndpointViewModel(
            endpointKey = testKey,
            configRepository = configRepository,
            stateRepository = stateRepository
        )

        collectState(stateFlow = viewModel.uiState)

        val content = viewModel.uiState.value.shouldBeInstanceOf<NetworkMockEndpointUiState.Content>()
        content.endpointUiModel.descriptor.key shouldBe testKey
        content.endpointUiModel.descriptor.availableResponses.size shouldBe 1
        content.endpointUiModel.currentState shouldBe EndpointMockState.Network
    }

    @Test
    fun emitsErrorState_whenDiscoveryFails() = runTest {
        val stateFlow = MutableStateFlow(value = NetworkMockState())
        val configRepository = createConfigRepositoryMock(
            loadResult = Result.success(value = testConfiguration()),
            discoveryException = RuntimeException("disk error")
        )
        val stateRepository = createStateRepositoryMock(stateFlow = stateFlow)

        val viewModel = NetworkMockEndpointViewModel(
            endpointKey = testKey,
            configRepository = configRepository,
            stateRepository = stateRepository
        )

        collectState(stateFlow = viewModel.uiState)

        val error = viewModel.uiState.value.shouldBeInstanceOf<NetworkMockEndpointUiState.Error>()
        error.message shouldBe "disk error"
    }

    @Test
    fun emitsErrorState_whenEndpointConfigNotFound() = runTest {
        val unknownKey = EndpointKey(
            groupId = "unknown-api",
            environmentId = "staging",
            endpointId = "unknown"
        )
        val stateFlow = MutableStateFlow(value = NetworkMockState())
        val configRepository = createConfigRepositoryMock(
            loadResult = Result.success(value = testConfiguration()),
            endpointKey = unknownKey
        )
        val stateRepository = createStateRepositoryMock(stateFlow = stateFlow)

        val viewModel = NetworkMockEndpointViewModel(
            endpointKey = unknownKey,
            configRepository = configRepository,
            stateRepository = stateRepository
        )

        collectState(stateFlow = viewModel.uiState)

        viewModel.uiState.value.shouldBeInstanceOf<NetworkMockEndpointUiState.Error>()
    }

    @Test
    fun setMockState_persistsMockState() = runTest {
        val stateFlow = MutableStateFlow(value = NetworkMockState())
        val configRepository = createConfigRepositoryMock(
            loadResult = Result.success(value = testConfiguration())
        )
        val stateRepository = createStateRepositoryMock(stateFlow = stateFlow)

        val viewModel = NetworkMockEndpointViewModel(
            endpointKey = testKey,
            configRepository = configRepository,
            stateRepository = stateRepository
        )

        viewModel.setMockState(responseFileName = "getUser-200.json")

        stateFlow.value.getEndpointState(key = testKey)
            .shouldBeInstanceOf<EndpointMockState.Mock>()
            .responseFile shouldBe "getUser-200.json"
    }

    @Test
    fun setMockState_withNull_revertsToNetwork() = runTest {
        val stateFlow = MutableStateFlow(
            value = NetworkMockState().withEndpointState(
                key = testKey,
                state = EndpointMockState.Mock(responseFile = "getUser-200.json")
            )
        )
        val configRepository = createConfigRepositoryMock(
            loadResult = Result.success(value = testConfiguration())
        )
        val stateRepository = createStateRepositoryMock(stateFlow = stateFlow)

        val viewModel = NetworkMockEndpointViewModel(
            endpointKey = testKey,
            configRepository = configRepository,
            stateRepository = stateRepository
        )

        viewModel.setMockState(responseFileName = null)

        stateFlow.value.getEndpointState(key = testKey) shouldBe EndpointMockState.Network
    }

    private fun createConfigRepositoryMock(
        loadResult: Result<MockConfiguration>,
        loadDelayMs: Long = 0L,
        discoveryException: Exception? = null,
        endpointKey: EndpointKey = testKey
    ): MockConfigRepository {
        val repository = mockk<MockConfigRepository>()

        coEvery { repository.loadConfiguration() } coAnswers {
            if (loadDelayMs > 0) delay(timeMillis = loadDelayMs)
            loadResult
        }

        if (discoveryException != null) {
            coEvery { repository.discoverResponseFiles(key = any()) } throws discoveryException
        } else {
            coEvery { repository.discoverResponseFiles(key = endpointKey) } returns listOf(
                MockResponse(
                    statusCode = 200,
                    fileName = "getUser-200.json",
                    displayName = "Success (200)",
                    content = "{}"
                )
            )
        }

        return repository
    }

    private fun createStateRepositoryMock(
        stateFlow: MutableStateFlow<NetworkMockState>
    ): MockStateRepository {
        val repository = mockk<MockStateRepository>()

        coEvery { repository.setEndpointMockState(any<EndpointKey>(), any()) } coAnswers {
            val key = firstArg<EndpointKey>()
            val state = secondArg<EndpointMockState>()
            stateFlow.value = stateFlow.value.withEndpointState(key = key, state = state)
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
                    )
                ),
                environments = listOf(
                    EnvironmentConfig(
                        id = "staging",
                        name = "Staging",
                        url = "https://staging.api.example.com"
                    )
                )
            )
        )
    )
}
