package com.worldline.devview.timecapsule

import androidx.compose.runtime.mutableStateListOf
import kotlin.time.Clock

/** A single recorded state, labelled and timestamped at the moment it was captured. */
internal data class Recorded<out S : Any>(
    val id: Long,
    val atMillis: Long,
    val label: String,
    val state: S
)

/**
 * Records the state history of a single screen and replays entries back into it.
 *
 * Lives only as long as the screen's [TimeCapsuleEffect] composition — created on first
 * composition, discarded on dispose. This is what makes the timeline reset when the host
 * app navigates away.
 */
internal class ScreenCapsule<S : Any>(
    private val owner: TimeCapsuleOwner<S>,
    private val label: (S) -> String,
    private val maxEntries: Int
) {
    private val recordedEntries = mutableStateListOf<Recorded<S>>()
    private var nextId = 0L

    val entries: List<Recorded<S>> get() = recordedEntries

    fun record(state: S) {
        if (recordedEntries.size == maxEntries) {
            recordedEntries.removeAt(index = 0)
        }
        recordedEntries += Recorded(
            id = nextId++,
            atMillis = Clock.System.now().toEpochMilliseconds(),
            label = label(state),
            state = state
        )
    }

    // ponytail: restoring re-enters `owner.state`, which records the restored value as a
    // new entry. Truthful (a restore is a state transition) but repeated restores grow the
    // timeline. Suppressing this needs a race-prone flag or identity tracking; not worth it.
    fun restore(id: Long) {
        val entry = recordedEntries.firstOrNull { it.id == id } ?: return
        owner.restoreState(state = entry.state)
    }

    fun clear() {
        recordedEntries.clear()
    }
}
