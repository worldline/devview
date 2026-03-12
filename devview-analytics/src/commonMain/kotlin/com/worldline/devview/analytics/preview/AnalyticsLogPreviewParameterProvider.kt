package com.worldline.devview.analytics.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.worldline.devview.analytics.model.AnalyticsLog
import com.worldline.devview.analytics.model.AnalyticsLogType
import kotlin.time.Clock

/**
 * Preview parameter provider for [AnalyticsLog] composable previews.
 *
 * Generates sample analytics log instances for each [AnalyticsLogType] to
 * facilitate UI previews during development. This allows developers to see
 * how individual log items will render for each event type.
 *
 * ## Usage
 * ```kotlin
 * @Preview
 * @Composable
 * fun MyPreview(
 *     @PreviewParameter(AnalyticsLogPreviewParameterProvider::class) log: AnalyticsLog
 * ) {
 *     AnalyticsLogItem(analyticsLog = log)
 * }
 * ```
 *
 * @see AnalyticsLog
 * @see AnalyticsLogType
 */
internal class AnalyticsLogPreviewParameterProvider : PreviewParameterProvider<AnalyticsLog> {
    /**
     * Provides a sequence of sample [AnalyticsLog] instances, one for each
     * [AnalyticsLogType] variant.
     */
    override val values: Sequence<AnalyticsLog>
        get() = AnalyticsLogType
            .allTypes()
            .map {
                AnalyticsLog(
                    tag = "Tag",
                    screenClass = "ScreenClass",
                    timestamp = Clock.System.now().toEpochMilliseconds(),
                    type = it
                )
            }.asSequence()

    override fun getDisplayName(index: Int): String =
        AnalyticsLogType.allTypes().get(index = index).displayName
}
