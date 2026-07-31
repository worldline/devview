package com.worldline.devview.timecapsule

import kotlinx.coroutines.flow.StateFlow

/**
 * Contract implemented by a screen's state holder (typically a `ViewModel`) so its state
 * history can be recorded and restored by the [TimeCapsule] module.
 *
 * ## Usage
 * ```kotlin
 * class CounterViewModel : ViewModel(), TimeCapsuleOwner<CounterState> {
 *     private val _state = MutableStateFlow(CounterState())
 *     override val state: StateFlow<CounterState> = _state.asStateFlow()
 *
 *     override fun restoreState(state: CounterState) {
 *         _state.value = state
 *     }
 * }
 * ```
 *
 * Restoring state is the integrator's responsibility to use safely: DevView does not
 * guarantee the host app keeps working correctly after a state is pushed back into a
 * running screen out of band.
 *
 * @see TimeCapsuleEffect
 */
public interface TimeCapsuleOwner<S : Any> {
    /** The screen's current state, observed to build the recorded timeline. */
    public val state: StateFlow<S>

    /** Pushes [state] back into the screen, restoring it as the current state. */
    public fun restoreState(state: S)
}
