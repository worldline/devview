package com.worldline.devview.analytics

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.worldline.devview.analytics.model.AnalyticsLog
import com.worldline.devview.analytics.model.AnalyticsLogCategory
import com.worldline.devview.analytics.model.AnalyticsLogType
import kotlin.time.Clock
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test

class AnalyticsScreenTest {

    private fun ComposeUiTest.waitUntilDisplayed(tag: String) {
        waitUntil(timeoutMillis = 5_000L) {
            runCatching {
                onNodeWithTag(testTag = tag).assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }

    @Test
    fun analyticsScreen_shows_empty_state_when_no_logs() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalAnalytics provides emptyList()) {
                AnalyticsScreen(highlightedAnalyticsLogTypes = persistentListOf())
            }
        }

        waitUntilDisplayed(tag = "empty_state_message")
    }

    @Test
    fun analyticsScreen_filters_by_query_and_clear_restores_results() = runComposeUiTest {
        val logs = listOf(
            log(tag = "login_click", type = AnalyticsLogCategory.Action.Click),
            log(tag = "screen_view", type = AnalyticsLogCategory.Screen.View)
        )

        setContent {
            CompositionLocalProvider(LocalAnalytics provides logs) {
                AnalyticsScreen(highlightedAnalyticsLogTypes = persistentListOf())
            }
        }

        waitUntilDisplayed(tag = "analytics_log_item_login_click")
        waitUntilDisplayed(tag = "analytics_log_item_screen_view")

        onNodeWithTag(testTag = "analytics_filter_field").performTextInput("login")

        waitUntilDisplayed(tag = "analytics_log_item_login_click")
        onAllNodesWithTag(testTag = "analytics_log_item_screen_view").assertCountEquals(0)

        onNodeWithContentDescription(label = "Clear filter").performClick()

        waitUntilDisplayed(tag = "analytics_log_item_screen_view")
    }

    @Test
    fun analyticsScreen_can_filter_by_category_chip() = runComposeUiTest {
        val logs = listOf(
            log(tag = "click_tag", type = AnalyticsLogCategory.Action.Click),
            log(tag = "view_tag", type = AnalyticsLogCategory.Screen.View)
        )

        setContent {
            CompositionLocalProvider(LocalAnalytics provides logs) {
                AnalyticsScreen(highlightedAnalyticsLogTypes = persistentListOf())
            }
        }

        onNodeWithContentDescription(label = "Expand filter").performClick()
        onNodeWithTag(testTag = "category_chip_${AnalyticsLogCategory.Action.Click.category.displayName}").performClick()

        waitUntilDisplayed(tag = "analytics_log_item_click_tag")
        onAllNodesWithTag(testTag = "analytics_log_item_view_tag").assertCountEquals(0)
    }

    @Test
    fun analyticsScreen_time_range_filters_out_old_logs() = runComposeUiTest {
        val now = Clock.System.now().toEpochMilliseconds()
        val logs = listOf(
            log(
                tag = "recent_log",
                type = AnalyticsLogCategory.Action.Click,
                timestamp = now - 60_000L
            ),
            log(
                tag = "old_log",
                type = AnalyticsLogCategory.Action.Click,
                timestamp = now - (20 * 60 * 1000L)
            )
        )

        setContent {
            CompositionLocalProvider(LocalAnalytics provides logs) {
                AnalyticsScreen(highlightedAnalyticsLogTypes = persistentListOf())
            }
        }

        onNodeWithContentDescription(label = "Expand filter").performClick()
        onNodeWithTag(testTag = "time_range_5m").performClick()

        waitUntilDisplayed(tag = "analytics_log_item_recent_log")
        onAllNodesWithTag(testTag = "analytics_log_item_old_log").assertCountEquals(0)
    }

    @Test
    fun analyticsScreen_shows_no_match_message_for_non_matching_query() = runComposeUiTest {
        val logs = listOf(
            log(tag = "login_click", type = AnalyticsLogCategory.Action.Click)
        )

        setContent {
            CompositionLocalProvider(LocalAnalytics provides logs) {
                AnalyticsScreen(highlightedAnalyticsLogTypes = persistentListOf())
            }
        }

        onNodeWithTag(testTag = "analytics_filter_field").performTextInput("does-not-match")

        waitUntilDisplayed(tag = "empty_state_message")
        onAllNodesWithTag(testTag = "analytics_log_item_login_click").assertCountEquals(0)
    }

    private fun log(
        tag: String,
        type: AnalyticsLogType,
        timestamp: Long = 1_700_000_000_000
    ): AnalyticsLog =
        AnalyticsLog(
            tag = tag,
            screenClass = "TestScreen",
            timestamp = timestamp,
            type = type
        )
}

