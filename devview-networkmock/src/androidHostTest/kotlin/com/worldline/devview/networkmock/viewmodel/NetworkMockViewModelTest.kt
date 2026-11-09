package com.worldline.devview.networkmock.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.worldline.devview.networkmock.MockStateFilter
import com.worldline.devview.networkmock.OperationFilters
import com.worldline.devview.networkmock.OperationSort
import com.worldline.devview.networkmock.core.NetworkMockResourceLoader
import com.worldline.devview.networkmock.core.model.ApiSpec
import com.worldline.devview.networkmock.core.model.HttpMethod
import com.worldline.devview.networkmock.core.model.MockConfiguration
import com.worldline.devview.networkmock.core.model.MockResponse
import com.worldline.devview.networkmock.core.model.NetworkMockState
import com.worldline.devview.networkmock.core.model.Operation
import com.worldline.devview.networkmock.core.model.OperationKey
import com.worldline.devview.networkmock.core.model.OperationMockState
import com.worldline.devview.networkmock.core.repository.MockConfigRepository
import com.worldline.devview.networkmock.core.repository.MockStateRepository
import com.worldline.devview.test.FakePreferencesDataStore
import com.worldline.devview.test.ViewModelTest
import com.worldline.devview.test.collectState
import com.worldline.devview.test.collectStates
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest

