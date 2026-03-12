package com.worldline.devview.analytics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.worldline.devview.analytics.model.AnalyticsLogCategory
import com.worldline.devview.analytics.model.HighlightedAnalyticsLog

/**
 * A card that displays a single highlighted analytics log summary.
 *
 * This composable renders an [ElevatedCard] containing two pieces of information
 * side by side:
 * - A **label** (top-left): either `"Total logs"` for a [HighlightedAnalyticsLog.Total]
 *   entry, or the [AnalyticsLogType.displayName][com.worldline.devview.analytics.model.AnalyticsLogType.displayName]
 *   for a [HighlightedAnalyticsLog.Type] entry. Rendered in a small, medium-weight style
 *   using the outline colour.
 * - A **count** (below the label): the numeric value of [HighlightedAnalyticsLog.count],
 *   rendered in a large, semi-bold headline style.
 * - An **icon** (top-right): sourced from [HighlightedAnalyticsLog.icon], clipped to
 *   `MaterialTheme.shapes.extraSmall`, and tinted with the category's
 *   [HighlightedAnalyticsLog.contentColor] on a [HighlightedAnalyticsLog.containerColor]
 *   background.
 *
 * This card is intended to be placed inside [HighlightedAnalyticsLogsHeader], which
 * arranges multiple cards in a two-column grid as a sticky header for the analytics screen.
 *
 * @param highlightedLog The log summary to display. Either a [HighlightedAnalyticsLog.Total]
 *        for the overall event count, or a [HighlightedAnalyticsLog.Type] for the count of
 *        a specific log type.
 * @param modifier Modifier to be applied to the root [ElevatedCard].
 *
 * @see HighlightedAnalyticsLog
 * @see HighlightedAnalyticsLogsHeader
 */
@Composable
internal fun HighlightedAnalyticsLogCard(
    highlightedLog: HighlightedAnalyticsLog,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(),
        shape = MaterialTheme.shapes.small
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 8.dp,
                    horizontal = 12.dp
                )
        ) {
            Column {
                Text(
                    text = when (highlightedLog) {
                        is HighlightedAnalyticsLog.Total -> "Total logs"
                        is HighlightedAnalyticsLog.Type -> highlightedLog.type.displayName
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmallEmphasized.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "${highlightedLog.count}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.headlineSmallEmphasized.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
            Icon(
                modifier = Modifier
                    .clip(shape = MaterialTheme.shapes.extraSmall)
                    .background(color = highlightedLog.containerColor)
                    .padding(all = 2.dp)
                    .size(size = 18.dp)
                    .align(alignment = Alignment.TopEnd),
                imageVector = highlightedLog.icon,
                contentDescription = null,
                tint = highlightedLog.contentColor
            )
        }
    }
}

@Preview(locale = "en")
@Composable
internal fun HighlightedAnalyticsLogCardPreview() {
    MaterialTheme {
        Surface {
            HighlightedAnalyticsLogCard(
                highlightedLog = HighlightedAnalyticsLog.Type(
                    type = AnalyticsLogCategory.Session.Login,
                    count = 42
                )
            )
        }
    }
}
