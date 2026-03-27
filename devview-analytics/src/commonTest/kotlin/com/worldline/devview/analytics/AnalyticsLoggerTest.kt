package com.worldline.devview.analytics

import com.worldline.devview.analytics.model.AnalyticsLog
import com.worldline.devview.analytics.model.AnalyticsLogCategory
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.AfterTest
import kotlin.test.Test

class AnalyticsLoggerTest {

    @AfterTest
    fun tearDown() {
        AnalyticsLogger.clear()
    }

    @Test
    fun `log adds analytics entries and updates hasLogs`() {
        val first = testLog(tag = "first")
        val second = testLog(tag = "second")

        AnalyticsLogger.log(first)
        AnalyticsLogger.log(second)

        AnalyticsLogger.hasLogs shouldBe true
        AnalyticsLogger.logs shouldHaveSize 2
        AnalyticsLogger.logs.shouldContainExactly(first, second)
    }

    @Test
    fun `clear removes all logged entries`() {
        AnalyticsLogger.log(testLog(tag = "only"))

        AnalyticsLogger.clear()

        AnalyticsLogger.hasLogs shouldBe false
        AnalyticsLogger.logs shouldHaveSize 0
    }

    private fun testLog(tag: String): AnalyticsLog = AnalyticsLog(
        tag = tag,
        screenClass = "TestScreen",
        timestamp = 1_700_000_000_000,
        type = AnalyticsLogCategory.Action.Click
    )
}

