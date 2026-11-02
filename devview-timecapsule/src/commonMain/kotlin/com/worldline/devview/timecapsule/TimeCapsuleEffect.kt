package com.worldline.devview.timecapsule

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlin.reflect.KProperty1

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
 * @param subtitle Identifies the recorded screen in the Time Capsule header (e.g. `"Recording
 *   CounterViewModel"`). Defaults to `owner`'s class name, which is `null` for an anonymous
 *   class — pass this explicitly in that case.
 * @param maxEntries Maximum number of retained entries; the oldest is dropped once reached.
 *
 * @see TimeCapsuleOwner
 * @see TimeCapsule
 */
@Composable
public fun <S : Any> TimeCapsuleEffect(
    owner: TimeCapsuleOwner<S>,
    label: (S) -> String = { it.toString() },
    subtitle: String? = null,
    maxEntries: Int = TimeCapsule.DEFAULT_MAX_ENTRIES
) {
    TimeCapsuleEffect(
        owner = owner,
        textFields = emptyList(),
        label = label,
        subtitle = subtitle,
        maxEntries = maxEntries
    )
}

/**
 * Records screen state and the text and selection of explicitly declared [TextFieldState]
 * properties, including edits that do not emit a new [TimeCapsuleOwner.state].
 *
 * ```kotlin
 * TimeCapsuleEffect(
 *     owner = viewModel,
 *     textFields = listOf(FormState::name, FormState::description)
 * )
 * ```
 *
 * Each entry captures detached text and selection values. Restore edits the fields belonging
 * to the saved screen state before passing that state to [TimeCapsuleOwner.restoreState].
 * Other mutable objects in the screen state are not copied. Keyboard composition, focus,
 * and undo history are not restored. Snapshot observation may coalesce rapid edits; this
 * records state history, not every input event.
 *
 * Recording ends when this effect leaves composition. Changing [owner] or [textFields]
 * starts a new timeline. Restore and field editing must run on the UI thread.
 *
 * @param owner The screen's state holder, typically a `ViewModel`.
 * @param textFields Properties of the screen state to observe and restore. Names must be unique.
 * @param label A pure, read-only function producing the screen label. Captured text fields
 *   are displayed separately using their property names.
 * @param subtitle Screen name shown in the header; defaults to the owner's class name.
 * @param maxEntries Maximum number of retained entries.
 *
 * @see TimeCapsuleEffect
 * @see TimeCapsuleOwner
 */
@Composable
public fun <S : Any> TimeCapsuleEffect(
    owner: TimeCapsuleOwner<S>,
    textFields: List<KProperty1<S, TextFieldState>>,
    label: (S) -> String = { it.toString() },
    subtitle: String? = null,
    maxEntries: Int = TimeCapsule.DEFAULT_MAX_ENTRIES
) {
    val fields = textFields.toList()
    val capsule = remember(key1 = owner, key2 = fields) {
        ScreenCapsule(
            owner = owner,
            label = label,
            maxEntries = maxEntries,
            subtitleOverride = subtitle,
            textFields = fields
        )
    }

    DisposableEffect(key1 = capsule) {
        TimeCapsule.register(capsule = capsule)
        onDispose { TimeCapsule.unregister(capsule = capsule) }
    }

    LaunchedEffect(key1 = capsule) {
        capsule.collectStates()
    }
}
