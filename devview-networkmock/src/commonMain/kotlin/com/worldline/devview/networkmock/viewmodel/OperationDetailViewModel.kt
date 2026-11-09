package com.worldline.devview.networkmock.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worldline.devview.networkmock.core.model.MockResponse
import com.worldline.devview.networkmock.core.model.OperationKey
import com.worldline.devview.networkmock.core.model.OperationMockState
import com.worldline.devview.networkmock.core.repository.MockConfigRepository
import com.worldline.devview.networkmock.core.repository.MockStateRepository
import com.worldline.devview.networkmock.model.OperationUiModel
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
internal data class SequenceDraftStep(val id: String, val response: OperationMockState.Mock)

// Discovery, a retained draft and its commit acknowledgement share one entry lifetime.
@Suppress("TooManyFunctions")
internal class OperationDetailViewModel(
    private val operation: OperationKey,
    private val configRepository: MockConfigRepository,
    private val stateRepository: MockStateRepository,
    private val savedStateHandle: SavedStateHandle,
    internal val mutations: NetworkMockMutationCoordinator
) : ViewModel() {
    private val loader = OperationDetailsLoader(configRepository = configRepository)
    private val entryId = mutations.createEntryId()
    private var loadJob: Job? = null
    private var loadGeneration = 0L
    private val loaded = MutableStateFlow<LoadedOperation?>(value = null)
    private val error = MutableStateFlow<String?>(value = null)
    private val privateSelection = MutableStateFlow<MutationTicket?>(value = null)
    val selection: StateFlow<MutationTicket?> = privateSelection.asStateFlow()
    private val privateDraft = MutableStateFlow(
        value = savedStateHandle
            .get<String>(key = DRAFT_KEY)
            ?.let {
                Json.decodeFromString<List<SequenceDraftStep>>(string = it)
            }.orEmpty()
    )
    val draft: StateFlow<List<SequenceDraftStep>> = privateDraft.asStateFlow()

    val state: StateFlow<OperationSheetState> = combine(
        flow = loaded,
        flow2 = error,
        flow3 = stateRepository.observeState()
    ) { details, failure, current ->
        when {
            failure != null -> OperationSheetState.Error(message = failure)
            details == null -> OperationSheetState.Loading
            else -> OperationSheetState.Content(
                operationUiModel = OperationUiModel(
                    descriptor = details.descriptor,
                    currentState = current.getOperationState(
                        key = operation
                    ) ?: OperationMockState.Network
                ),
                responses = details.responses
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = OperationSheetState.Loading
    )

    init {
        load()
    }

    fun reload() {
        if (!mutations.interactionsEnabled) return
        configRepository.invalidate()
        load()
    }

    @Suppress("TooGenericExceptionCaught")
    private fun load() {
        val generation = ++loadGeneration
        loadJob?.cancel()
        loaded.value = null
        error.value = null
        loadJob = viewModelScope.launch {
            try {
                val details = loader.load(key = operation)
                if (generation != loadGeneration) return@launch
                if (!savedStateHandle.contains(key = DRAFT_KEY)) {
                    val active = stateRepository.getState().getOperationState(key = operation)
                        as? OperationMockState.Sequence
                    if (generation != loadGeneration) return@launch
                    if (!savedStateHandle.contains(key = DRAFT_KEY)) {
                        updateDraft(
                            steps = active?.responses.orEmpty().map { newStep(response = it) }
                        )
                    }
                }
                loaded.value = details
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (generation ==
                    loadGeneration
                ) {
                    error.value =
                        failure.message ?: "Could not load this operation."
                }
            }
        }
    }

    fun apply(response: MockResponse) {
        val details = state.value as? OperationSheetState.Content ?: return
        if (details.responses.none { it.asState() == response.asState() }) return
        submit(command = NetworkMockMutation.Select(key = operation, state = response.asState()))
    }

    fun saveSequence() {
        val details = state.value as? OperationSheetState.Content ?: return
        val steps = privateDraft.value
        if (steps.size < 2 || steps.any { step ->
                details.responses.none { it.asState() == step.response }
            }
        ) {
            return
        }
        submit(
            command = NetworkMockMutation.Select(
                key = operation,
                state = OperationMockState.Sequence(responses = steps.map { it.response })
            )
        )
    }

    private fun submit(command: NetworkMockMutation) {
        if (!mutations.interactionsEnabled) return
        privateSelection.value = mutations.submit(
            command = command,
            origin = MutationOrigin(entryId = entryId)
        )
    }

    fun acknowledgeSelection(ticketId: Long): Boolean {
        val ticket = privateSelection.value ?: return false
        val matchesOrigin = ticket.id == ticketId && ticket.origin.entryId == entryId
        if (!mutations.interactionsEnabled || !matchesOrigin ||
            ticket.outcome.value != MutationOutcome.Succeeded
        ) {
            return false
        }
        privateSelection.value = null
        return true
    }

    fun add(response: MockResponse) {
        if (!canEdit()) return
        val details = state.value as OperationSheetState.Content
        if (details.responses.none { it.asState() == response.asState() }) return
        updateDraft(steps = privateDraft.value + newStep(response = response.asState()))
    }

    fun remove(id: String) {
        if (!canEdit()) return
        updateDraft(steps = privateDraft.value.filterNot { it.id == id })
    }

    fun move(id: String, offset: Int) {
        if (!canEdit() || offset !in setOf(-1, 1)) return
        val steps = privateDraft.value.toMutableList()
        val index = steps.indexOfFirst { it.id == id }
        val target = index + offset
        if (index < 0 || target !in steps.indices) return
        steps.add(index = target, element = steps.removeAt(index = index))
        updateDraft(steps = steps)
    }

    private fun canEdit(): Boolean {
        val details = state.value as? OperationSheetState.Content ?: return false
        return mutations.interactionsEnabled && privateDraft.value.all { step ->
            details.responses.any { it.asState() == step.response }
        }
    }

    private fun updateDraft(steps: List<SequenceDraftStep>) {
        savedStateHandle[DRAFT_KEY] = Json.encodeToString(value = steps)
        privateDraft.value = steps
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun newStep(response: OperationMockState.Mock): SequenceDraftStep =
        SequenceDraftStep(id = Uuid.random().toString(), response = response)

    private companion object {
        const val DRAFT_KEY = "networkmock_sequence_draft"
    }
}

internal fun MockResponse.asState(): OperationMockState.Mock = OperationMockState.Mock(
    statusCode = statusCode,
    mediaType = mediaType,
    exampleName = exampleName
)
