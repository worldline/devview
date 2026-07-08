package com.worldline.devview.analytics.components

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.worldline.devview.analytics.model.AnalyticsLog
import com.worldline.devview.analytics.preview.AnalyticsLogPreviewParameterProvider

/**
 * Individual log entry item for the analytics display.
 *
 * This composable renders a single analytics log event in a row format with a clear
 * visual hierarchy:
 * - **Tag** (most important): large bold text on the left.
 * - **Timestamp** (secondary): visible but de-emphasised, top-right.
 * - **Category chip** (tertiary): colour-coded pill with icon and type name, bottom-right.
 * - **Screen class**: muted subtitle below the tag.
 *
 * ## Layout Structure
 * ```
 * Row
 * ├── Column (Tag + Screen Class) - weighted to fill remaining space
 * └── Column (Timestamp + Category chip) - end-aligned
 * ```
 *
 * @param modifier Modifier to be applied to the root composable.
 * @param analyticsLog The analytics log data to display.
 *
 * @see AnalyticsLog
 * @see HighlightedAnalyticsLogsHeader
 * @see com.worldline.devview.analytics.AnalyticsScreen
 */
@Composable
internal fun AnalyticsLogItem(analyticsLog: AnalyticsLog, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(weight = 1f),
            verticalArrangement = Arrangement.spacedBy(space = 2.dp)
        ) {
            Text(
                modifier = Modifier.testTag(tag = "analytics_log_tag_${analyticsLog.tag}"),
                text = analyticsLog.tag,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                modifier = Modifier.testTag(
                    tag = "analytics_log_screen_class_${analyticsLog.screenClass}"
                ),
                text = analyticsLog.screenClass,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(space = 4.dp)
        ) {
            Text(
                modifier = Modifier.testTag(
                    tag = "analytics_log_timestamp_${analyticsLog.formattedTimestamp}"
                ),
                text = analyticsLog.formattedTimestamp,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            CategoryChip(
                modifier = Modifier.testTag(
                    tag = "analytics_log_category_chip_${analyticsLog.type.category}"
                ),
                category = analyticsLog.type.category
            )
        }
    }
}

@Preview(locale = "en")
@Composable
private fun AnalyticsLogItemPreview(
    @PreviewParameter(
        provider = AnalyticsLogPreviewParameterProvider::class
    ) analyticsLog: AnalyticsLog
) {
    MaterialTheme {
        Surface {
            AnalyticsLogItem(
                analyticsLog = analyticsLog
            )
        }
    }
}
