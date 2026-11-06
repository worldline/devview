package com.worldline.devview.analytics

import com.worldline.devview.analytics.model.AnalyticsLog
import com.worldline.devview.analytics.model.AnalyticsLogCategory
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class AnalyticsFiltersTest {
    private val now = 1_700_000_000_000L
    private val click = AnalyticsLog(tag = "Login", screenClass = "Home", timestamp = now, type = AnalyticsLogCategory.Action.Click)
    private val view = AnalyticsLog(tag = "view", screenClass = "Login", timestamp = now - 300_000, type = AnalyticsLogCategory.Screen.View)

    @Test
    fun query_matches_tag_or_screen_and_categories_combine_with_time() {
        filterLogs(logs = listOf(click, view), query = "LOGIN", categories = emptySet(), range = TimeRange.All, now = now) shouldBe listOf(click, view)
        filterLogs(logs = listOf(click, view), query = "login", categories = setOf(click.type.category.displayName), range = TimeRange.Last5Min, now = now) shouldBe listOf(click)
        filterLogs(logs = listOf(click, view), query = "absent", categories = emptySet(), range = TimeRange.All, now = now) shouldBe emptyList()
    }

    @Test
    fun time_window_includes_boundary_and_expires_when_now_advances() {
        filterLogs(logs = listOf(view), query = "", categories = emptySet(), range = TimeRange.Last5Min, now = now) shouldBe listOf(view)
        filterLogs(logs = listOf(view), query = "", categories = emptySet(), range = TimeRange.Last5Min, now = now + 1) shouldBe emptyList()
        filterLogs(logs = listOf(view), query = "", categories = emptySet(), range = TimeRange.All, now = now + 1_800_000) shouldBe listOf(view)
    }

    @Test
    fun row_keys_are_unique_for_repeated_instances_and_stable_for_other_events() {
        val keys = eventRowKeys(logs = listOf(click, click, view))
        keys.distinct().size shouldBe 3
        eventRowKeys(logs = listOf(view)).single() shouldBe keys.last()
        eventRowKeys(logs = listOf(click.copy(), click.copy())).distinct().size shouldBe 2
    }
}
