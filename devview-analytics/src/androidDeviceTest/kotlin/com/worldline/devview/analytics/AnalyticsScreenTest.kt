package com.worldline.devview.analytics

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.ui.test.StateRestorationTester
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.worldline.devview.analytics.model.AnalyticsLog
import com.worldline.devview.analytics.model.AnalyticsLogCategory
import com.worldline.devview.analytics.model.AnalyticsLogType
import com.worldline.devview.test.waitUntilTagCount
import io.kotest.matchers.shouldBe
import kotlin.time.Clock
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test

class AnalyticsScreenTest {
    @Test
    fun empty_history_shows_recording_guidance() = runComposeUiTest {
        setContent { TestAnalyticsScreen(highlightedAnalyticsLogTypes = persistentListOf(), logs = emptyList()) }
        onNodeWithText(text = "No logs yet").assertIsDisplayed()
    }

    @Test
    fun search_and_clear_restore_results() = runComposeUiTest {
        val logs = listOf(log(tag = "login_click"), log(tag = "screen_view", type = AnalyticsLogCategory.Screen.View))
        setContent { TestAnalyticsScreen(highlightedAnalyticsLogTypes = persistentListOf(), logs = logs) }
        onNodeWithText(text = "Filter by tag or screen").performTextInput(text = "login")
        waitUntilTagCount(tag = "analytics_log_item_screen_view", expectedCount = 0)
        onNodeWithText(text = "Filter by tag or screen").performTextClearance()
        waitUntilTagCount(tag = "analytics_log_item_screen_view", expectedCount = 1)
    }

    @Test
    fun category_only_empty_state_is_recoverable_and_badge_survives_collapse() = runComposeUiTest {
        val logs = mutableStateOf(value = listOf(log(tag = "click"), log(tag = "view", type = AnalyticsLogCategory.Screen.View)))
        setContent { TestAnalyticsScreen(highlightedAnalyticsLogTypes = persistentListOf(), logs = logs.value) }
        onNodeWithContentDescription(label = "Show filters, 0 active").performClick()
        val tag = "category_chip_${AnalyticsLogCategory.Action.Click.category.displayName}"
        waitUntilTagCount(tag = tag, expectedCount = 1)
        onNodeWithTag(testTag = tag).performScrollTo().performClick()
        runOnIdle { logs.value = listOf(log(tag = "view", type = AnalyticsLogCategory.Screen.View)) }
        onNodeWithText(text = "No logs match your filters").assertIsDisplayed()
        onNodeWithContentDescription(label = "Hide filters, 1 active").performClick()
        onNodeWithContentDescription(label = "Show filters, 1 active").assertIsDisplayed()
        onNodeWithText(text = "Clear search and filters").performClick()
        waitUntilTagCount(tag = "analytics_log_item_view", expectedCount = 1)
    }

    @Test
    fun time_range_excludes_old_events() = runComposeUiTest {
        val now = Clock.System.now().toEpochMilliseconds()
        val logs = listOf(log(tag = "recent", timestamp = now - 60_000), log(tag = "old", timestamp = now - 1_200_000))
        setContent { TestAnalyticsScreen(highlightedAnalyticsLogTypes = persistentListOf(), logs = logs) }
        onNodeWithContentDescription(label = "Show filters, 0 active").performClick()
        onNodeWithTag(testTag = "time_range_5m").performScrollTo().performClick()
        waitUntilTagCount(tag = "analytics_log_item_recent", expectedCount = 1)
        waitUntilTagCount(tag = "analytics_log_item_old", expectedCount = 0)
    }

