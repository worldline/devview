package com.worldline.devview.networkmock.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worldline.devview.networkmock.core.repository.MockStateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Retained by module registration, not by a sheet or a navigation entry. */
internal class NetworkMockSessionViewModel(stateRepository: MockStateRepository) : ViewModel() {
    val mutations = NetworkMockMutationCoordinator(
        stateRepository = stateRepository,
        scope = viewModelScope
    )
    var sheetTransitionInProgress: Boolean by mutableStateOf(value = false)
    val interactionsEnabled: Boolean get() = mutations.interactionsEnabled &&
        !sheetTransitionInProgress

    private val privateReloadGeneration = MutableStateFlow(value = 0L)
    val reloadGeneration: StateFlow<Long> = privateReloadGeneration.asStateFlow()

    fun requestReload() {
        if (!interactionsEnabled) return
        check(
            value = privateReloadGeneration.value < Long.MAX_VALUE
        ) { "Network mock reload identifiers exhausted" }
        privateReloadGeneration.value += 1
    }

    fun resetAll() {
        if (!interactionsEnabled) return
        mutations.submit(
            command = NetworkMockMutation.ResetAll,
            origin = MutationOrigin(entryId = 0)
        )
    }
}
