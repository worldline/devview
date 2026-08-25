package com.worldline.devview.networkmock.viewmodel

import com.worldline.devview.networkmock.core.model.ApiSpec
import com.worldline.devview.networkmock.core.model.MockConfiguration
import com.worldline.devview.networkmock.core.model.MockResponse
import com.worldline.devview.networkmock.core.model.NetworkMockState
import com.worldline.devview.networkmock.core.model.Operation
import com.worldline.devview.networkmock.core.model.OperationKey
import com.worldline.devview.networkmock.core.model.OperationMockState
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

    private val testKey = OperationKey(specId = "user-api", operationId = "getUser")

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
            operationKey = testKey,
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
            operationKey = testKey,
            configRepository = configRepository,
            stateRepository = stateRepository
        )

        collectState(stateFlow = viewModel.uiState)

        val content = viewModel.uiState.value.shouldBeInstanceOf<NetworkMockEndpointUiState.Content>()
        content.operationUiModel.descriptor.key shouldBe testKey
        content.operationUiModel.descriptor.availableResponses.size shouldBe 1
        content.operationUiModel.currentState shouldBe OperationMockState.Network
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
            operationKey = testKey,
            configRepository = configRepository,
            stateRepository = stateRepository
        )

        collectState(stateFlow = viewModel.uiState)

        val error = viewModel.uiState.value.shouldBeInstanceOf<NetworkMockEndpointUiState.Error>()
        error.message shouldBe "disk error"
    }

    @Test
    fun emitsErrorState_whenEndpointConfigNotFound() = runTest {
        val unknownKey = OperationKey(specId = "unknown-api", operationId = "unknown")
        val stateFlow = MutableStateFlow(value = NetworkMockState())
        val configRepository = createConfigRepositoryMock(
            loadResult = Result.success(value = testConfiguration()),
            operationKey = unknownKey
        )
        val stateRepository = createStateRepositoryMock(stateFlow = stateFlow)

        val viewModel = NetworkMockEndpointViewModel(
            operationKey = unknownKey,
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
            operationKey = testKey,
            configRepository = configRepository,
            stateRepository = stateRepository
        )

        viewModel.setMockState(
            response = MockResponse(statusCode = 200, exampleName = "default", displayName = "Success (200)", content = "{}")
        )

        stateFlow.value.getOperationState(key = testKey)
            .shouldBeInstanceOf<OperationMockState.Mock>()
            .let {
                it.statusCode shouldBe 200
                it.exampleName shouldBe "default"
            }
    }

    @Test
    fun setMockState_withNull_revertsToNetwork() = runTest {
        val stateFlow = MutableStateFlow(
            value = NetworkMockState().withOperationState(
                key = testKey,
                state = OperationMockState.Mock(statusCode = 200, exampleName = "default")
            )
        )
        val configRepository = createConfigRepositoryMock(
            loadResult = Result.success(value = testConfiguration())
        )
        val stateRepository = createStateRepositoryMock(stateFlow = stateFlow)

        val viewModel = NetworkMockEndpointViewModel(
            operationKey = testKey,
            configRepository = configRepository,
            stateRepository = stateRepository
        )

        viewModel.setMockState(response = null)

        stateFlow.value.getOperationState(key = testKey) shouldBe OperationMockState.Network
    }

    private fun createConfigRepositoryMock(
        loadResult: Result<MockConfiguration>,
        loadDelayMs: Long = 0L,
        discoveryException: Exception? = null,
        operationKey: OperationKey = testKey
    ): MockConfigRepository {
        val repository = mockk<MockConfigRepository>()

        coEvery { repository.loadConfiguration() } coAnswers {
            if (loadDelayMs > 0) delay(timeMillis = loadDelayMs)
            loadResult
        }

        if (discoveryException != null) {
            coEvery { repository.discoverResponseFiles(key = any()) } throws discoveryException
        } else {
            coEvery { repository.discoverResponseFiles(key = operationKey) } returns listOf(
                MockResponse(
                    statusCode = 200,
                    exampleName = "default",
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

        coEvery { repository.setOperationMockState(any<OperationKey>(), any()) } coAnswers {
            val key = firstArg<OperationKey>()
            val state = secondArg<OperationMockState>()
            stateFlow.value = stateFlow.value.withOperationState(key = key, state = state)
        }

        every { repository.observeState() } returns stateFlow
        every { repository.registerOperations(any()) } just Runs
        coEvery { repository.getState() } coAnswers { stateFlow.value }

        return repository
    }

    private fun testConfiguration(): MockConfiguration = MockConfiguration(
        specs = listOf(
            ApiSpec(
                id = "user-api",
                name = "User API",
                servers = listOf("https://staging.api.example.com"),
                operations = listOf(
                    Operation(
                        operationId = "getUser",
                        name = "Get User",
                        path = "/api/users/{userId}",
                        method = "GET"
                    )
                )
            )
        )
    )
}
