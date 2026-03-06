package com.worldline.devview.analytics.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.worldline.devview.analytics.model.AnalyticsLog
import com.worldline.devview.analytics.model.AnalyticsLogType
import kotlin.time.Clock

/**
 * Preview parameter provider for lists of [AnalyticsLog] in composable previews.
 *
 * Generates a sample list of analytics logs containing one entry for each
 * [AnalyticsLogType]. This is useful for previewing list-based UI components
 * like the analytics screen with representative data.
 *
 * ## Usage
 * ```kotlin
 * @Preview
 * @Composable
 * fun ScreenPreview(
 *     @PreviewParameter(AnalyticsLogListPreviewParameterProvider::class) logs: List<AnalyticsLog>
 * ) {
 *     CompositionLocalProvider(LocalAnalytics provides logs) {
 *         AnalyticsScreen()
 *     }
 * }
 * ```
 *
 * @see AnalyticsLog
 * @see AnalyticsLogType
 * @see com.worldline.devview.analytics.AnalyticsScreen
 */
internal class AnalyticsLogListPreviewParameterProvider :
    PreviewParameterProvider<List<AnalyticsLog>> {
    /**
     * Provides a sequence containing a single list of sample [AnalyticsLog] instances,
     * with one log entry for each [AnalyticsLogType] variant.
     */
    override val values: Sequence<List<AnalyticsLog>>
        get() = sequenceOf(
            element = AnalyticsLogType.entries.map {
                AnalyticsLog(
                    tag = "Tag",
                    screenClass = "ScreenClass",
                    timestamp = Clock.System.now().toEpochMilliseconds(),
                    type = it
                )
            }
        )
}