    @Test
    fun filters_survive_saved_state_restoration() = runComposeUiTest {
        val logs = listOf(log(tag = "login"), log(tag = "view", type = AnalyticsLogCategory.Screen.View))
        val restoration = StateRestorationTester(this)
        restoration.setContent { TestAnalyticsScreen(highlightedAnalyticsLogTypes = persistentListOf(), logs = logs) }
        onNodeWithContentDescription(label = "Show filters, 0 active").performClick()
        val tag = "category_chip_${AnalyticsLogCategory.Action.Click.category.displayName}"
        onNodeWithTag(testTag = tag).performScrollTo().performClick()
        onNodeWithTag(testTag = "time_range_5m").performScrollTo().performClick()
        onNodeWithText(text = "Filter by tag or screen").performTextInput(text = "login")
        restoration.emulateSaveAndRestore()
        onNodeWithContentDescription(label = "Hide filters, 2 active").assertIsDisplayed()
        onNodeWithTag(testTag = tag).assertIsSelected()
        waitUntilTagCount(tag = "analytics_log_item_view", expectedCount = 0)
        waitUntilTagCount(tag = "analytics_log_item_login", expectedCount = 1)
    }

    @Test
    fun repeated_object_rows_render_and_details_show_parameters() = runComposeUiTest {
        val event = log(tag = "repeat", params = mapOf("order" to "demo-42"))
        setContent { TestAnalyticsScreen(highlightedAnalyticsLogTypes = persistentListOf(), logs = listOf(event, event)) }
        waitUntilTagCount(tag = "analytics_log_item_repeat", expectedCount = 2)
        onAllNodesWithTag(testTag = "analytics_log_item_repeat")[1].performClick()
        onNodeWithText(text = "demo-42").assertIsDisplayed()
        val clipboard = InstrumentationRegistry.getInstrumentation().targetContext
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        onNodeWithText(text = "Copy event").assertIsDisplayed().performClick()
        waitUntil(timeoutMillis = 10_000) {
            clipboard.primaryClip?.getItemAt(0)?.text?.toString() == event.copyText
        }
        onNodeWithText(text = "Back to events").performClick()
        waitUntilTagCount(tag = "analytics_log_item_repeat", expectedCount = 2)
    }

    @Test
    fun missing_event_detail_has_an_empty_state() = runComposeUiTest {
        setContent { AnalyticsDetailScreen(log = null) }
        onNodeWithText(text = "Event no longer available").assertIsDisplayed()
        onNodeWithText(text = "Copy event").assertDoesNotExist()
    }

    @Test
    fun default_source_observes_new_logger_snapshots() = runComposeUiTest {
        AnalyticsLogger.clear()
        setContent { TestAnalyticsScreen(highlightedAnalyticsLogTypes = persistentListOf()) }
        onNodeWithText(text = "No logs yet").assertIsDisplayed()
        runOnIdle { AnalyticsLogger.log(log = log(tag = "live")) }
        waitUntilTagCount(tag = "analytics_log_item_live", expectedCount = 1)
        runOnIdle { AnalyticsLogger.clear() }
        onNodeWithText(text = "No logs yet").assertIsDisplayed()
    }

    @Test
    fun events_display_newest_first() = runComposeUiTest {
        val logs = listOf(log(tag = "older"), log(tag = "newest"))
        setContent { TestAnalyticsScreen(highlightedAnalyticsLogTypes = persistentListOf(), logs = logs) }
        val newest = onNodeWithTag(testTag = "analytics_log_item_newest")
        val older = onNodeWithTag(testTag = "analytics_log_item_older")
        newest.assertIsDisplayed()
        older.assertIsDisplayed()
        (newest.fetchSemanticsNode().positionInRoot.y < older.fetchSemanticsNode().positionInRoot.y) shouldBe true
    }

