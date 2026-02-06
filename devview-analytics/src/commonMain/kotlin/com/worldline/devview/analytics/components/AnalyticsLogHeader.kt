package com.worldline.devview.analytics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Header component for the analytics log table.
 *
 * This composable displays the column headers for the analytics log display,
 * including "Type", "Tag/Screen class", and "Time". It uses a custom layout
 * to ensure the "Type" column width matches the widest entry in the list for
 * proper alignment.
 *
 * This component is typically used as a sticky header in the analytics screen
 * to provide context for the log entries below.
 *
 * @param modifier Modifier to be applied to the root composable.
 * @param typeWidth Optional fixed width in pixels for the "Type" column. When provided,
 *        ensures alignment with the log items below. If null, uses natural width.
 *
 * @see AnalyticsLogItem
 * @see com.worldline.devview.analytics.AnalyticsScreen
 */
@Composable
internal fun AnalyticsLogHeader(
    modifier: Modifier = Modifier,
    typeWidth: Int? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 8.dp)
            .then(modifier),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val width = typeWidth ?: placeable.width
                layout(width, placeable.height) {
                    placeable.place(0, 0)
                }
            },
            text = "Type",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            )
        )

        Column(
            modifier = Modifier
                .weight(1f),
        ) {
            Text(
                text = "Tag",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "Screen class",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Text(
            text = "Time",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Preview
@Composable
private fun AnalyticsLogHeaderPreview() {
    MaterialTheme {
        Surface {
            AnalyticsLogHeader()
        }
    }
}
