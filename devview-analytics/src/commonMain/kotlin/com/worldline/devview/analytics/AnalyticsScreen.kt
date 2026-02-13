package com.worldline.devview.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.worldline.devview.analytics.components.AnalyticsLogHeader
import com.worldline.devview.analytics.components.AnalyticsLogItem
import com.worldline.devview.analytics.model.AnalyticsLog
import com.worldline.devview.analytics.preview.AnalyticsLogListPreviewParameterProvider

/**
 * Main UI component for displaying analytics logs in a tabular format.
 *
 * This composable renders a scrollable list of analytics events with a sticky header,
 * displaying event types, tags, screen classes, and timestamps. The UI automatically
 * adjusts column widths to ensure proper alignment across all log entries.
 *
 * ## Features
 * - Sticky header with column labels
 * - Auto-sizing columns for consistent alignment
 * - Formatted timestamps (HH:mm:ss)
 * - Event type indicators
 * - Horizontal dividers between entries
 * - Responsive layout
 *
 * ## Usage
 *
 * ### Basic Usage
 * ```kotlin
 * CompositionLocalProvider(LocalAnalytics provides AnalyticsLogger.logs) {
 *     AnalyticsScreen(
 *         modifier = Modifier.fillMaxSize()
 *     )
 * }
 * ```
 *
 * ### With Custom Logs
 * ```kotlin
 * val customLogs = remember { listOf(
 *     AnalyticsLog(
 *         tag = "user_login",
 *         screenClass = "LoginScreen",
 *         timestamp = System.currentTimeMillis(),
 *         type = AnalyticsLogType.EVENT
 *     )
 * )}
 *
 * CompositionLocalProvider(LocalAnalytics provides customLogs) {
 *     AnalyticsScreen()
 * }
 * ```
 *
 * @param modifier Modifier to be applied to the root composable.
 *
 * @throws IllegalStateException if [LocalAnalytics] is not provided in the composition.
 *
 * @see AnalyticsLogger
 * @see LocalAnalytics
 * @see AnalyticsLog
 */
@Composable
public fun AnalyticsScreen(modifier: Modifier = Modifier) {
    val analytics = LocalAnalytics.current

    var logTypeWidth by remember { mutableIntStateOf(value = 0) }

    val spacing = 8.dp

    LazyColumn(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(space = spacing)
    ) {
        stickyHeader {
            AnalyticsLogHeader(
                modifier = Modifier
                    .padding(horizontal = 16.dp),
                typeWidth = logTypeWidth
            )
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
        itemsIndexed(
            items = analytics
        ) { index, log ->
            AnalyticsLogItem(
                modifier = Modifier
                    .padding(horizontal = 16.dp),
                analyticsLog = log,
                onWidthCalculated = { width ->
                    logTypeWidth = maxOf(a = width, b = logTypeWidth)
                    logTypeWidth
                }
            )
            if (index != analytics.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = spacing)
                )
            }
        }
    }
}

@Preview
@Composable
private fun AnalyticsLogScreenPreview(
    @PreviewParameter(
        AnalyticsLogListPreviewParameterProvider::class
    ) analyticsLogs: List<AnalyticsLog>
) {
    MaterialTheme {
        Scaffold {
            CompositionLocalProvider(value = LocalAnalytics provides analyticsLogs) {
                AnalyticsScreen(
                    modifier = Modifier
                        .padding(paddingValues = it)
                )
            }
        }
    }
}