    @Test
    fun arrivals_preserve_row_and_offset_while_browsing_and_returning_from_details() = runComposeUiTest {
        val entries = mutableStateOf(value = (0..40).map { log(tag = "row$it") })
        setContent { TestAnalyticsScreen(highlightedAnalyticsLogTypes = persistentListOf(), logs = entries.value) }
        val list = onNodeWithTag(testTag = "analytics_event_list")
        list.performScrollToIndex(index = 21)
        list.performSemanticsAction(key = SemanticsActions.ScrollBy) { scroll -> scroll(0f, 23f) }
        val inspected = onNodeWithTag(testTag = "analytics_log_item_row20")
        val initialY = inspected.fetchSemanticsNode().positionInRoot.y
        runOnIdle { entries.value = entries.value + log(tag = "live-arrival") }
        inspected.fetchSemanticsNode().positionInRoot.y shouldBe initialY
        inspected.performClick()
        val repeated = log(tag = "repeated-arrival")
        runOnIdle { entries.value = entries.value + listOf(repeated, repeated) }
        onNodeWithText(text = "Back to events").performClick()
        inspected.fetchSemanticsNode().positionInRoot.y shouldBe initialY
        runOnIdle { entries.value = entries.value + log(tag = "another-arrival") }
        inspected.fetchSemanticsNode().positionInRoot.y shouldBe initialY
        onNodeWithContentDescription(label = "Latest events").performClick()
        onNodeWithTag(testTag = "analytics_log_item_another-arrival").assertIsDisplayed()
    }

    @Test
    fun returning_from_top_details_keeps_the_inspected_event_below_the_header() = runComposeUiTest {
        val entries = mutableStateOf(value = (0..40).map { log(tag = "row$it") })
        val restoration = StateRestorationTester(this)
        restoration.setContent { TestAnalyticsScreen(highlightedAnalyticsLogTypes = persistentListOf(), logs = entries.value) }
        val inspected = onNodeWithTag(testTag = "analytics_log_item_row39")
        inspected.assertIsDisplayed()
        val initialY = inspected.fetchSemanticsNode().positionInRoot.y
        inspected.performClick()
        runOnIdle { entries.value = entries.value + (41..50).map { log(tag = "row$it") } }
        restoration.emulateSaveAndRestore()
        onNodeWithText(text = "Back to events").performClick()
        inspected.assertIsDisplayed()
        inspected.fetchSemanticsNode().positionInRoot.y shouldBe initialY
        runOnIdle { entries.value = entries.value + log(tag = "latest") }
        inspected.fetchSemanticsNode().positionInRoot.y shouldBe initialY
        onNodeWithContentDescription(label = "Latest events").performClick()
        onNodeWithTag(testTag = "analytics_log_item_latest").assertIsDisplayed()
    }

    @Test
    fun row_anchor_and_offset_survive_saved_state_with_new_arrivals() = runComposeUiTest {
        val entries = mutableStateOf(value = (0..40).map { log(tag = "row$it") })
        val restoration = StateRestorationTester(this)
        restoration.setContent { TestAnalyticsScreen(highlightedAnalyticsLogTypes = persistentListOf(), logs = entries.value) }
        val list = onNodeWithTag(testTag = "analytics_event_list")
        list.performScrollToIndex(index = 21)
        list.performSemanticsAction(key = SemanticsActions.ScrollBy) { scroll -> scroll(0f, 23f) }
        val row = onNodeWithTag(testTag = "analytics_log_item_row20")
        val initialY = row.fetchSemanticsNode().positionInRoot.y
        runOnIdle { entries.value = entries.value + log(tag = "newest") }
        restoration.emulateSaveAndRestore()
        row.fetchSemanticsNode().positionInRoot.y shouldBe initialY
    }

