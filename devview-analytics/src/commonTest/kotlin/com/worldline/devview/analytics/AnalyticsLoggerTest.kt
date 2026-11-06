package com.worldline.devview.analytics

import com.worldline.devview.analytics.model.AnalyticsLog
import com.worldline.devview.analytics.model.AnalyticsLogCategory
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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

    @Test
    fun repeated_log_calls_have_distinct_ids_without_changing_value_equality() {
        val event = testLog(tag = "repeat")
        AnalyticsLogger.log(log = event)
        AnalyticsLogger.log(log = event)
        val records = AnalyticsLogger.logs
        records[0] shouldBe records[1]
        records[0].id shouldNotBe records[1].id
        event.copy().id shouldNotBe event.id
    }

    @Test
    fun previously_returned_history_and_recorded_parameters_are_immutable_snapshots() {
        val params = mutableMapOf("screen" to "home")
        AnalyticsLogger.log(log = testLog(tag = "first").copy(params = params))
        val snapshot = AnalyticsLogger.logs
        params["screen"] = "changed"
        AnalyticsLogger.log(log = testLog(tag = "second"))
        snapshot shouldHaveSize 1
        snapshot.single().params shouldBe mapOf("screen" to "home")
        AnalyticsLogger.clear()
        snapshot shouldHaveSize 1
        AnalyticsLogger.logs shouldHaveSize 0
    }

    @Test
    fun event_copy_text_has_complete_timestamp_category_and_sorted_parameters() {
        val event = testLog(tag = "tap").copy(params = mapOf("z" to "last", "a" to "first"))
        event.detailedTimestamp.contains(other = ".000 (") shouldBe true
        event.copyText shouldBe "tap\nTestScreen\n${event.detailedTimestamp}\n${event.type.displayName}\na: first\nz: last"
    }

    private fun testLog(tag: String): AnalyticsLog = AnalyticsLog(
        tag = tag,
        screenClass = "TestScreen",
        timestamp = 1_700_000_000_000,
        type = AnalyticsLogCategory.Action.Click
    )
}

