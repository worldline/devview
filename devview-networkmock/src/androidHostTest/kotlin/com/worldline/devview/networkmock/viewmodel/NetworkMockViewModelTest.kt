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
import com.worldline.devview.test.collectStates
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
import kotlin.test.AfterTest
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

    @AfterTest
    override fun tearDown() {
        super.tearDown()
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
        val configRepository =
            createConfigRepositoryMock(loadResult = Result.success(testConfiguration()))
        val stateRepository = createStateRepositoryMock(stateFlow)

        val viewModel = NetworkMockViewModel(configRepository, stateRepository)

        collectState(viewModel.uiState)

        val content = viewModel.uiState.value.shouldBeInstanceOf<NetworkMockUiState.Content>()
        content.specs.shouldHaveSize(2)
        content.specs.first { it.specId == "user-api" }
            .operations.shouldHaveSize(2)
        content.specs.first { it.specId == "catalog-api" }
            .operations.shouldHaveSize(1)
    }

    @Test
    fun emitsContent_withEmptySpecs_whenConfigHasNoSpecs() = runTest {
        val stateFlow = MutableStateFlow(NetworkMockState())
        val configRepository = createConfigRepositoryMock(
            loadResult = Result.success(MockConfiguration(specs = emptyList()))
        )
        val stateRepository = createStateRepositoryMock(stateFlow)

        val viewModel = NetworkMockViewModel(configRepository, stateRepository)

        collectState(viewModel.uiState)

        viewModel.uiState.value
            .shouldBeInstanceOf<NetworkMockUiState.Content>()
            .specs shouldHaveSize 0
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
        val configRepository =
            createConfigRepositoryMock(loadResult = Result.success(testConfiguration()))
        val stateRepository = createStateRepositoryMock(stateFlow)

        val viewModel = NetworkMockViewModel(configRepository, stateRepository)

        collectState(viewModel.uiState)

        viewModel.uiState.value
            .shouldBeInstanceOf<NetworkMockUiState.Content>()
            .globalMockingEnabled shouldBe true
    }

    @Test
    fun setGlobalMockingEnabled_persistsInRepository() = runTest {
        val stateFlow = MutableStateFlow(NetworkMockState())
        val configRepository =
            createConfigRepositoryMock(loadResult = Result.success(testConfiguration()))
        val stateRepository = createStateRepositoryMock(stateFlow)

        val viewModel = NetworkMockViewModel(configRepository, stateRepository)

        viewModel.setGlobalMockingEnabled(enabled = true)

        stateFlow.value.globalMockingEnabled shouldBe true
        coVerify(exactly = 1) { stateRepository.setGlobalMockingEnabled(enabled = true) }
    }

    @Test
    fun setOperationMockState_persistsMockAndNetworkTransitions() = runTest {
        val stateFlow = MutableStateFlow(NetworkMockState())
        val configRepository =
            createConfigRepositoryMock(loadResult = Result.success(testConfiguration()))
        val stateRepository = createStateRepositoryMock(stateFlow)

        val viewModel = NetworkMockViewModel(configRepository, stateRepository)

        val key = OperationKey(specId = "user-api", operationId = "getUser")
        val response = MockResponse(statusCode = 200, exampleName = "default", displayName = "Success (200)", content = "{}")

        viewModel.setOperationMockState(key, response)
        stateFlow.value.getOperationState(key)
            .shouldBeInstanceOf<OperationMockState.Mock>()
            .let {
                it.statusCode shouldBe 200
                it.exampleName shouldBe "default"
            }

        viewModel.setOperationMockState(key, null)
        stateFlow.value.getOperationState(key) shouldBe OperationMockState.Network
    }

    @Test
    fun resetAllToNetwork_resetsEveryConfiguredEndpoint_toNetwork() = runTest {
        val stateFlow = MutableStateFlow(NetworkMockState())
        val configRepository =
            createConfigRepositoryMock(loadResult = Result.success(testConfiguration()))
        val stateRepository = createStateRepositoryMock(stateFlow)

        val viewModel = NetworkMockViewModel(configRepository, stateRepository)

        viewModel.setOperationMockState(
            key = OperationKey(specId = "user-api", operationId = "getUser"),
            response = MockResponse(statusCode = 200, exampleName = "default", displayName = "Success (200)", content = "{}")
        )
        viewModel.setOperationMockState(
            key = OperationKey(specId = "catalog-api", operationId = "getProduct"),
            response = MockResponse(statusCode = 200, exampleName = "default", displayName = "Success (200)", content = "{}")
        )

        val statesSlot = slot<Map<OperationKey, OperationMockState>>()
        coEvery { stateRepository.setAllOperationStates(states = capture(statesSlot)) } coAnswers {
            stateFlow.value = stateFlow.value.copy(
                operationStates = statesSlot.captured.mapKeys { (key, _) -> key.compositeKey }
            )
        }

        viewModel.resetAllToNetwork()

        val allNetwork = statesSlot.captured
        allNetwork[OperationKey("user-api", "getUser")] shouldBe OperationMockState.Network
        allNetwork[OperationKey("user-api", "createUser")] shouldBe OperationMockState.Network
        allNetwork[OperationKey("catalog-api", "getProduct")] shouldBe OperationMockState.Network
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

        coEvery { repository.discoverResponseFiles(any<OperationKey>()) } coAnswers {
            when (firstArg<OperationKey>().operationId) {
                "getUser" -> listOf(MockResponse(200, "default", "Success (200)", "{}"))
                "createUser" -> listOf(MockResponse(201, "default", "Created (201)", "{}"))
                "getProduct" -> listOf(MockResponse(200, "default", "Success (200)", "{}"))
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
        coEvery { repository.setOperationMockState(any<OperationKey>(), any()) } coAnswers {
            val key = firstArg<OperationKey>()
            val state = secondArg<OperationMockState>()
            stateFlow.value = stateFlow.value.withOperationState(key, state)
        }
        coEvery { repository.setAllOperationStates(any()) } coAnswers {
            val states = firstArg<Map<OperationKey, OperationMockState>>()
            stateFlow.value = stateFlow.value.copy(
                operationStates = states.mapKeys { (key, _) -> key.compositeKey }
            )
        }
        coEvery { repository.resetKnownOperationsToNetwork() } coAnswers {
            stateFlow.value = stateFlow.value.resetAllToNetwork()
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
                    ),
                    Operation(
                        operationId = "createUser",
                        name = "Create User",
                        path = "/api/users",
                        method = "POST"
                    )
                )
            ),
            ApiSpec(
                id = "catalog-api",
                name = "Catalog API",
                servers = listOf("https://api.example.com"),
                operations = listOf(
                    Operation(
                        operationId = "getProduct",
                        name = "Get Product",
                        path = "/api/products/{productId}",
                        method = "GET"
                    )
                )
            )
        )
    )
}