private const val LARGE_SPEC_PATH = "specs/large-api.json"

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

        // #98: the main list is built from spec metadata only — no response body is read
        coVerify(exactly = 0) { configRepository.discoverResponseFiles(key = any()) }
    }

    @Test
    fun loadingLargeSpec_readsOnlyTheSpecFile_notAnyResponseBody() = runTest {
        val loader = RecordingResourceLoader(specJson = largeSpecJson(operationCount = 300))
        val configRepository = MockConfigRepository(
            specPaths = listOf(LARGE_SPEC_PATH),
            resourceLoader = loader
        )
        val stateRepository = MockStateRepository(dataStore = FakePreferencesDataStore())

        val viewModel = NetworkMockViewModel(configRepository, stateRepository)

        collectState(viewModel.uiState)

        val content = viewModel.uiState.value.shouldBeInstanceOf<NetworkMockUiState.Content>()
        content.specs.single().operations.shouldHaveSize(300)
        loader.loadedPaths shouldBe listOf(LARGE_SPEC_PATH)
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
        val response = MockResponse(
            statusCode = 200,
            mediaType = "application/json",
            exampleName = "default",
            displayName = "Success (200)",
            content = "{}"
        )

        viewModel.setOperationMockState(key, response)
        stateFlow.value.getOperationState(key)
            .shouldBeInstanceOf<OperationMockState.Mock>()
            .let {
                it.statusCode shouldBe 200
                it.mediaType shouldBe "application/json"
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
            response = MockResponse(
                statusCode = 200,
                mediaType = "application/json",
                exampleName = "default",
                displayName = "Success (200)",
                content = "{}"
            )
        )
        viewModel.setOperationMockState(
            key = OperationKey(specId = "catalog-api", operationId = "getProduct"),
            response = MockResponse(
                statusCode = 200,
                mediaType = "application/json",
                exampleName = "default",
                displayName = "Success (200)",
                content = "{}"
            )
        )

        viewModel.resetAllToNetwork()

        coVerify(exactly = 1) { stateRepository.resetKnownOperationsToNetwork() }
        stateFlow.value.getOperationState(OperationKey("user-api", "getUser")) shouldBe OperationMockState.Network
        stateFlow.value.getOperationState(OperationKey("user-api", "createUser")) shouldBe OperationMockState.Network
        stateFlow.value.getOperationState(OperationKey("catalog-api", "getProduct")) shouldBe OperationMockState.Network
    }

    @Test
    fun reloadConfiguration_invalidatesRepositoryThenReloads() = runTest {
        val stateFlow = MutableStateFlow(NetworkMockState())
        val configRepository =
            createConfigRepositoryMock(loadResult = Result.success(testConfiguration()))
        val stateRepository = createStateRepositoryMock(stateFlow)
        every { configRepository.invalidate() } just Runs

        val viewModel = NetworkMockViewModel(configRepository, stateRepository)
        collectState(viewModel.uiState)

        viewModel.reloadConfiguration()

        coVerifyOrder {
            configRepository.loadConfiguration()
            configRepository.invalidate()
            configRepository.loadConfiguration()
        }
        viewModel.uiState.value.shouldBeInstanceOf<NetworkMockUiState.Content>()
    }

    @Test
    fun sheetState_isHidden_initially() = runTest {
        val stateFlow = MutableStateFlow(NetworkMockState())
        val configRepository =
            createConfigRepositoryMock(loadResult = Result.success(testConfiguration()))
        val stateRepository = createStateRepositoryMock(stateFlow)

        val viewModel = NetworkMockViewModel(configRepository, stateRepository)

        collectStates(viewModel.uiState, viewModel.sheetState)

        viewModel.sheetState.value shouldBe OperationSheetState.Hidden
    }

    @Test
    fun openOperation_emitsLoading_whileDiscoveryInProgress() = runTest {
        val stateFlow = MutableStateFlow(NetworkMockState())
        val configRepository = createConfigRepositoryMock(
            loadResult = Result.success(testConfiguration()),
            discoveryDelayMs = 500
        )
        val stateRepository = createStateRepositoryMock(stateFlow)
        val key = OperationKey(specId = "user-api", operationId = "getUser")

        val viewModel = NetworkMockViewModel(configRepository, stateRepository)
        collectStates(viewModel.uiState, viewModel.sheetState)

        viewModel.openOperation(key = key)

        viewModel.sheetState.value shouldBe OperationSheetState.Loading
    }

    @Test
    fun openOperation_emitsContentState_afterSuccessfulDiscovery() = runTest {
        val stateFlow = MutableStateFlow(NetworkMockState())
        val configRepository = createConfigRepositoryMock(
            loadResult = Result.success(testConfiguration())
        )
        val stateRepository = createStateRepositoryMock(stateFlow)
        val key = OperationKey(specId = "user-api", operationId = "getUser")

        val viewModel = NetworkMockViewModel(configRepository, stateRepository)
        collectStates(viewModel.uiState, viewModel.sheetState)

        viewModel.openOperation(key = key)

        val content = viewModel.sheetState.value.shouldBeInstanceOf<OperationSheetState.Content>()
        content.operationUiModel.descriptor.key shouldBe key
        content.responses.size shouldBe 1
        content.operationUiModel.currentState shouldBe OperationMockState.Network
    }

    @Test
    fun openOperation_emitsErrorState_whenDiscoveryFails() = runTest {
        val stateFlow = MutableStateFlow(NetworkMockState())
        val configRepository = createConfigRepositoryMock(
            loadResult = Result.success(testConfiguration()),
            discoveryException = RuntimeException("disk error")
        )
        val stateRepository = createStateRepositoryMock(stateFlow)

        val viewModel = NetworkMockViewModel(configRepository, stateRepository)
        collectStates(viewModel.uiState, viewModel.sheetState)

        viewModel.openOperation(key = OperationKey(specId = "user-api", operationId = "getUser"))

        val error = viewModel.sheetState.value.shouldBeInstanceOf<OperationSheetState.Error>()
        error.message shouldBe "disk error"
    }

    @Test
    fun openOperation_emitsErrorState_whenOperationConfigNotFound() = runTest {
        val stateFlow = MutableStateFlow(NetworkMockState())
        val configRepository = createConfigRepositoryMock(
            loadResult = Result.success(testConfiguration())
        )
        val stateRepository = createStateRepositoryMock(stateFlow)

        val viewModel = NetworkMockViewModel(configRepository, stateRepository)
        collectStates(viewModel.uiState, viewModel.sheetState)

        viewModel.openOperation(key = OperationKey(specId = "unknown-api", operationId = "unknown"))

        viewModel.sheetState.value.shouldBeInstanceOf<OperationSheetState.Error>()
    }

    @Test
    fun closeSheet_resetsToHidden() = runTest {
        val stateFlow = MutableStateFlow(NetworkMockState())
        val configRepository = createConfigRepositoryMock(
            loadResult = Result.success(testConfiguration())
        )
        val stateRepository = createStateRepositoryMock(stateFlow)

        val viewModel = NetworkMockViewModel(configRepository, stateRepository)
        collectStates(viewModel.uiState, viewModel.sheetState)

        viewModel.openOperation(key = OperationKey(specId = "user-api", operationId = "getUser"))
        viewModel.sheetState.value.shouldBeInstanceOf<OperationSheetState.Content>()

        viewModel.closeSheet()

        viewModel.sheetState.value shouldBe OperationSheetState.Hidden
    }

    @Test
    fun openOperation_switchingToAnotherOperation_doesNotFlashPreviousContent() = runTest {
        val stateFlow = MutableStateFlow(NetworkMockState())
        val firstKey = OperationKey(specId = "user-api", operationId = "getUser")
        val secondKey = OperationKey(specId = "user-api", operationId = "createUser")
        val configRepository = mockk<MockConfigRepository>()
        coEvery { configRepository.loadConfiguration() } returns Result.success(testConfiguration())
        coEvery { configRepository.discoverResponseFiles(key = firstKey) } returns listOf(
            MockResponse(
                statusCode = 200,
                mediaType = "application/json",
                exampleName = "default",
                displayName = "Success (200)",
                content = "{}"
            )
        )
        coEvery { configRepository.discoverResponseFiles(key = secondKey) } coAnswers {
            delay(timeMillis = 500)
            listOf(
                MockResponse(
                    statusCode = 201,
                    mediaType = "application/json",
                    exampleName = "default",
                    displayName = "Created (201)",
                    content = "{}"
                )
            )
        }
        val stateRepository = createStateRepositoryMock(stateFlow)

        val viewModel = NetworkMockViewModel(configRepository, stateRepository)
        collectStates(viewModel.uiState, viewModel.sheetState)

        viewModel.openOperation(key = firstKey)
        viewModel.sheetState.value.shouldBeInstanceOf<OperationSheetState.Content>()

        // Re-opening a second (still-discovering) operation must not show the first one's
        // stale content — see LoadedOperation.key in NetworkMockViewModel.sheetState.
        viewModel.openOperation(key = secondKey)

        viewModel.sheetState.value shouldBe OperationSheetState.Loading
    }

    @Test
    fun setOperationMockState_reflectsInSheetContent() = runTest {
        val stateFlow = MutableStateFlow(NetworkMockState())
        val configRepository = createConfigRepositoryMock(
            loadResult = Result.success(testConfiguration())
        )
        val stateRepository = createStateRepositoryMock(stateFlow)
        val key = OperationKey(specId = "user-api", operationId = "getUser")
        val response = MockResponse(
            statusCode = 200,
            mediaType = "application/json",
            exampleName = "default",
            displayName = "Success (200)",
            content = "{}"
        )

        val viewModel = NetworkMockViewModel(configRepository, stateRepository)
        collectStates(viewModel.uiState, viewModel.sheetState)

        viewModel.openOperation(key = key)
        viewModel.setOperationMockState(key = key, response = response)

        val content = viewModel.sheetState.value.shouldBeInstanceOf<OperationSheetState.Content>()
        content.operationUiModel.currentState shouldBe OperationMockState.Mock(
            statusCode = 200,
            mediaType = "application/json",
            exampleName = "default"
        )
    }

    @Test
    fun globalToggleSuccess_isNotAnOperationSelection_forALaterSheet() = runTest {
        val config = createConfigRepositoryMock(loadResult = Result.success(testConfiguration()))
        val repository = createStateRepositoryMock(MutableStateFlow(NetworkMockState()))
        val viewModel = NetworkMockViewModel(config, repository)
        collectStates(viewModel.uiState, viewModel.sheetState)

        viewModel.setGlobalMockingEnabled(enabled = false)
        viewModel.selection.value shouldBe null
        viewModel.openOperation(key = OperationKey(specId = "user-api", operationId = "getUser"))
        viewModel.selection.value shouldBe null
        viewModel.sheetState.value.shouldBeInstanceOf<OperationSheetState.Content>()
    }

    @Test
    fun successfulSelection_acknowledgesOnce_andCannotCloseANewerSheet() = runTest {
        val config = createConfigRepositoryMock(loadResult = Result.success(testConfiguration()))
        val repository = createStateRepositoryMock(MutableStateFlow(NetworkMockState()))
        val viewModel = NetworkMockViewModel(config, repository)
        val first = OperationKey(specId = "user-api", operationId = "getUser")
        collectStates(viewModel.uiState, viewModel.sheetState)
        viewModel.openOperation(key = first)
        viewModel.setOperationMockState(key = first, response = null)
        val firstTicket = requireNotNull(viewModel.selection.value)
        viewModel.canAcknowledgeSelection(ticketId = firstTicket.id) shouldBe true

        val second = OperationKey(specId = "user-api", operationId = "createUser")
        viewModel.openOperation(key = second)
        viewModel.selection.value shouldBe null
        viewModel.acknowledgeSelection(ticketId = firstTicket.id) shouldBe false
        viewModel.sheetState.value.shouldBeInstanceOf<OperationSheetState.Content>()

        viewModel.setOperationMockState(key = second, response = null)
        val secondTicket = requireNotNull(viewModel.selection.value)
        viewModel.acknowledgeSelection(ticketId = secondTicket.id) shouldBe true
        viewModel.acknowledgeSelection(ticketId = secondTicket.id) shouldBe false
        viewModel.sheetState.value shouldBe OperationSheetState.Hidden
    }

    @Test
    fun reloadAcceptedBeforeABusyWrite_isConsumedAfterTheWrite_notDroppedOrRepeated() = runTest {
        val config = createConfigRepositoryMock(loadResult = Result.success(testConfiguration()))
        val repository = createStateRepositoryMock(MutableStateFlow(NetworkMockState()))
        val release = CompletableDeferred<Unit>()
        every { config.invalidate() } just Runs
        coEvery { repository.setGlobalMockingEnabled(enabled = false) } coAnswers { release.await() }
        val viewModel = NetworkMockViewModel(config, repository)
        collectState(viewModel.uiState)
        viewModel.setGlobalMockingEnabled(enabled = false)
        viewModel.consumeReload(generation = 1L)
        coVerify(exactly = 1) { config.loadConfiguration() }

        release.complete(Unit)
        viewModel.consumeReload(generation = 1L)
        viewModel.consumeReload(generation = 1L)
        coVerify(exactly = 2) { config.loadConfiguration() }
        io.mockk.verify(exactly = 1) { config.invalidate() }
    }

    @Test
    fun listState_restoresAllFiltersAndSelectedApiGroup() = runTest {
        val handle = SavedStateHandle()
        val configRepository = createConfigRepositoryMock(loadResult = Result.success(testConfiguration()))
        val stateRepository = createStateRepositoryMock(MutableStateFlow(NetworkMockState()))
        val viewModel = NetworkMockViewModel(configRepository, stateRepository, handle)
        val filters = OperationFilters(
            query = "USER",
            states = setOf(MockStateFilter.MOCKED),
            version = "v2",
            methods = setOf("GET", "POST"),
            tags = setOf("Users"),
            sort = OperationSort.MOCKED_FIRST
        )
        viewModel.updateFilters(specId = "user-api", filters = filters)
        viewModel.updateFilters(specId = "catalog-api", filters = OperationFilters(query = "product"))
        viewModel.selectSpec(specId = "catalog-api")
        viewModel.setFiltersExpanded(expanded = true)

        val restoredHandle = SavedStateHandle(
            initialState = mapOf("networkmock_list" to handle.get<String>(key = "networkmock_list"))
        )
        val restored = NetworkMockViewModel(configRepository, stateRepository, restoredHandle)
        restored.listState.value shouldBe viewModel.listState.value
        restored.listState.value.filters["user-api"] shouldBe filters
        restored.listState.value.selectedSpecId shouldBe "catalog-api"
        restored.listState.value.expanded shouldBe true
        filters.activeCount shouldBe 6
    }

    @Test
    fun clearingFilters_preservesQueryWithoutChangingAnotherApiGroup() = runTest {
        val configRepository = createConfigRepositoryMock(loadResult = Result.success(testConfiguration()))
        val stateRepository = createStateRepositoryMock(MutableStateFlow(NetworkMockState()))
        val viewModel = NetworkMockViewModel(configRepository, stateRepository)
        viewModel.updateFilters(
            specId = "user-api",
            filters = OperationFilters(query = "user", version = "v1", sort = OperationSort.METHOD)
        )
        viewModel.updateFilters(
            specId = "catalog-api",
            filters = OperationFilters(query = "product", tags = setOf("Catalog"))
        )
        viewModel.updateFilters(specId = "user-api", filters = OperationFilters(query = "user"))

        viewModel.listState.value.filters["user-api"] shouldBe OperationFilters(query = "user")
        viewModel.listState.value.filters["catalog-api"] shouldBe OperationFilters(query = "product", tags = setOf("Catalog"))
    }

    @Test
    fun selectingApiGroupAndExpandingFilters_preservesAnUncollectedFilterEdit() = runTest {
        val configRepository = createConfigRepositoryMock(loadResult = Result.success(testConfiguration()))
        val stateRepository = createStateRepositoryMock(MutableStateFlow(NetworkMockState()))
        val viewModel = NetworkMockViewModel(configRepository, stateRepository)
        val filters = OperationFilters(query = "just edited", tags = setOf("Users"))

        viewModel.updateFilters(specId = "user-api", filters = filters)
        viewModel.selectSpec(specId = "catalog-api")
        viewModel.setFiltersExpanded(expanded = true)

        viewModel.listState.value.filters["user-api"] shouldBe filters
        viewModel.listState.value.selectedSpecId shouldBe "catalog-api"
        viewModel.listState.value.expanded shouldBe true
    }

    private fun createConfigRepositoryMock(
        loadResult: Result<MockConfiguration>,
        loadDelayMs: Long = 0L,
        discoveryResult: List<MockResponse> = listOf(
            MockResponse(
                statusCode = 200,
                mediaType = "application/json",
                exampleName = "default",
                displayName = "Success (200)",
                content = "{}"
            )
        ),
        discoveryException: Exception? = null,
        discoveryDelayMs: Long = 0L
    ): MockConfigRepository {
        val repository = mockk<MockConfigRepository>()

        coEvery { repository.loadConfiguration() } coAnswers {
            if (loadDelayMs > 0) {
                delay(loadDelayMs)
            }
            loadResult
        }

        if (discoveryException != null) {
            coEvery { repository.discoverResponseFiles(key = any()) } throws discoveryException
        } else {
            coEvery { repository.discoverResponseFiles(key = any()) } coAnswers {
                if (discoveryDelayMs > 0) {
                    delay(discoveryDelayMs)
                }
                discoveryResult
            }
        }

        return repository
    }

    private fun createStateRepositoryMock(
        stateFlow: MutableStateFlow<NetworkMockState>
    ): MockStateRepository {
        val repository = mockk<MockStateRepository>()
        val registeredKeys = mutableSetOf<OperationKey>()

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
            stateFlow.value = registeredKeys.fold(stateFlow.value.resetAllToNetwork()) { state, key ->
                state.withOperationState(key, OperationMockState.Network)
            }
        }

        every { repository.observeState() } returns stateFlow
        every { repository.registerOperations(any()) } answers {
            registeredKeys.addAll(firstArg<Collection<OperationKey>>())
            Unit
        }

        coEvery { repository.getState() } coAnswers { stateFlow.value }

        return repository
    }

    /** Records every path loaded; the single spec file lives at [LARGE_SPEC_PATH]. */
    private class RecordingResourceLoader(private val specJson: String) : NetworkMockResourceLoader {
        val loadedPaths = mutableListOf<String>()

        override suspend fun load(path: String): ByteArray {
            loadedPaths += path
            return if (path == LARGE_SPEC_PATH) {
                specJson.encodeToByteArray()
            } else {
                error("Unexpected resource read: $path")
            }
        }
    }

    /**
     * Builds a spec with [operationCount] operations, each declaring three response
     * examples — large enough that eagerly reading every response body would be obvious in
     * a test run, and none of the `externalValue` files backing those examples actually
     * exist, so [loadingLargeSpec_readsOnlyTheSpecFile_notAnyResponseBody] fails loudly if
     * anything but the spec itself is ever read.
     */
    private fun largeSpecJson(operationCount: Int): String {
        val paths = (1..operationCount).joinToString(separator = ",\n") { index ->
            """
            "/operation$index": {
              "get": {
                "operationId": "op$index",
                "responses": {
                  "200": {
                    "content": { "application/json": { "examples": {
                      "default": { "externalValue": "responses/op$index-200-default.json" },
                      "alt": { "externalValue": "responses/op$index-200-alt.json" }
                    } } }
                  },
                  "404": {
                    "content": { "application/json": { "examples": {
                      "default": { "externalValue": "responses/op$index-404-default.json" }
                    } } }
                  }
                }
              }
            }
            """.trimIndent()
        }
        return """
            {
              "info": { "title": "Large API" },
              "servers": [ { "url": "https://api.example.com" } ],
              "paths": { $paths }
            }
        """.trimIndent()
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
                        method = HttpMethod.Get
                    ),
                    Operation(
                        operationId = "createUser",
                        name = "Create User",
                        path = "/api/users",
                        method = HttpMethod.Post
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
                        method = HttpMethod.Get
                    )
                )
            )
        )
    )
}
