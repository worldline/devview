package com.worldline.devview.networkmock.viewmodel

import androidx.lifecycle.SavedStateHandle
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
import com.worldline.devview.test.ViewModelTest
import com.worldline.devview.test.collectState
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class OperationDetailViewModelTest : ViewModelTest() {
    @BeforeTest
    override fun setup() = super.setup()

    @AfterTest
    override fun tearDown() = super.tearDown()

    @Test
    fun staleDiscoveryAndError_cannotReplaceLatestReload() = runTest {
        val fixture = Fixture(backgroundScope)
        val discoveries = mutableListOf<Continuation<List<MockResponse>>>()
        coEvery { fixture.config.discoverResponseFiles(any()) } coAnswers {
            suspendCoroutine { discoveries += it }
        }
        val vm = fixture.viewModel()
        collectState(vm.state)
        vm.state.value shouldBe OperationSheetState.Loading
        vm.reload()
        discoveries[1].resume(listOf(responseB))
        runCurrent()
        vm.state.value.shouldBeInstanceOf<OperationSheetState.Content>().responses shouldBe listOf(responseB)
        discoveries[0].resumeWithException(IllegalStateException("stale discovery"))
        runCurrent()
        vm.state.value.shouldBeInstanceOf<OperationSheetState.Content>().responses shouldBe listOf(responseB)
        vm.reload()
        discoveries[2].resume(listOf(responseA))
        runCurrent()
        vm.state.value.shouldBeInstanceOf<OperationSheetState.Content>().responses shouldBe listOf(responseA)
    }

    @Test
    fun staleStateRead_cannotReseedDraftOrPublishOldDetails() = runTest {
        val fixture = Fixture(backgroundScope)
        var firstRead: Continuation<NetworkMockState>? = null
        var reads = 0
        coEvery { fixture.repository.getState() } coAnswers {
            reads++
            if (reads == 1) suspendCoroutine { firstRead = it } else fixture.current.value
        }
        val vm = fixture.viewModel()
        collectState(vm.state)
        vm.state.value shouldBe OperationSheetState.Loading
        fixture.responses = listOf(responseB)
        fixture.current.value = fixture.current.value.withOperationState(
            key,
            OperationMockState.Sequence(listOf(responseB.asState(), responseB.asState()))
        )
        vm.reload()
        runCurrent()
        val latestDraft = vm.draft.value
        latestDraft.map { it.response } shouldBe listOf(responseB.asState(), responseB.asState())
        firstRead!!.resume(
            NetworkMockState().withOperationState(
                key,
                OperationMockState.Sequence(listOf(responseA.asState(), responseA.asState()))
            )
        )
        runCurrent()
        vm.draft.value shouldBe latestDraft
        vm.state.value.shouldBeInstanceOf<OperationSheetState.Content>().responses shouldBe listOf(responseB)
    }

    @Test
    fun duplicateDraftSteps_keepIndependentIdsAcrossMovesRemovalAndReconstruction() = runTest {
        val fixture = Fixture(backgroundScope)
        val handle = SavedStateHandle()
        val vm = fixture.viewModel(handle)
        collectState(vm.state)
        vm.add(responseA)
        vm.add(responseA)
        val first = vm.draft.value[0]
        val second = vm.draft.value[1]
        (first.id == second.id) shouldBe false
        vm.move(second.id, -1)
        vm.draft.value shouldBe listOf(second, first)
        vm.move(first.id, 1)
        vm.draft.value shouldBe listOf(second, first)
        vm.remove(second.id)
        vm.draft.value shouldBe listOf(first)
        val restored = fixture.viewModel(SavedStateHandle(mapOf(DRAFT_KEY to handle.get<String>(DRAFT_KEY))))
        collectState(restored.state)
        restored.draft.value shouldBe listOf(first)
        coVerify(exactly = 0) { fixture.repository.setOperationMockState(any(), any()) }
    }

    @Test
    fun existingSequenceSeedsOnce_andSavedEmptyDraftDoesNotReseed() = runTest {
        val fixture = Fixture(backgroundScope)
        fixture.current.value = fixture.current.value.withOperationState(
            key,
            OperationMockState.Sequence(listOf(responseA.asState(), responseB.asState()), currentIndex = 1)
        )
        val handle = SavedStateHandle()
        val vm = fixture.viewModel(handle)
        collectState(vm.state)
        vm.draft.value.map { it.response } shouldBe listOf(responseA.asState(), responseB.asState())
        vm.draft.value.toList().forEach { vm.remove(it.id) }
        vm.draft.value shouldBe emptyList()
        val restored = fixture.viewModel(SavedStateHandle(mapOf(DRAFT_KEY to handle.get<String>(DRAFT_KEY))))
        collectState(restored.state)
        restored.draft.value shouldBe emptyList()
        restored.saveSequence()
        restored.selection.value shouldBe null
    }

    @Test
    fun unresolvedDraft_isRetainedAndFrozenUntilReloadResolvesEveryStep() = runTest {
        val fixture = Fixture(backgroundScope)
        fixture.responses = listOf(responseA)
        val steps = listOf(SequenceDraftStep("a", responseA.asState()), SequenceDraftStep("b", responseB.asState()))
        val vm = fixture.viewModel(SavedStateHandle(mapOf(DRAFT_KEY to Json.encodeToString(steps))))
        collectState(vm.state)
        vm.add(responseA)
        vm.remove("b")
        vm.move("a", 1)
        vm.saveSequence()
        vm.draft.value shouldBe steps
        vm.selection.value shouldBe null
        fixture.responses = listOf(responseA, responseB)
        vm.reload()
        vm.move("b", -1)
        vm.draft.value shouldBe steps.reversed()
        vm.saveSequence()
        runCurrent()
        vm.selection.value!!.outcome.value shouldBe MutationOutcome.Succeeded
        fixture.current.value.getOperationState(key) shouldBe OperationMockState.Sequence(
            listOf(responseB.asState(), responseA.asState())
        )
    }

    @Test
    fun failedSave_keepsDraftAndBlocksEditsReloadAndNavigationUntilRetry() = runTest {
        val fixture = Fixture(backgroundScope)
        var attempts = 0
        coEvery { fixture.repository.setOperationMockState(any(), any()) } coAnswers {
            attempts++
            if (attempts == 1) error("disk error")
            fixture.current.value = fixture.current.value.withOperationState(firstArg(), secondArg())
        }
        val vm = fixture.viewModel()
        collectState(vm.state)
        vm.add(responseA)
        vm.saveSequence()
        vm.selection.value shouldBe null
        vm.add(responseB)
        val draft = vm.draft.value
        vm.saveSequence()
        val ticket = vm.selection.value!!
        runCurrent()
        ticket.outcome.value shouldBe MutationOutcome.Failed("disk error")
        vm.remove(draft[0].id)
        vm.move(draft[0].id, 1)
        vm.add(responseA)
        vm.apply(responseA)
        vm.reload()
        vm.draft.value shouldBe draft
        vm.selection.value shouldBe ticket
        vm.acknowledgeSelection(ticket.id) shouldBe false
        coVerify(exactly = 0) { fixture.config.invalidate() }
        fixture.mutations.retryFailed()
        runCurrent()
        ticket.outcome.value shouldBe MutationOutcome.Succeeded
        vm.acknowledgeSelection(ticket.id + 1) shouldBe false
        vm.acknowledgeSelection(ticket.id) shouldBe true
        vm.acknowledgeSelection(ticket.id) shouldBe false
        attempts shouldBe 2
    }

    @Test
    fun successfulApply_isAcknowledgedOnlyAfterTailFinishes_andOnlyOnce() = runTest {
        val scope = CoroutineScope(backgroundScope.coroutineContext + StandardTestDispatcher(testScheduler))
        val fixture = Fixture(scope)
        val release = CompletableDeferred<Unit>()
        coEvery { fixture.repository.setGlobalMockingEnabled(false) } coAnswers { release.await() }
        val vm = fixture.viewModel()
        collectState(vm.state)
        vm.apply(responseA)
        val ticket = vm.selection.value!!
        vm.acknowledgeSelection(ticket.id) shouldBe false
        fixture.mutations.submit(NetworkMockMutation.GlobalToggle(false), MutationOrigin(entryId = 99L))
        runCurrent()
        ticket.outcome.value shouldBe MutationOutcome.Succeeded
        vm.acknowledgeSelection(ticket.id) shouldBe false
        release.complete(Unit)
        runCurrent()
        vm.acknowledgeSelection(ticket.id) shouldBe true
        vm.selection.value shouldBe null
        vm.acknowledgeSelection(ticket.id) shouldBe false
    }

    @Test
    fun loadError_hasRetry_andUnknownResponseCannotBeApplied() = runTest {
        val fixture = Fixture(backgroundScope)
        coEvery { fixture.config.discoverResponseFiles(any()) } throws IllegalStateException("missing resource")
        val vm = fixture.viewModel()
        collectState(vm.state)
        vm.state.value shouldBe OperationSheetState.Error("missing resource")
        vm.apply(responseA)
        vm.selection.value shouldBe null
        coEvery { fixture.config.discoverResponseFiles(any()) } returns listOf(responseA)
        vm.reload()
        vm.state.value.shouldBeInstanceOf<OperationSheetState.Content>().responses shouldBe listOf(responseA)
        vm.apply(responseB)
        vm.selection.value shouldBe null
        vm.apply(responseA)
        runCurrent()
        vm.selection.value!!.outcome.value shouldBe MutationOutcome.Succeeded
    }

    private class Fixture(scope: CoroutineScope) {
        val config = mockk<MockConfigRepository>()
        val repository = mockk<MockStateRepository>()
        val current = MutableStateFlow(NetworkMockState())
        var responses = listOf(responseA, responseB)
        val mutations: NetworkMockMutationCoordinator

        init {
            coEvery { config.loadConfiguration() } returns Result.success(
                MockConfiguration(
                    listOf(
                        ApiSpec(
                            id = "api",
                            name = "API",
                            servers = listOf("https://example.com"),
                            operations = listOf(Operation("get", "Get item", "/item", HttpMethod.Get))
                        )
                    )
                )
            )
            coEvery { config.discoverResponseFiles(any()) } coAnswers { responses }
            every { config.invalidate() } just Runs
            every { repository.observeState() } returns current
            coEvery { repository.getState() } coAnswers { current.value }
            coEvery { repository.setOperationMockState(any(), any()) } coAnswers {
                current.value = current.value.withOperationState(firstArg(), secondArg())
            }
            mutations = NetworkMockMutationCoordinator(repository, scope)
        }

        fun viewModel(handle: SavedStateHandle = SavedStateHandle()): OperationDetailViewModel =
            OperationDetailViewModel(key, config, repository, handle, mutations)
    }

    private companion object {
        const val DRAFT_KEY = "networkmock_sequence_draft"
        val key = OperationKey("api", "get")
        val responseA = MockResponse(200, "application/json", "a", "Success", "{}")
        val responseB = MockResponse(404, "application/json", "b", "Missing", "{\"error\":true}")
    }
}
