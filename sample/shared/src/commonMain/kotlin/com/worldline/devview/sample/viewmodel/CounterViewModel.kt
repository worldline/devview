package com.worldline.devview.sample.viewmodel

import androidx.compose.foundation.text.input.TextFieldState
import com.worldline.devview.timecapsule.TimeCapsuleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * State recorded by the sample [CounterViewModel] and shown in the TimeCapsule timeline.
 *
 * @property count Current counter value.
 * @property label Description of the last counter action.
 * @property note Editable note captured independently of screen state emissions.
 */
public data class CounterState(
    val count: Int,
    val label: String,
    val note: TextFieldState = TextFieldState()
)

/** Sample state holder demonstrating [TimeCapsuleOwner] integration. */
public class CounterViewModel : TimeCapsuleOwner<CounterState> {
    private val _state = MutableStateFlow(value = CounterState(count = 0, label = "Boot"))
    override val state: StateFlow<CounterState> = _state.asStateFlow()

    override fun restoreState(state: CounterState) {
        _state.value = state
    }

    public fun increment() {
        _state.update { current ->
            current.copy(count = current.count + 1, label = "Count ${current.count + 1}")
        }
    }

    public fun decrement() {
        _state.update { current ->
            current.copy(count = current.count - 1, label = "Count ${current.count - 1}")
        }
    }
}
