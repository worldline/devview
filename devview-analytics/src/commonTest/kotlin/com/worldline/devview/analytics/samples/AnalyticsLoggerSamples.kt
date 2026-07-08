package com.worldline.devview.analytics.samples

import com.worldline.devview.analytics.AnalyticsLogger
import com.worldline.devview.analytics.model.AnalyticsLog
import com.worldline.devview.analytics.model.AnalyticsLogCategory
import kotlin.time.Clock

class AnalyticsLoggerSamples {
    fun logSample() {
        AnalyticsLogger.log(
            AnalyticsLog(
                tag = "HomeScreen",
                screenClass = "com.example.ui.HomeScreen",
                timestamp = Clock.System.now().toEpochMilliseconds(),
                type = AnalyticsLogCategory.Screen.View
            )
        )
    }
}
