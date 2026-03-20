package com.worldline.devview.analytics

import com.worldline.devview.analytics.model.AnalyticsLog
import com.worldline.devview.analytics.model.AnalyticsLogCategory
import com.worldline.devview.analytics.model.AnalyticsLogType
import com.worldline.devview.core.Section
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.AfterTest
import kotlin.test.Test

class AnalyticsModuleTest {

    @AfterTest
    fun tearDown() {
        AnalyticsLogger.clear()
    }

    @Test
    fun `analytics module exposes expected metadata and destination action`() {
        val module = Analytics()

        module.section shouldBe Section.LOGGING
        module.destinations.keys.shouldContain(AnalyticsDestination.Main)

        val mainMetadata = module.destinations[AnalyticsDestination.Main].shouldNotBeNull()

        mainMetadata.title shouldBe "Analytics"
        mainMetadata.actions shouldHaveSize 1

        val clearAction = mainMetadata.actions.single()

        clearAction.popup.shouldNotBeNull().apply {
            title shouldBe "Clear Logs"
            confirmButton shouldBe "Clear"
            dismissButton shouldBe "Cancel"
        }
    }

    @Test
    fun `clear action removes existing logs`() {
        val module = Analytics()

        val clearAction = module.destinations[AnalyticsDestination.Main]
            .shouldNotBeNull()
            .actions
            .single()

        AnalyticsLogger.log(
            AnalyticsLog(
                tag = "tap",
                screenClass = "HomeScreen",
                timestamp = 1_700_000_000_000,
                type = AnalyticsLogCategory.Action.Click
            )
        )

        clearAction.action()

        AnalyticsLogger.hasLogs shouldBe false
    }

    @Test
    fun `analytics constructor keeps highlighted log types`() {
        val module = Analytics(
            highlightedLogType1 = AnalyticsLogCategory.Action.Scroll,
            highlightedLogType2 = AnalyticsLogCategory.Performance.Metrics,
            highlightedLogType3 = AnalyticsLogCategory.Social.Share
        )

        module.highlightedLogType1 shouldBe AnalyticsLogCategory.Action.Scroll
        module.highlightedLogType2 shouldBe AnalyticsLogCategory.Performance.Metrics
        module.highlightedLogType3 shouldBe AnalyticsLogCategory.Social.Share
    }

    @Test
    fun `analytics log types grouping is consistent`() {
        val allTypes = AnalyticsLogType.allTypes()

        val grouped = AnalyticsLogType.typesByCategory()

        grouped.values.flatten().toSet() shouldBe allTypes.toSet()
    }
}
