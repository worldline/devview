package com.worldline.devview.analytics.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.worldline.devview.analytics.model.HighlightedAnalyticsLog
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

/** Non-interactive counts, kept compact so events remain the main content. */
@Composable
internal fun HighlightedAnalyticsLogsHeader(
    highlightedAnalyticsLogs: PersistentList<HighlightedAnalyticsLog>,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(items = highlightedAnalyticsLogs) { summary ->
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                val label = when (summary) {
                    is HighlightedAnalyticsLog.Total -> "total"
                    is HighlightedAnalyticsLog.Type -> summary.type.displayName.lowercase()
                }
                Text(
                    text = "${summary.count} $label",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Preview
@Composable
private fun HighlightedAnalyticsLogsHeaderPreview() {
    MaterialTheme {
        HighlightedAnalyticsLogsHeader(
            highlightedAnalyticsLogs = persistentListOf(HighlightedAnalyticsLog.Total(count = 34))
        )
    }
}
