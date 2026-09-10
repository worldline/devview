package com.worldline.devview.timecapsule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.worldline.devview.timecapsule.components.TimeCapsuleRow

/**
 * Screen showing the state history of whichever screen is currently recording via
 * [TimeCapsuleEffect], newest entry first, with a restore action on each row.
 *
 * @param modifier Modifier to be applied to the root container.
 * @param bottomPadding Additional bottom padding applied to the list, used to avoid overlap
 *   with system UI when this screen is nested inside another `Scaffold`.
 *
 * @see TimeCapsule
 * @see TimeCapsuleEffect
 */
@Composable
public fun TimeCapsuleScreen(modifier: Modifier = Modifier, bottomPadding: Dp = 0.dp) {
    val capsule = TimeCapsule.current
    val entries = capsule?.entries.orEmpty().asReversed()
    var expandedIds by remember { mutableStateOf(value = emptySet<Long>()) }

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
                modifier = Modifier.weight(weight = 1f).fillMaxWidth().padding(all = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(space = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        modifier = Modifier.size(size = 48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (capsule == null) {
                            "No screen is recording. Add TimeCapsuleEffect(owner) to a screen."
                        } else {
                            "No states captured yet."
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
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
                    previousLabel = previous?.label,
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
