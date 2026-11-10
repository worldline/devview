package com.worldline.devview.timecapsule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.worldline.devview.timecapsule.components.TimeCapsuleRow
import com.worldline.devview.ui.DevViewEmptyState

/**
 * Screen showing the state history of whichever screen is currently recording via
 * [TimeCapsuleEffect], newest entry first, with restore actions on earlier entries.
 *
 * @param modifier Modifier to be applied to the root container.
 * @param bottomPadding Additional bottom padding applied to the list, used to avoid overlap
 *   with system UI when this screen is nested inside another `Scaffold`.
 *
 * @see TimeCapsule
 * @see TimeCapsuleEffect
 */
@Composable
internal fun TimeCapsuleScreen(modifier: Modifier = Modifier, bottomPadding: Dp = 0.dp) {
    val capsule = TimeCapsule.current
    val entries = capsule?.entries.orEmpty().asReversed()
    val recordingId = capsule?.recordingId.orEmpty()
    val retainedIds = entries.map { it.id }.toSet()
    var expandedIds by rememberExpandedEntryIds(
        recordingId = recordingId,
        retainedIds = retainedIds
    )

    Column(modifier = modifier.fillMaxSize()) {
        capsule?.subtitle?.let { subtitle ->
            Text(
                text = "Recording $subtitle",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider()
        }

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(
                        weight = 1f
                    ).fillMaxWidth()
                    .padding(bottom = bottomPadding),
                contentAlignment = Alignment.Center
            ) {
                DevViewEmptyState(
                    icon = Icons.Rounded.History,
                    title = if (capsule ==
                        null
                    ) {
                        "No screen is recording"
                    } else {
                        "No states captured yet"
                    },
                    message = if (capsule == null) {
                        "Add TimeCapsuleEffect(owner) to a screen to record its state history."
                    } else {
                        "State changes will appear here while this screen is recording."
                    }
                )
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.weight(weight = 1f).fillMaxWidth(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = bottomPadding + 16.dp
            )
        ) {
            itemsIndexed(items = entries, key = { _, entry -> entry.id }) { index, entry ->
                val previous = entries.getOrNull(index = index + 1)
                TimeCapsuleRow(
                    entry = entry,
                    previous = previous,
                    deltaMillis = previous?.let { entry.atMillis - it.atMillis },
                    isCurrent = index == 0,
                    isLast = index == entries.lastIndex,
                    expanded = entry.id in expandedIds,
                    onExpandToggle = {
                        expandedIds = if (entry.id in expandedIds) {
                            expandedIds - entry.id
                        } else {
                            expandedIds + entry.id
                        }
                    },
                    onRestoreClick = { capsule?.restore(id = entry.id) },
                    // Without this, an item's own expand/collapse resize snaps the items below it
                    // straight to their new position instead of sliding — animateItem() is what
                    // lets the rail (its height following this row's) resize in step with siblings
                    // reflowing, rather than the two visibly disagreeing mid-animation.
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@Composable
internal fun rememberExpandedEntryIds(
    recordingId: String,
    retainedIds: Set<Long>
): MutableState<List<Long>> {
    val expanded = rememberSaveable(
        recordingId,
        stateSaver = listSaver<List<Long>, Any>(
            save = { listOf(element = recordingId) + it.filter { id -> id in retainedIds } },
            // Saved inputs are not validated when a new composition restores its state.
            restore = { saved ->
                if (saved.firstOrNull() == recordingId) {
                    saved.drop(n = 1).map { it as Long }.filter { it in retainedIds }
                } else {
                    emptyList()
                }
            }
        )
    ) { mutableStateOf(value = emptyList<Long>()) }
    LaunchedEffect(key1 = retainedIds) {
        expanded.value = expanded.value.filter { it in retainedIds }
    }
    return expanded
}
