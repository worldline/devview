package com.worldline.devview.consolelogger

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.StateRestorationTester
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.worldline.devview.consolelogger.model.ConsoleLog
import com.worldline.devview.consolelogger.model.LogLevel
import com.worldline.devview.consolelogger.theme.LocalLogColorScheme
import com.worldline.devview.consolelogger.theme.LogColorScheme
import com.worldline.devview.consolelogger.theme.rememberLogColorScheme
import com.worldline.devview.test.FakePreferencesDataStore
import com.worldline.devview.test.waitUntilTagCount
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okio.IOException
import org.junit.Test

class ConsoleScreenTest {
    @Test
    fun empty_history_shows_guidance() = runComposeUiTest {
        setContent { TestConsoleScreen(logs = emptyList()) }
        onNodeWithText(text = "No logs yet").assertIsDisplayed()
    }

    @Test
    fun search_and_clear_restore_results() = runComposeUiTest {
        val first = log(tag = "network", message = "request sent")
        val second = log(tag = "ui", message = "button clicked")
        setContent { TestConsoleScreen(logs = listOf(first, second)) }
        onNodeWithText(text = "Filter by tag or message").performTextInput(text = "network")
        waitUntilTagCount(tag = "console_log_item_${second.id}", expectedCount = 0)
        onNodeWithText(text = "Filter by tag or message").performTextClearance()
        onNodeWithTag(testTag = "console_log_item_${second.id}").assertIsDisplayed()
    }

    @Test
    fun hiding_all_levels_is_recoverable_and_badge_survives_collapse() = runComposeUiTest {
        setContent { TestConsoleScreen(logs = listOf(log(tag = "a", message = "info"))) }
        onNodeWithContentDescription(label = "Show filters, 2 active").performClick()
        LogLevel.entries.forEach { level ->
            onNodeWithTag(testTag = "console_level_chip_${level.name}").performScrollTo().performClick()
        }
        onNodeWithText(text = "No logs match your filters").assertIsDisplayed()
        onNodeWithContentDescription(label = "Hide filters, 9 active").performClick()
        onNodeWithContentDescription(label = "Show filters, 9 active").assertIsDisplayed()
        onNodeWithText(text = "Clear search and filters").performClick()
        onNodeWithContentDescription(label = "Show filters, 0 active").assertIsDisplayed()
    }

    @Test
    fun expanding_message_pauses_follow_and_copies_full_entry() = runComposeUiTest {
        val entry = log(tag = "trace", message = "first line\nsecond line", level = LogLevel.ERROR)
        setContent { TestConsoleScreen(logs = listOf(entry)) }
        onNodeWithText(text = "first line").performClick()
        onNodeWithTag(testTag = "console_expanded_message_${entry.id}").assertIsDisplayed()
        onNodeWithContentDescription(label = "Error", useUnmergedTree = true).assertExists()
        onNodeWithContentDescription(label = "Resume following latest log").assertIsDisplayed()
        val clipboard = InstrumentationRegistry.getInstrumentation().targetContext
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        onNodeWithText(text = "Copy log").performClick()
        waitUntil(timeoutMillis = 10_000) {
            clipboard.primaryClip?.getItemAt(0)?.text?.toString() == entry.copyText()
        }
    }

    @Test
    fun filters_expansion_and_follow_mode_survive_restoration() = runComposeUiTest {
        val entry = log(tag = "trace", message = "first\nsecond", level = LogLevel.ERROR)
        val restoration = StateRestorationTester(this)
        restoration.setContent { TestConsoleScreen(logs = listOf(entry)) }
        onNodeWithText(text = "first").performClick()
        onNodeWithContentDescription(label = "Show filters, 2 active").performClick()
        onNodeWithTag(testTag = "console_level_chip_INFO").performScrollTo().performClick()
        onNodeWithText(text = "Filter by tag or message").performTextInput(text = "trace")
        restoration.emulateSaveAndRestore()
        onNodeWithContentDescription(label = "Hide filters, 3 active").assertIsDisplayed()
        onNodeWithTag(testTag = "console_level_chip_ERROR").assertIsSelected()
        onNodeWithTag(testTag = "console_expanded_message_${entry.id}").assertExists()
        onNodeWithContentDescription(label = "Resume following latest log").assertExists()
    }