    @Test
    fun opening_details_and_returning_preserves_search_category_and_time_range() = runComposeUiTest {
        val logs = listOf(log(tag = "login"), log(tag = "view", type = AnalyticsLogCategory.Screen.View))
        setContent { TestAnalyticsScreen(highlightedAnalyticsLogTypes = persistentListOf(), logs = logs) }
        onNodeWithContentDescription(label = "Show filters, 0 active").performClick()
        val tag = "category_chip_${AnalyticsLogCategory.Action.Click.category.displayName}"
        onNodeWithTag(testTag = tag).performScrollTo().performClick()
        onNodeWithTag(testTag = "time_range_5m").performScrollTo().performClick()
        onNodeWithText(text = "Filter by tag or screen").performTextInput(text = "login")
        onNodeWithTag(testTag = "analytics_log_item_login").performClick()
        onNodeWithText(text = "Back to events").performClick()
        onNodeWithContentDescription(label = "Hide filters, 2 active").assertIsDisplayed()
        onNodeWithTag(testTag = tag).assertIsSelected()
        onNodeWithTag(testTag = "time_range_5m").assertIsSelected()
        onNodeWithText(text = "Filter by tag or screen").assertTextContains(value = "login")
        waitUntilTagCount(tag = "analytics_log_item_view", expectedCount = 0)
    }

    @Test
    fun restored_detail_does_not_resolve_to_a_new_event_with_identical_data() = runComposeUiTest {
        val event = log(tag = "original")
        // Equal payloads still represent different recordings after process restoration.
        val logs = mutableStateOf(value = listOf(event), policy = neverEqualPolicy())
        val restoration = StateRestorationTester(this)
        restoration.setContent { TestAnalyticsScreen(highlightedAnalyticsLogTypes = persistentListOf(), logs = logs.value) }
        onNodeWithTag(testTag = "analytics_log_item_original").performClick()
        runOnIdle { logs.value = listOf(event.copy()) }
        restoration.emulateSaveAndRestore()
        onNodeWithText(text = "Event no longer available").assertIsDisplayed()
        onNodeWithText(text = "Copy event").assertDoesNotExist()
        onNodeWithText(text = "Back to events").performClick()
        onNodeWithTag(testTag = "analytics_log_item_original").assertIsDisplayed()
    }

    @Test
    fun time_window_refresh_removes_expired_events_without_input() = runComposeUiTest {
        val event = log(tag = "expiring", timestamp = Clock.System.now().toEpochMilliseconds() - 298_000)
        setContent { TestAnalyticsScreen(highlightedAnalyticsLogTypes = persistentListOf(), logs = listOf(event)) }
        onNodeWithContentDescription(label = "Show filters, 0 active").performClick()
        onNodeWithTag(testTag = "time_range_5m").performScrollTo().performClick()
        waitUntilTagCount(tag = "analytics_log_item_expiring", expectedCount = 1)
        waitUntilTagCount(tag = "analytics_log_item_expiring", expectedCount = 0)
        onNodeWithText(text = "No logs match your filters").assertIsDisplayed()
    }

    @Composable
    private fun TestAnalyticsScreen(
        highlightedAnalyticsLogTypes: PersistentList<AnalyticsLogType>,
        logs: List<AnalyticsLog> = AnalyticsLogger.logs
    ) {
        val backStack = rememberNavBackStack(AnalyticsDestination.Main)
        val goBack = { backStack.removeLastOrNull(); Unit }
        NavDisplay(
            backStack = backStack,
            onBack = goBack,
            entryDecorators = listOf(element = rememberSaveableStateHolderNavEntryDecorator()),
            entryProvider = entryProvider {
                entry<AnalyticsDestination.Main> {
                    AnalyticsScreen(
                        highlightedAnalyticsLogTypes = highlightedAnalyticsLogTypes,
                        logs = logs,
                        onEventClick = { backStack.add(element = AnalyticsDestination.Detail(id = it.id)) }
                    )
                }
                entry<AnalyticsDestination.Detail> { destination ->
                    AnalyticsDetailScreen(
                        log = logs.firstOrNull { it.id == destination.id },
                        onBack = goBack
                    )
                }
            }
        )
    }

    private fun log(
        tag: String,
        type: AnalyticsLogType = AnalyticsLogCategory.Action.Click,
        timestamp: Long = Clock.System.now().toEpochMilliseconds(),
        params: Map<String, String> = emptyMap()
    ): AnalyticsLog = AnalyticsLog(tag = tag, screenClass = "TestScreen", timestamp = timestamp, type = type, params = params)
}
