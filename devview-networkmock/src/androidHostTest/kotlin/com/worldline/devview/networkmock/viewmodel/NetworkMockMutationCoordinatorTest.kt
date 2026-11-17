package com.worldline.devview.networkmock.viewmodel

import com.worldline.devview.networkmock.core.model.OperationKey
import com.worldline.devview.networkmock.core.model.OperationMockState
import com.worldline.devview.networkmock.core.repository.MockStateRepository
import com.worldline.devview.test.ViewModelTest
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

class NetworkMockMutationCoordinatorTest : ViewModelTest() {
    @BeforeTest
    override fun setup() = super.setup()

    @AfterTest
    override fun tearDown() = super.tearDown()

    @Test
    fun synchronousAcceptance_preservesOriginsAndExecutesEveryCommandInOrder() = runTest {
        val repository = mockk<MockStateRepository>()
        val writes = mutableListOf<String>()
        val release = CompletableDeferred<Unit>()
        coEvery { repository.setGlobalMockingEnabled(true) } coAnswers {
            writes += "global"
            release.await()
        }
        coEvery { repository.setOperationMockState(any(), any()) } coAnswers { writes += "select" }
        coEvery { repository.resetOperationSequencePosition(any()) } coAnswers { writes += "position" }
        coEvery { repository.resetKnownOperationsToNetwork() } coAnswers { writes += "reset" }
        val coordinator = NetworkMockMutationCoordinator(
            repository,
            CoroutineScope(backgroundScope.coroutineContext + StandardTestDispatcher(testScheduler))
        )
        val origin = MutationOrigin(entryId = coordinator.createEntryId(), sheetId = 4L)
        val key = OperationKey("api", "get")
        val tickets = listOf(
            coordinator.submit(NetworkMockMutation.GlobalToggle(true), origin),
            coordinator.submit(NetworkMockMutation.Select(key, OperationMockState.Network), origin),
            coordinator.submit(NetworkMockMutation.ResetSequence(key), origin),
            coordinator.submit(NetworkMockMutation.ResetAll, origin)
        )

        tickets.map { it.id } shouldBe listOf(1L, 2L, 3L, 4L)
        tickets.map { it.origin } shouldBe List(4) { origin }
        coordinator.state.value.pendingCount shouldBe 4
        coordinator.state.value.head shouldBe tickets.first()
        coordinator.interactionsEnabled shouldBe false
        runCurrent()
        writes shouldBe listOf("global")
        tickets.first().outcome.value shouldBe MutationOutcome.Running

        release.complete(Unit)
        runCurrent()
        writes shouldBe listOf("global", "select", "position", "reset")
        tickets.forEach { it.outcome.value shouldBe MutationOutcome.Succeeded }
        coordinator.state.value.pendingCount shouldBe 0
        coordinator.interactionsEnabled shouldBe true
    }

    @Test
    fun retryFailedHead_keepsItsTicketAndFrozenSequence_beforeExecutingTail() = runTest {
        val repository = mockk<MockStateRepository>()
        val attempts = mutableListOf<OperationMockState>()
        var tailWrites = 0
        coEvery { repository.setOperationMockState(any(), any()) } coAnswers {
            attempts += secondArg<OperationMockState>()
            if (attempts.size == 1) error("disk error")
        }
        coEvery { repository.setGlobalMockingEnabled(false) } coAnswers { tailWrites++ }
        val coordinator = NetworkMockMutationCoordinator(
            repository,
            CoroutineScope(backgroundScope.coroutineContext + StandardTestDispatcher(testScheduler))
        )
        val responses = mutableListOf(
            OperationMockState.Mock(200, "application/json", "default"),
            OperationMockState.Mock(404, "application/json", "default")
        )
        val expected = OperationMockState.Sequence(responses.toList())
        val origin = MutationOrigin(entryId = coordinator.createEntryId())
        val head = coordinator.submit(
            NetworkMockMutation.Select(OperationKey("api", "get"), OperationMockState.Sequence(responses)),
            origin
        )
        val tail = coordinator.submit(NetworkMockMutation.GlobalToggle(false), origin)
        responses.clear()
        runCurrent()

        coordinator.state.value.head shouldBe head
        coordinator.state.value.failure shouldBe "disk error"
        head.outcome.value shouldBe MutationOutcome.Failed("disk error")
        tail.outcome.value shouldBe MutationOutcome.Queued
        tailWrites shouldBe 0
        attempts shouldBe listOf(expected)

        coordinator.retryFailed()
        head.outcome.value shouldBe MutationOutcome.Queued
        runCurrent()
        attempts shouldBe listOf(expected, expected)
        head.outcome.value shouldBe MutationOutcome.Succeeded
        tail.outcome.value shouldBe MutationOutcome.Succeeded
        tailWrites shouldBe 1
        coordinator.interactionsEnabled shouldBe true
    }

    @Test
    fun cancelOnlyFailedHead_unblocksTail_andCannotCancelRunningWrite() = runTest {
        val repository = mockk<MockStateRepository>()
        var attempts = 0
        val release = CompletableDeferred<Unit>()
        coEvery { repository.resetKnownOperationsToNetwork() } coAnswers {
            attempts++
            error("disk error")
        }
        coEvery { repository.setGlobalMockingEnabled(true) } coAnswers { release.await() }
        val coordinator = NetworkMockMutationCoordinator(
            repository,
            CoroutineScope(backgroundScope.coroutineContext + StandardTestDispatcher(testScheduler))
        )
        val origin = MutationOrigin(entryId = coordinator.createEntryId())
        val head = coordinator.submit(NetworkMockMutation.ResetAll, origin)
        val tail = coordinator.submit(NetworkMockMutation.GlobalToggle(true), origin)
        runCurrent()
        coordinator.cancelFailed()
        head.outcome.value shouldBe MutationOutcome.Cancelled
        runCurrent()
        attempts shouldBe 1
        tail.outcome.value shouldBe MutationOutcome.Running
        coordinator.cancelFailed()
        coordinator.retryFailed()
        coordinator.state.value.head shouldBe tail
        release.complete(Unit)
        runCurrent()
        tail.outcome.value shouldBe MutationOutcome.Succeeded
        coordinator.cancelFailed()
        coordinator.retryFailed()
        coordinator.state.value.pendingCount shouldBe 0
    }
}