    @Test
    fun newest_first_rows_fill_the_viewport_without_empty_follow_padding() = runComposeUiTest {
        val older = log(tag = "older", message = "older message")
        val latest = log(tag = "latest", message = "latest message")
        setContent { TestConsoleScreen(logs = listOf(older, latest)) }
        val newestNode = onNodeWithTag(testTag = "console_log_item_${latest.id}")
        val olderNode = onNodeWithTag(testTag = "console_log_item_${older.id}")
        newestNode.assertIsDisplayed()
        olderNode.assertIsDisplayed()
        (newestNode.fetchSemanticsNode().positionInRoot.y < olderNode.fetchSemanticsNode().positionInRoot.y) shouldBe true
    }

    @Test
    fun paused_arrivals_preserve_the_inspected_row_and_resume_returns_to_latest() = runComposeUiTest {
        val entries = mutableStateOf(value = (0..40).map { log(tag = "row$it", message = "message $it") })
        setContent { TestConsoleScreen(logs = entries.value) }
        val inspected = entries.value[20]
        onNodeWithTag(testTag = "console_log_item_${entries.value.last().id}").performClick()
        onNodeWithTag(testTag = "console_log_list").performScrollToIndex(index = 20)
        val row = onNodeWithTag(testTag = "console_log_item_${inspected.id}")
        val initialY = row.fetchSemanticsNode().positionInRoot.y
        val latest = log(tag = "incoming", message = "incoming message")
        runOnIdle { entries.value = entries.value + latest }
        row.fetchSemanticsNode().positionInRoot.y shouldBe initialY
        onNodeWithContentDescription(label = "Resume following latest log").performClick()
        onNodeWithTag(testTag = "console_log_item_${latest.id}").assertIsDisplayed()
    }

