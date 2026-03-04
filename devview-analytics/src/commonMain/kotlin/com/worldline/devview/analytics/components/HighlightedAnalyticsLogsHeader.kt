package com.worldline.devview.analytics.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.GridTrackSize
import androidx.compose.foundation.layout.columns
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.rows
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.worldline.devview.analytics.model.AnalyticsLogCategory
import com.worldline.devview.analytics.model.HighlightedAnalyticsLog
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

/**
 * Header component for the analytics screen that displays a summary grid of
 * highlighted analytics log counts.
 *
 * This composable renders a two-column grid of [HighlightedAnalyticsLogCard] items,
 * one card per entry in [highlightedAnalyticsLogs]. Each card shows either the total
 * number of recorded events ([HighlightedAnalyticsLog.Total]) or the count for a
 * specific log type ([HighlightedAnalyticsLog.Type]), along with a colour-coded icon
 * that reflects the log category.
 *
 * The grid uses fixed-height rows (64 dp) and equal-width columns (50 % each), with
 * a 16 dp gap between both rows and columns.
 *
 * This component is used as a header inside [com.worldline.devview.analytics.AnalyticsScreen]
 * to give users an at-a-glance overview before scrolling through the detailed log list below.
 *
 * @param highlightedAnalyticsLogs The list of highlighted log summaries to display.
 *        Typically contains one [HighlightedAnalyticsLog.Total] entry followed by one
 *        [HighlightedAnalyticsLog.Type] entry per tracked log type.
 * @param modifier Modifier to be applied to the root [Column].
 *
 * @see HighlightedAnalyticsLog
 * @see HighlightedAnalyticsLogCard
 * @see com.worldline.devview.analytics.AnalyticsScreen
 */
@Composable
internal fun HighlightedAnalyticsLogsHeader(
    highlightedAnalyticsLogs: PersistentList<HighlightedAnalyticsLog>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Grid(
            config = {
                columns(
                    GridTrackSize.Percentage(value = 0.5f),
                    GridTrackSize.Percentage(value = 0.5f)
                )
                rows(
                    GridTrackSize.Fixed(size = 64.dp),
                    GridTrackSize.Fixed(size = 64.dp)
                )
                rowGap(gap = 16.dp)
                columnGap(gap = 16.dp)
            }
        ) {
            highlightedAnalyticsLogs.forEach {
                HighlightedAnalyticsLogCard(
                    modifier = Modifier
                        .fillMaxSize(),
                    highlightedLog = it
                )
            }
        }
    }
}

@Preview(locale = "en")
@Composable
private fun HighlightedAnalyticsLogsHeaderPreview() {
    MaterialTheme {
        Surface {
            HighlightedAnalyticsLogsHeader(
                highlightedAnalyticsLogs = persistentListOf(
                    HighlightedAnalyticsLog.Total(
                        count = 100
                    ),
                    HighlightedAnalyticsLog.Type(
                        type = AnalyticsLogCategory.Action.Click,
                        count = 50
                    ),
                    HighlightedAnalyticsLog.Type(
                        type = AnalyticsLogCategory.Custom.Event,
                        count = 30
                    ),
                    HighlightedAnalyticsLog.Type(
                        type = AnalyticsLogCategory.Performance.Error,
                        count = 20
                    )
                )
            )
        }
    }
}
