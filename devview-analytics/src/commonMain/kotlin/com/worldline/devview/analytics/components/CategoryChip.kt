package com.worldline.devview.analytics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.worldline.devview.analytics.model.AnalyticsLogCategory

/**
 * A compact, colour-coded chip component that visually represents an analytics log category.
 *
 * This composable renders a small pill-shaped UI element containing:
 * - An icon representing the log category (e.g., click, navigation, error).
 * - The display name of the category as text.
 *
 * The background and content colours are derived from the provided [AnalyticsLogCategory],
 * ensuring consistent visual theming across the analytics UI. The chip is designed to be
 * used within log entries and summary cards to quickly convey the type of event being displayed.
 *
 * @param category The [AnalyticsLogCategory] whose properties determine the chip's appearance and content.
 * @param modifier Modifier to be applied to the root composable for layout adjustments.
 *
 * @see AnalyticsLogCategory
 */
@Composable
internal fun CategoryChip(category: AnalyticsLogCategory, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(shape = MaterialTheme.shapes.extraSmall)
            .background(color = category.containerColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = null,
            modifier = Modifier.size(size = 12.dp),
            tint = category.contentColor
        )
        Text(
            text = category.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = category.contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(locale = "en")
@Composable
private fun CategoryChipPreview() {
    MaterialTheme {
        Surface {
            CategoryChip(
                category = AnalyticsLogCategory.Action.Click
            )
        }
    }
}
