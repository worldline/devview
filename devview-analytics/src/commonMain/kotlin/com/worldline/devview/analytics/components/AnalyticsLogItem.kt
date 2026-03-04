package com.worldline.devview.analytics.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.worldline.devview.analytics.model.AnalyticsLog
import com.worldline.devview.analytics.preview.AnalyticsLogPreviewParameterProvider

/**
 * Individual log entry item for the analytics display.
 *
 * This composable renders a single analytics log event in a row format, displaying
 * the event type, tag, screen class, and formatted timestamp. It uses a custom
 * layout modifier to communicate its width to the parent for column alignment.
 *
 * ## Layout Structure
 * ```
 * Row
 * ├── Text (Event Type) - with width callback
 * ├── Column (Tag + Screen Class) - weighted to fill remaining space
 * └── Text (Timestamp)
 * ```
 *
 * @param modifier Modifier to be applied to the root composable.
 * @param analyticsLog The analytics log data to display.
 * @param onWidthCalculated Callback invoked during layout with the measured width
 *        of the event type text. Returns the width to use for alignment (usually
 *        the maximum width across all items). Returns null to use natural width.
 *
 * @see AnalyticsLog
 * @see AnalyticsLogHeader
 * @see com.worldline.devview.analytics.AnalyticsScreen
 */
@Composable
internal fun AnalyticsLogItem(
    analyticsLog: AnalyticsLog,
    onWidthCalculated: (Int) -> Int?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.layout { measurable, constraints ->
                val placeable = measurable.measure(constraints = constraints)
                val width = onWidthCalculated(placeable.width) ?: placeable.width

                layout(width = width, height = placeable.height) {
                    placeable.place(x = 0, y = 0)
                }
            },
            text = analyticsLog.type.name.capitalize(),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            )
        )

        Column(
            modifier = Modifier
                .weight(weight = 1f)
        ) {
            Text(
                text = analyticsLog.tag,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = analyticsLog.screenClass,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Text(
            text = analyticsLog.formattedTimestamp,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            )
        )
    }
}

/**
 * Capitalizes the first character of a string.
 *
 * This extension function is used to format event type names for display
 * (e.g., "screen" becomes "Screen").
 *
 * @return A new string with the first character capitalized.
 */
@Suppress("CommentOverPrivateFunction")
private fun String.capitalize() =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

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
                analyticsLog = analyticsLog,
                onWidthCalculated = { null }
            )
        }
    }
}
