package com.worldline.devview.crashhistory

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.worldline.devview.crashhistory.core.CrashMetadata
import com.worldline.devview.crashhistory.core.CrashRecord
import com.worldline.devview.crashhistory.core.CrashRecordKind
import com.worldline.devview.test.waitUntilTagCount
import io.kotest.matchers.shouldBe
import org.junit.Test

class CrashHistoryScreenTest {

    @Test
    fun crashListScreen_shows_empty_state_when_no_records() = runComposeUiTest {
        setContent { CrashListScreen(records = emptyList(), onOpen = {}) }

        waitUntilTagCount(tag = "crash_empty_state", expectedCount = 1)
        waitUntilTagCount(tag = "crash_export_all_button", expectedCount = 0)
    }

    @Test
    fun crashListScreen_lists_records_and_opens_the_tapped_one() = runComposeUiTest {
        var opened: String? = null
        setContent {
            CrashListScreen(
                records = listOf(record(id = "a"), record(id = "b")),
                onOpen = { opened = it }
            )
        }

        waitUntilTagCount(tag = "crash_item_a", expectedCount = 1)
        waitUntilTagCount(tag = "crash_item_b", expectedCount = 1)
        waitUntilTagCount(tag = "crash_export_all_button", expectedCount = 1)

        onNodeWithTag(testTag = "crash_item_b").performClick()

        opened shouldBe "b"
    }

    @Test
    fun crashDetailScreen_shows_stack_trace_and_actions() = runComposeUiTest {
        setContent { CrashDetailScreen(record = record(id = "a")) }

        waitUntilTagCount(tag = "crash_detail_stack_trace", expectedCount = 1)
        waitUntilTagCount(tag = "crash_copy_button", expectedCount = 1)
        waitUntilTagCount(tag = "crash_share_button", expectedCount = 1)
        waitUntilTagCount(tag = "crash_export_button", expectedCount = 1)
    }

    @Test
    fun crashDetailScreen_shows_message_when_record_was_cleared() = runComposeUiTest {
        setContent { CrashDetailScreen(record = null) }

        waitUntilTagCount(tag = "crash_detail_missing", expectedCount = 1)
    }

    private fun record(id: String) = CrashRecord(
        id = id,
        timestampMillis = 1_700_000_000_000,
        kind = CrashRecordKind.UNCAUGHT_EXCEPTION,
        exceptionType = "java.lang.IllegalStateException",
        message = "boom",
        stackTrace = "java.lang.IllegalStateException: boom\n\tat Foo.bar(Foo.kt:1)",
        metadata = CrashMetadata(
            platform = "Android",
            osVersion = "14",
            deviceModel = "Pixel",
            appVersion = "1.0",
            buildNumber = "1"
        )
    )
}
