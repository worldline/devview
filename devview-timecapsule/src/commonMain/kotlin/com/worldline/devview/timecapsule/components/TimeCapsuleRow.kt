package com.worldline.devview.timecapsule.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.worldline.devview.timecapsule.Recorded
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

/**
 * Single entry in the [com.worldline.devview.timecapsule.TimeCapsuleScreen] timeline.
 *
 * @param deltaMillis Time elapsed since the previous entry, or `null` for the oldest entry.
 */
@Composable
internal fun TimeCapsuleRow(
    entry: Recorded<*>,
    deltaMillis: Long?,
    onRestoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(weight = 1f),
                verticalArrangement = Arrangement.spacedBy(space = 4.dp)
            ) {
                Text(
                    text = entry.label,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = deltaMillis?.let { "+${formatDelta(millis = it)}" } ?: "Initial state",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(onClick = onRestoreClick) {
                Text(text = "Restore")
            }
        }
    }
}

private fun formatDelta(millis: Long): String {
    val duration = millis.milliseconds
    return if (millis < 1000) {
        duration.toString(unit = DurationUnit.MILLISECONDS)
    } else {
        duration.toString(unit = DurationUnit.SECONDS, decimals = 1)
    }
}
