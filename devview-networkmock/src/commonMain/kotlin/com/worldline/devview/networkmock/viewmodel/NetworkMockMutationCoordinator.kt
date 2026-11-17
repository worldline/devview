package com.worldline.devview.networkmock.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.worldline.devview.networkmock.core.model.OperationKey
import com.worldline.devview.networkmock.core.model.OperationMockState
import com.worldline.devview.networkmock.core.repository.MockStateRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal sealed interface NetworkMockMutation {
    data class GlobalToggle(val enabled: Boolean) : NetworkMockMutation

    data class Select(val key: OperationKey, val state: OperationMockState) : NetworkMockMutation

    data class ResetSequence(val key: OperationKey) : NetworkMockMutation

    data object ResetAll : NetworkMockMutation
}

internal sealed interface MutationOutcome {
    data object Queued : MutationOutcome

    data object Running : MutationOutcome

    data class Failed(val message: String) : MutationOutcome

    data object Succeeded : MutationOutcome

    data object Cancelled : MutationOutcome
}

internal data class MutationOrigin(val entryId: Long, val sheetId: Long? = null)

internal class MutationTicket(
    val id: Long,
    val origin: MutationOrigin,
    val command: NetworkMockMutation
) {
    private val privateOutcome = MutableStateFlow<MutationOutcome>(value = MutationOutcome.Queued)
    val outcome: StateFlow<MutationOutcome> = privateOutcome.asStateFlow()

    internal fun update(outcome: MutationOutcome) {
        privateOutcome.value = outcome
    }
}

internal data class NetworkMockMutationState(
    val head: MutationTicket? = null,
    val pendingCount: Int = 0,
    val failure: String? = null
) {
    val busy: Boolean get() = head != null
}

/** One module-owned consumer; a failed head blocks later accepted commands until Retry or Cancel. */
internal class NetworkMockMutationCoordinator(
    private val stateRepository: MockStateRepository,
    private val scope: CoroutineScope
) {
    private val queue = ArrayDeque<MutationTicket>()
    private val wakeups = Channel<Unit>(capacity = Channel.CONFLATED)
    private var nextId = 0L
    private var nextEntryId = 0L
    private val privateState = MutableStateFlow(value = NetworkMockMutationState())
    val state: StateFlow<NetworkMockMutationState> = privateState.asStateFlow()

    // The navigation metadata callback must also invalidate Compose's enabled state immediately.
    var interactionsEnabled: Boolean by mutableStateOf(value = true)
        private set

    // Any repository failure becomes recoverable UI state; cancellation still ends the session.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun consume() {
        while (wakeups.receiveCatching().isSuccess) {
            while (queue.isNotEmpty() && queue.first().outcome.value !is MutationOutcome.Failed) {
                val ticket = queue.first()
                ticket.update(outcome = MutationOutcome.Running)
                publish()
                try {
                    apply(command = ticket.command)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    ticket.update(
                        outcome = MutationOutcome.Failed(
                            message = error.message ?: "Could not save the mock setting."
                        )
                    )
                    publish()
                    break
                }
                queue.removeFirst()
                ticket.update(outcome = MutationOutcome.Succeeded)
                publish()
            }
        }
    }

    init {
        scope.launch { consume() }
    }

    fun createEntryId(): Long {
        check(value = nextEntryId < Long.MAX_VALUE) { "Network mock entry identifiers exhausted" }
        return ++nextEntryId
    }

    fun submit(command: NetworkMockMutation, origin: MutationOrigin): MutationTicket {
        check(value = scope.isActive) { "Network mock session has ended" }
        check(value = nextId < Long.MAX_VALUE) { "Network mock command identifiers exhausted" }
        val frozenCommand = if (command is NetworkMockMutation.Select) {
            val sequence = command.state as? OperationMockState.Sequence
            if (sequence == null) {
                command
            } else {
                command.copy(
                    state = sequence.copy(responses = sequence.responses.toList())
                )
            }
        } else {
            command
        }
        val ticket = MutationTicket(id = ++nextId, origin = origin, command = frozenCommand)
        queue.addLast(element = ticket)
        publish()
        check(
            value = wakeups.trySend(element = Unit).isSuccess
        ) { "Network mock session has ended" }
        return ticket
    }

    fun retryFailed() {
        val ticket = queue.firstOrNull() ?: return
        if (ticket.outcome.value !is MutationOutcome.Failed) return
        ticket.update(outcome = MutationOutcome.Queued)
        publish()
        wakeups.trySend(element = Unit)
    }

    fun cancelFailed() {
        val ticket = queue.firstOrNull() ?: return
        if (ticket.outcome.value !is MutationOutcome.Failed) return
        queue.removeFirst()
        ticket.update(outcome = MutationOutcome.Cancelled)
        publish()
        wakeups.trySend(element = Unit)
    }

    private fun publish() {
        val head = queue.firstOrNull()
        val state = NetworkMockMutationState(
            head = head,
            pendingCount = queue.size,
            failure = (head?.outcome?.value as? MutationOutcome.Failed)?.message
        )
        interactionsEnabled = !state.busy
        privateState.value = state
    }

    private suspend fun apply(command: NetworkMockMutation) {
        when (command) {
            is NetworkMockMutation.GlobalToggle ->
                stateRepository.setGlobalMockingEnabled(enabled = command.enabled)
            is NetworkMockMutation.Select ->
                stateRepository.setOperationMockState(key = command.key, state = command.state)
            is NetworkMockMutation.ResetSequence ->
                stateRepository.resetOperationSequencePosition(key = command.key)
            NetworkMockMutation.ResetAll -> stateRepository.resetKnownOperationsToNetwork()
        }
    }
}
