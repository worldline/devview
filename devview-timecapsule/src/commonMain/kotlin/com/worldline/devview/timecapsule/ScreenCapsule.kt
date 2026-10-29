package com.worldline.devview.timecapsule

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshotFlow
import kotlin.reflect.KProperty1
import kotlin.time.Clock
import kotlinx.coroutines.flow.collectLatest

/** A single recorded state, labelled and timestamped at the moment it was captured. */
internal data class Recorded<out S : Any>(
    val id: Long,
    val atMillis: Long,
    val label: String,
    val state: S,
    val textFields: List<TextFieldSnapshot> = emptyList()
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
    private val maxEntries: Int,
    subtitleOverride: String? = null,
    private val textFields: List<KProperty1<S, TextFieldState>> = emptyList()
) {
    init {
        require(value = textFields.map { it.name }.distinct().size == textFields.size) {
            "TimeCapsule text field property names must be unique."
        }
    }

    private val recordedEntries = mutableStateListOf<Recorded<S>>()
    private var nextId = 0L

    val entries: List<Recorded<S>> get() = recordedEntries

    /**
     * Identifies which screen is recording, shown as a header above the timeline.
     *
     * ponytail: R8 would obfuscate this in a minified build. DevView is a debug-only overlay
     * so that combination shouldn't arise; integrators who hit it pass `subtitleOverride`
     * explicitly.
     */
    val subtitle: String? = subtitleOverride ?: owner::class.simpleName

    suspend fun collectStates() {
        owner.state.collectLatest { state ->
            if (textFields.isEmpty()) {
                record(state = state)
            } else {
                snapshotFlow { capture(state = state) }.collect { captured ->
                    append(captured = captured)
                }
            }
        }
    }

    fun record(state: S) {
        append(captured = capture(state = state))
    }

    private fun capture(state: S): CapturedState<S> = CapturedState(
        state = state,
        label = label(state),
        textFields = textFields.map { property ->
            val field = property.get(receiver = state)
            TextFieldSnapshot(
                name = property.name,
                text = field.text.toString(),
                selection = field.selection
            )
        }
    )

    private fun append(captured: CapturedState<S>) {
        if (recordedEntries.size == maxEntries) {
            recordedEntries.removeAt(index = 0)
        }
        recordedEntries += Recorded(
            id = nextId++,
            atMillis = Clock.System.now().toEpochMilliseconds(),
            label = captured.label,
            state = captured.state,
            textFields = captured.textFields
        )
    }

    // ponytail: restoring re-enters `owner.state`, which records the restored value as a
    // new entry. Truthful (a restore is a state transition) but repeated restores grow the
    // timeline. Suppressing this needs a race-prone flag or identity tracking; not worth it.
    fun restore(id: Long) {
        val entry = recordedEntries.firstOrNull { it.id == id } ?: return
        val savedFields = entry.textFields.associateBy { it.name }
        textFields.forEach { property ->
            val saved = savedFields.getValue(key = property.name)
            property.get(receiver = entry.state).edit {
                replace(start = 0, end = length, text = saved.text)
                selection = saved.selection
            }
        }
        owner.restoreState(state = entry.state)
    }

    private data class CapturedState<S : Any>(
        val state: S,
        val label: String,
        val textFields: List<TextFieldSnapshot>
    )

    fun clear() {
        recordedEntries.clear()
    }
}