    @Test
    fun paused_anchor_waits_for_saved_noise_rules_and_survives_absence_and_restoration() = runComposeUiTest {
        val store = FakePreferencesDataStore()
        runBlocking {
            ConsoleNoiseRepository(dataStore = store, defaults = Console.DEFAULT_EXCLUDED_PATTERNS)
                .update(change = ConsoleNoiseChange.DisableAll)
        }
        var readGate: CompletableDeferred<Unit>? = null
        val delayedStore = object : DataStore<Preferences> by store {
            override val data: Flow<Preferences> = flow {
                readGate?.await()
                emitAll(flow = store.data)
            }
        }
        val entries = mutableStateOf(value = (0..40).map { log(tag = "setRequestedFrameRate", message = "message $it") })
        val show = mutableStateOf(value = true)
        val inspected = entries.value[20]
        val restoration = StateRestorationTester(this)
        restoration.setContent {
            val holder = rememberSaveableStateHolder()
            if (show.value) {
                holder.SaveableStateProvider(key = "console") {
                    ConsoleScreen(dataStore = delayedStore, defaults = Console.DEFAULT_EXCLUDED_PATTERNS, logs = entries.value)
                }
            }
        }
        onNodeWithTag(testTag = "console_log_item_${entries.value.last().id}")
            .assertIsDisplayed().performClick()
        onNodeWithTag("console_log_list").performScrollToIndex(index = 20)
        onNodeWithTag("console_log_list").performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 23f) }
        val row = onNodeWithTag("console_log_item_${inspected.id}")
        val initialY = row.fetchSemanticsNode().positionInRoot.y
        runOnIdle { show.value = false }
        runOnIdle {
            readGate = CompletableDeferred()
            entries.value = entries.value + log(tag = "setRequestedFrameRate", message = "incoming")
        }
        restoration.emulateSaveAndRestore()
        runOnIdle { show.value = true }
        onNodeWithText("No logs match your filters").assertIsDisplayed()
        runOnIdle { readGate!!.complete(Unit) }
        row.assertIsDisplayed()
        row.fetchSemanticsNode().positionInRoot.y shouldBe initialY
        onNodeWithContentDescription("Resume following latest log").assertIsDisplayed()
        runOnIdle { entries.value = entries.value + log(tag = "setRequestedFrameRate", message = "another") }
        row.fetchSemanticsNode().positionInRoot.y shouldBe initialY
    }

    @Test
    fun a_missing_restored_anchor_uses_the_newest_matching_row_without_resuming_follow() = runComposeUiTest {
        val entries = mutableStateOf(value = (0..40).map { log(tag = "row$it", message = "message $it") })
        val show = mutableStateOf(value = true)
        setContent {
            val holder = rememberSaveableStateHolder()
            if (show.value) {
                holder.SaveableStateProvider(key = "console") { TestConsoleScreen(logs = entries.value) }
            }
        }
        onNodeWithTag("console_log_item_${entries.value.last().id}")
            .performClick()
        onNodeWithTag("console_log_list").performScrollToIndex(index = 20)
        onNodeWithTag("console_log_list").performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 23f) }
        runOnIdle { show.value = false }
        val replacement = log(tag = "replacement", message = "newest matching")
        val excluded = log(tag = "setRequestedFrameRate", message = "newest excluded")
        runOnIdle {
            entries.value = (0..30).map { log(tag = "older$it", message = "older $it") } + replacement + excluded
            show.value = true
        }
        val row = onNodeWithTag("console_log_item_${replacement.id}")
        row.assertIsDisplayed()
        row.fetchSemanticsNode().positionInRoot.y shouldBe
            onNodeWithTag("console_log_list").fetchSemanticsNode().positionInRoot.y
        onNodeWithTag("console_log_item_${excluded.id}").assertDoesNotExist()
        onNodeWithContentDescription("Resume following latest log").assertIsDisplayed()
    }

    @Test
    fun following_moves_to_new_entry_when_retained_size_is_unchanged() = runComposeUiTest {
        val entries = mutableStateOf(value = (0..30).map { log(tag = "row$it", message = "message $it") })
        setContent { TestConsoleScreen(logs = entries.value) }
        val next = log(tag = "newest", message = "newest message")
        runOnIdle { entries.value = entries.value.drop(n = 1) + next }
        onNodeWithTag(testTag = "console_log_item_${next.id}").assertIsDisplayed()
    }

    @Test
    fun replacing_equal_payloads_uses_new_identity_and_repeated_objects_render() = runComposeUiTest {
        val first = log(tag = "same", message = "message")
        val entries = mutableStateOf(value = listOf(first), policy = neverEqualPolicy())
        setContent { TestConsoleScreen(logs = entries.value) }
        onNodeWithTag(testTag = "console_log_item_${first.id}").performClick()
        val next = first.copy()
        runOnIdle { entries.value = listOf(next, next) }
        waitUntilTagCount(tag = "console_log_item_${first.id}", expectedCount = 0)
        onAllNodesWithTag(testTag = "console_log_item_${next.id}").fetchSemanticsNodes().size shouldBe 2
    }

    @Test
    fun eviction_and_clear_prune_expansion_but_filters_do_not() = runComposeUiTest {
        val entry = log(tag = "trace", message = "first\nsecond")
        val replacement = log(tag = "replacement", message = "another")
        val entries = mutableStateOf(value = listOf(entry))
        setContent { TestConsoleScreen(logs = entries.value) }
        onNodeWithTag(testTag = "console_log_item_${entry.id}").performClick()
        onNodeWithText(text = "Filter by tag or message").performTextInput(text = "missing")
        onNodeWithText(text = "No logs match your filters").assertIsDisplayed()
        onNodeWithText(text = "Filter by tag or message").performTextClearance()
        onNodeWithTag(testTag = "console_expanded_message_${entry.id}").assertExists()
        runOnIdle { entries.value = listOf(replacement) }
        waitForIdle()
        runOnIdle { entries.value = listOf(entry) }
        onNodeWithTag(testTag = "console_expanded_message_${entry.id}").assertDoesNotExist()
        onNodeWithTag(testTag = "console_log_item_${entry.id}").performClick()
        runOnIdle { entries.value = emptyList() }
        onNodeWithText(text = "No logs yet").assertIsDisplayed()
        runOnIdle { entries.value = listOf(entry) }
        onNodeWithTag(testTag = "console_expanded_message_${entry.id}").assertDoesNotExist()
    }

    @Test
    fun repeated_objects_keep_positional_expansion_when_an_earlier_occurrence_is_removed() = runComposeUiTest {
        val entry = log(tag = "same", message = "first\nsecond")
        val entries = mutableStateOf(value = listOf(entry, entry))
        setContent { TestConsoleScreen(logs = entries.value) }
        // Ranks are assigned before reversing; the newest occurrence retains rank one.
        onAllNodesWithTag(testTag = "console_log_item_${entry.id}")[0].performClick()
        onNodeWithTag(testTag = "console_expanded_message_${entry.id}").assertExists()
        runOnIdle { entries.value = listOf(entry) }
        onNodeWithTag(testTag = "console_expanded_message_${entry.id}").assertDoesNotExist()
    }

    @Test
    fun noise_rules_add_remove_disable_and_reset_survive_restoration() = runComposeUiTest {
        val noise = log(tag = "frame", message = "setRequestedFrameRate called")
        val useful = log(tag = "MyTag", message = "important")
        val restoration = StateRestorationTester(this)
        restoration.setContent { TestConsoleScreen(logs = listOf(noise, useful)) }
        onNodeWithTag(testTag = "console_log_item_${noise.id}").assertDoesNotExist()
        onNodeWithContentDescription(label = "Show filters, 2 active").performClick()
        onNodeWithTag(testTag = "console_noise_pattern_input").performScrollTo().performTextInput(text = "MyTag")
        onNodeWithText(text = "Add exclusion").performScrollTo().performClick()
        onNodeWithText(text = "No logs match your filters").assertIsDisplayed()
        restoration.emulateSaveAndRestore()
        onNodeWithTag(testTag = "console_noise_toggle_mytag").performScrollTo().assertExists()
        onNodeWithText(text = "Clear search and filters").performClick()
        onNodeWithTag(testTag = "console_log_item_${noise.id}").assertExists()
        onNodeWithContentDescription(label = "Remove exclusion: MyTag").performScrollTo().performClick()
        onNodeWithText(text = "Reset to defaults").performScrollTo().performClick()
        onNodeWithTag(testTag = "console_log_item_${noise.id}").assertDoesNotExist()
        onNodeWithTag(testTag = "console_log_item_${useful.id}").assertExists()
    }

    @Test
    fun failed_noise_write_keeps_saved_rules_and_retry_applies_the_edit() = runComposeUiTest {
        val entry = log(tag = "frame", message = "setRequestedFrameRate called")
        var fail = true
        val store = FakePreferencesDataStore()
        val failingStore = object : DataStore<Preferences> by store {
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
                if (fail) throw IOException("Disk full")
                return store.updateData(transform = transform)
            }
        }
        setContent {
            ConsoleScreen(dataStore = failingStore, defaults = Console.DEFAULT_EXCLUDED_PATTERNS, logs = listOf(entry))
        }
        onNodeWithContentDescription(label = "Show filters, 2 active").performClick()
        onNodeWithTag(testTag = "console_noise_toggle_setrequestedframerate").performScrollTo().performClick()
        onNodeWithText(text = "Could not save noise filters. Your last saved rules are still in use.")
            .performScrollTo().assertIsDisplayed()
        onNodeWithTag(testTag = "console_log_item_${entry.id}").assertDoesNotExist()
        onNodeWithTag(testTag = "console_noise_toggle_setrequestedframerate").assertIsNotEnabled()
        onNodeWithText(text = "Clear filters").assertIsNotEnabled()
        onNodeWithText(text = "Clear search and filters").assertIsNotEnabled()
        runOnIdle { fail = false }
        onNodeWithText(text = "Retry noise filters").performScrollTo().performClick()
        onNodeWithTag(testTag = "console_log_item_${entry.id}").assertExists()
        onNodeWithText(text = "Retry noise filters").assertDoesNotExist()
    }

    @Test
    fun failed_noise_read_does_not_overwrite_saved_rules_and_retry_loads_them() = runComposeUiTest {
        val entry = log(tag = "frame", message = "setRequestedFrameRate called")
        val store = FakePreferencesDataStore()
        runBlocking {
            ConsoleNoiseRepository(dataStore = store, defaults = Console.DEFAULT_EXCLUDED_PATTERNS)
                .update(change = ConsoleNoiseChange.DisableAll)
        }
        var fail = true
        var writes = 0
        val failingStore = object : DataStore<Preferences> by store {
            override val data: Flow<Preferences> = flow {
                if (fail) throw IOException("Cannot read preferences")
                emitAll(flow = store.data)
            }
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
                writes++
                return store.updateData(transform = transform)
            }
        }
        setContent {
            ConsoleScreen(dataStore = failingStore, defaults = Console.DEFAULT_EXCLUDED_PATTERNS, logs = listOf(entry))
        }
        onNodeWithContentDescription(label = "Show filters, 2 active").performClick()
        onNodeWithText(text = "Could not load noise filters. Your saved rules have not been changed.")
            .performScrollTo().assertIsDisplayed()
        onNodeWithTag(testTag = "console_noise_pattern_input").performScrollTo().assertIsNotEnabled()
        onNodeWithText(text = "Clear filters").assertIsNotEnabled()
        onNodeWithText(text = "Clear search and filters").assertIsNotEnabled()
        runOnIdle {
            writes shouldBe 0
            fail = false
        }
        onNodeWithText(text = "Retry noise filters").performScrollTo().performClick()
        onNodeWithTag(testTag = "console_log_item_${entry.id}").assertExists()
        onNodeWithContentDescription(label = "Hide filters, 0 active").assertExists()
        runOnIdle { writes shouldBe 0 }
    }

    @Test
    fun pending_noise_write_disables_further_edits_until_it_finishes() = runComposeUiTest {
        val entry = log(tag = "frame", message = "setRequestedFrameRate called")
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val store = FakePreferencesDataStore()
        var writes = 0
        val delayedStore = object : DataStore<Preferences> by store {
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
                writes++
                entered.complete(value = Unit)
                release.await()
                return store.updateData(transform = transform)
            }
        }
        setContent {
            ConsoleScreen(dataStore = delayedStore, defaults = Console.DEFAULT_EXCLUDED_PATTERNS, logs = listOf(entry))
        }
        onNodeWithContentDescription(label = "Show filters, 2 active").performClick()
        onNodeWithTag(testTag = "console_noise_toggle_setrequestedframerate").performScrollTo().performClick()
        waitUntil { entered.isCompleted }
        onNodeWithTag(testTag = "console_noise_toggle_setrequestedframerate").assertIsNotEnabled()
        onNodeWithText(text = "Reset to defaults").performScrollTo().assertIsNotEnabled()
        onNodeWithText(text = "Clear filters").assertIsNotEnabled()
        onNodeWithText(text = "Clear search and filters").assertIsNotEnabled()
        onNodeWithText(text = "Filter by tag or message").performTextInput(text = "frame")
        onNodeWithTag(testTag = "console_log_item_${entry.id}").assertDoesNotExist()
        runOnIdle { writes shouldBe 1 }
        release.complete(value = Unit)
        onNodeWithTag(testTag = "console_log_item_${entry.id}").assertExists()
        runOnIdle { writes shouldBe 1 }
    }

    @Test
    fun initial_noise_loading_disables_combined_clear_but_not_search_or_levels() = runComposeUiTest {
        val release = CompletableDeferred<Unit>()
        val store = FakePreferencesDataStore()
        val delayedStore = object : DataStore<Preferences> by store {
            override val data: Flow<Preferences> = flow {
                release.await()
                emitAll(flow = store.data)
            }
        }
        val entry = log(tag = "frame", message = "setRequestedFrameRate called")
        setContent {
            ConsoleScreen(dataStore = delayedStore, defaults = Console.DEFAULT_EXCLUDED_PATTERNS, logs = listOf(entry))
        }
        onNodeWithContentDescription(label = "Show filters, 2 active").performClick()
        onNodeWithText(text = "Clear filters").assertIsNotEnabled()
        onNodeWithText(text = "Clear search and filters").assertIsNotEnabled()
        onNodeWithText(text = "Filter by tag or message").assertIsEnabled()
        onNodeWithTag(testTag = "console_level_chip_INFO").performScrollTo().assertIsEnabled().performClick()
        onNodeWithContentDescription(label = "Hide filters, 3 active").assertExists()
        release.complete(value = Unit)
        onNodeWithText(text = "Clear filters").assertIsEnabled().performClick()
        onNodeWithTag(testTag = "console_log_item_${entry.id}").assertExists()
    }

    @Test
    fun read_retry_waits_for_a_new_emission_after_a_previously_successful_load() = runComposeUiTest {
        val failAfterLoad = CompletableDeferred<Unit>()
        val releaseRetry = CompletableDeferred<Unit>()
        val store = FakePreferencesDataStore()
        var reads = 0
        var writes = 0
        val recoveringStore = object : DataStore<Preferences> by store {
            override val data: Flow<Preferences> = flow {
                reads++
                if (reads == 1) {
                    emit(value = store.data.first())
                    failAfterLoad.await()
                    throw IOException("Read failed after loading")
                }
                releaseRetry.await()
                emitAll(flow = store.data)
            }
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
                writes++
                return store.updateData(transform = transform)
            }
        }
        val entry = log(tag = "frame", message = "setRequestedFrameRate called")
        setContent {
            ConsoleScreen(dataStore = recoveringStore, defaults = Console.DEFAULT_EXCLUDED_PATTERNS, logs = listOf(entry))
        }
        onNodeWithContentDescription(label = "Show filters, 2 active").performClick()
        onNodeWithText(text = "Clear filters").assertIsEnabled()
        failAfterLoad.complete(value = Unit)
        onNodeWithText(text = "Could not load noise filters. Your saved rules have not been changed.")
            .performScrollTo().assertIsDisplayed()
        onNodeWithText(text = "Clear filters").assertIsNotEnabled()
        onNodeWithText(text = "Retry noise filters").performScrollTo().performClick()
        waitUntil { reads == 2 }
        onNodeWithText(text = "Loading saved noise filters…").performScrollTo().assertIsDisplayed()
        onNodeWithTag(testTag = "console_noise_pattern_input").performScrollTo().assertIsNotEnabled()
        onNodeWithText(text = "Clear filters").assertIsNotEnabled()
        onNodeWithText(text = "Clear search and filters").assertIsNotEnabled()
        onNodeWithTag(testTag = "console_log_item_${entry.id}").assertDoesNotExist()
        runOnIdle { writes shouldBe 0 }
        releaseRetry.complete(value = Unit)
        onNodeWithText(text = "Clear filters").assertIsEnabled()
        runOnIdle { writes shouldBe 0 }
    }

    @Test
    fun rememberLogColorScheme_honours_explicit_scheme() = runComposeUiTest {
        var resolved: LogColorScheme? = null
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                CompositionLocalProvider(LocalLogColorScheme provides LogColorScheme.Light) {
                    resolved = rememberLogColorScheme()
                }
            }
        }
        waitForIdle()
        resolved shouldBe LogColorScheme.Light
    }

    @Test
    fun rememberLogColorScheme_falls_back_to_dark() = runComposeUiTest {
        var resolved: LogColorScheme? = null
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                resolved = rememberLogColorScheme()
            }
        }
        waitForIdle()
        resolved shouldBe LogColorScheme.Dark
    }

    private val dataStore = FakePreferencesDataStore()

    @Composable
    private fun TestConsoleScreen(logs: List<ConsoleLog>) {
        ConsoleScreen(
            logs = logs,
            dataStore = dataStore,
            defaults = Console.DEFAULT_EXCLUDED_PATTERNS
        )
    }

    private fun log(tag: String, message: String, level: LogLevel = LogLevel.INFO): ConsoleLog =
        ConsoleLog(level = level, tag = tag, message = message, timestamp = 1_700_000_000_000)
}
