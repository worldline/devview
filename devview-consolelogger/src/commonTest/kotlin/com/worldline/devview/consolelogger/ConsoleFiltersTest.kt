package com.worldline.devview.consolelogger

import com.worldline.devview.consolelogger.model.ConsoleLog
import com.worldline.devview.consolelogger.model.LogLevel
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ConsoleFiltersTest {
    @Test
    fun `query matches tag or complete message and respects visible levels`() {
        val first = log(tag = "Network", message = "request", level = LogLevel.INFO)
        val second = log(tag = "ui", message = "first\nNETWORK failure", level = LogLevel.ERROR)
        filterConsoleLogs(
            logs = listOf(first, second), query = "network", levels = setOf(LogLevel.ERROR)
        ) shouldContainExactly listOf(second)
        filterConsoleLogs(logs = listOf(first), query = "", levels = emptySet()) shouldBe emptyList()
    }

    @Test
    fun `noise rules match tag or full message without changing retained entries`() {
        val byTag = log(tag = "NoisyTag", message = "ordinary")
        val byMessage = log(tag = "ui", message = "first\nsetRequestedFrameRate second")
        val kept = log(tag = "useful", message = "request completed")
        val history = listOf(byTag, byMessage, kept)
        filterConsoleLogs(
            logs = history, query = "", levels = LogLevel.entries.toSet(),
            excludedPatterns = listOf("noisytag", "SETREQUESTEDFRAMERATE", " ")
        ) shouldContainExactly listOf(kept)
        history.size shouldBe 3
        filterConsoleLogs(logs = history, query = "", levels = LogLevel.entries.toSet()) shouldContainExactly history
    }

    @Test
    fun `built in frame rate exclusions hide both messages without dropping history`() {
        val history = listOf(
            log(tag = "ui", message = "setRequestedFrameRate called"),
            log(tag = "ui", message = "call SETFRAMERATECATEGORY for touch"),
            log(tag = "useful", message = "request completed")
        )
        filterConsoleLogs(
            logs = history, query = "", levels = LogLevel.entries.toSet(),
            excludedPatterns = Console.DEFAULT_EXCLUDED_PATTERNS
        ) shouldContainExactly listOf(history.last())
        history.size shouldBe 3
    }

    @Test
    fun `row keys distinguish repeats and survive filtering and eviction`() {
        val first = log(tag = "a", message = "one")
        val second = log(tag = "b", message = "two")
        val keys = consoleRowKeys(logs = listOf(first, second, second))
        keys.distinct().size shouldBe 3
        consoleRowKeys(logs = listOf(second)).single() shouldBe keys[1]
    }

    private fun log(tag: String, message: String, level: LogLevel = LogLevel.INFO): ConsoleLog =
        ConsoleLog(level = level, tag = tag, message = message, timestamp = 1_700_000_000_000)
}
