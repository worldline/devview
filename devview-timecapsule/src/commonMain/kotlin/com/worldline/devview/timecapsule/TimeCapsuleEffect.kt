package com.worldline.devview.timecapsule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

/**
 * Records [owner]'s state history for as long as this composable stays in composition, and
 * exposes it to the [TimeCapsule] module for inspection and restore.
 *
 * Call this once per screen, inside the screen's top-level composable. The recorded history
 * is scoped to this composition: navigating away from the screen disposes it, and the
 * timeline starts empty the next time the screen is composed.
 *
 * Restoring a state is the integrator's risk — DevView does not guarantee the host app keeps
 * working correctly after [TimeCapsuleOwner.restoreState] is invoked out of band.
 *
 * ## Usage
 * ```kotlin
 * @Composable
 * fun CounterScreen(viewModel: CounterViewModel) {
 *     TimeCapsuleEffect(owner = viewModel)
 *     // ...screen content
 * }
 * ```
 *
 * @param owner The screen's state holder, typically a `ViewModel`.
 * @param label Produces the one-line description shown for each recorded entry. Defaults to
 *   `state.toString()`.
 * @param maxEntries Maximum number of retained entries; the oldest is dropped once reached.
 *
 * @see TimeCapsuleOwner
 * @see TimeCapsule
 */
@Composable
public fun <S : Any> TimeCapsuleEffect(
    owner: TimeCapsuleOwner<S>,
    label: (S) -> String = { it.toString() },
    maxEntries: Int = TimeCapsule.DEFAULT_MAX_ENTRIES
) {
    val capsule = remember(key1 = owner) {
        ScreenCapsule(owner = owner, label = label, maxEntries = maxEntries)
    }

    DisposableEffect(key1 = capsule) {
        TimeCapsule.register(capsule = capsule)
        onDispose { TimeCapsule.unregister(capsule = capsule) }
    }

    LaunchedEffect(key1 = capsule) {
        owner.state.collect { state -> capsule.record(state = state) }
    }
}
