package com.worldline.devview.consolelogger

import com.worldline.devview.consolelogger.model.ConsoleLog
import com.worldline.devview.consolelogger.model.LogLevel
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

class ConsoleLoggerTest {
    @AfterTest
    fun tearDown() {
        ConsoleLogger.clear()
        ConsoleLogger.setExcludedPatterns(patterns = emptyList())
        ConsoleLogger.configure(maxEntries = ConsoleLogger.DEFAULT_MAX_ENTRIES)
    }

    @Test
    fun `recording repeats gets fresh IDs without changing value equality and snapshots do not change`() = runTest {
        val job = launch { ConsoleLogger.collectInto(maxEntries = 10) }
        val entry = testLog(message = "same")
        ConsoleLogger.log(log = entry)
        advanceUntilIdle()
        val before = ConsoleLogger.logs
        ConsoleLogger.log(log = entry)
        advanceUntilIdle()
        ConsoleLogger.logs shouldContainExactly listOf(entry, entry)
        ConsoleLogger.logs.map { it.id }.distinct().size shouldBe 2
        before.size shouldBe 1
        job.cancelAndJoin()
    }

    @Test
    fun `pending entries survive initial configuration and shrinking keeps newest entries`() = runTest {
        ConsoleLogger.configure(maxEntries = ConsoleLogger.DEFAULT_MAX_ENTRIES)
        ConsoleLogger.log(log = testLog(message = "first"))
        ConsoleLogger.log(log = testLog(message = "second"))
        ConsoleLogger.log(log = testLog(message = "third"))
        ConsoleLogger.configure(maxEntries = ConsoleLogger.DEFAULT_MAX_ENTRIES)
        val job = launch { ConsoleLogger.collectInto(maxEntries = 2) }
        advanceUntilIdle()
        ConsoleLogger.logs.map { it.message } shouldContainExactly listOf("second", "third")
        job.cancelAndJoin()
    }

    @Test
    fun `queue overflow and retained history eviction keep newest entries`() = runTest {
        ConsoleLogger.configure(maxEntries = 2)
        (1..3).forEach { ConsoleLogger.log(log = testLog(message = "$it")) }
        val job = launch { ConsoleLogger.collectInto() }
        advanceUntilIdle()
        ConsoleLogger.logs.map { it.message } shouldContainExactly listOf("2", "3")
        ConsoleLogger.log(log = testLog(message = "4"))
        advanceUntilIdle()
        ConsoleLogger.logs.map { it.message } shouldContainExactly listOf("3", "4")
        job.cancelAndJoin()
    }

    @Test
    fun `clear discards pending entries and permits new entries`() = runTest {
        ConsoleLogger.configure(maxEntries = 10)
        ConsoleLogger.log(log = testLog(message = "pending"))
        ConsoleLogger.clear()
        val job = launch { ConsoleLogger.collectInto() }
        advanceUntilIdle()
        ConsoleLogger.hasLogs shouldBe false
        ConsoleLogger.log(log = testLog(message = "retained"))
        advanceUntilIdle()
        ConsoleLogger.clear()
        ConsoleLogger.log(log = testLog(message = "new"))
        advanceUntilIdle()
        ConsoleLogger.logs.map { it.message } shouldContainExactly listOf("new")
        job.cancelAndJoin()
    }

    @Test
    fun `cancellation preserves queue and next owner trims history with one capture active`() = runTest {
        var activeCaptures = 0
        var maximumCaptures = 0
        val capture: suspend () -> Unit = {
            activeCaptures++
            maximumCaptures = maxOf(maximumCaptures, activeCaptures)
            try {
                awaitCancellation()
            } finally {
                activeCaptures--
            }
        }
        val first = launch { ConsoleLogger.collectInto(maxEntries = 3, capture = capture) }
        (1..3).forEach { ConsoleLogger.log(log = testLog(message = "$it")) }
        advanceUntilIdle()
        val second = launch { ConsoleLogger.collectInto(maxEntries = 2, capture = capture) }
        advanceUntilIdle()
        activeCaptures shouldBe 1
        first.cancelAndJoin()
        advanceUntilIdle()
        ConsoleLogger.logs.map { it.message } shouldContainExactly listOf("2", "3")
        second.cancelAndJoin()
        ConsoleLogger.log(log = testLog(message = "between lifetimes"))
        val third = launch { ConsoleLogger.collectInto(maxEntries = 2, capture = capture) }
        advanceUntilIdle()
        ConsoleLogger.logs.last().message shouldBe "between lifetimes"
        maximumCaptures shouldBe 1
        third.cancelAndJoin()
        activeCaptures shouldBe 0
    }

    @Test
    fun `configure rejects non-positive and unlimited maxEntries`() {
        listOf(0, -1, Int.MAX_VALUE).forEach { capacity ->
            shouldThrow<IllegalArgumentException> { ConsoleLogger.configure(maxEntries = capacity) }
        }
    }

    @Test
    fun `cancelled collector does not continue draining a nonempty queue`() = runTest {
        lateinit var collector: Job
        collector = launch(start = CoroutineStart.LAZY) {
            ConsoleLogger.collectInto(capture = {
                (1..10).forEach { ConsoleLogger.log(log = testLog(message = "$it")) }
                collector.cancel()
            })
        }
        collector.start()
        collector.join()
        ConsoleLogger.hasLogs shouldBe false
    }

    @Test
    fun `excluded noise cannot overflow the queue or evict accepted history`() = runTest {
        ConsoleLogger.configure(maxEntries = 2)
        ConsoleLogger.setExcludedPatterns(patterns = listOf("NOISY", "frame"))
        ConsoleLogger.log(log = testLog(message = "important"))
        repeat(times = 20) {
            ConsoleLogger.log(log = testLog(message = "noisy message"))
            ConsoleLogger.log(log = testLog(message = "message").copy(tag = "FrameLoop"))
        }
        val job = launch { ConsoleLogger.collectInto() }
        advanceUntilIdle()
        ConsoleLogger.logs.map { it.message } shouldContainExactly listOf("important")
        repeat(times = 20) { ConsoleLogger.log(log = testLog(message = "noisy message")) }
        advanceUntilIdle()
        ConsoleLogger.logs.map { it.message } shouldContainExactly listOf("important")
        job.cancelAndJoin()
    }

    @Test
    fun `initial exclusions remove pending noise before shrinking the queue`() = runTest {
        ConsoleLogger.log(log = testLog(message = "important"))
        repeat(times = 20) { ConsoleLogger.log(log = testLog(message = "noise")) }
        val patterns = MutableStateFlow(value = listOf("noise"))
        val job = launch { ConsoleLogger.collectInto(maxEntries = 1, excludedPatterns = patterns) }
        advanceUntilIdle()
        ConsoleLogger.logs.map { it.message } shouldContainExactly listOf("important")
        job.cancelAndJoin()
    }

    @Test
    fun `changing admission preserves history and disabling only admits future matches`() = runTest {
        val patterns = MutableStateFlow(value = emptyList<String>())
        val job = launch { ConsoleLogger.collectInto(maxEntries = 5, excludedPatterns = patterns) }
        ConsoleLogger.log(log = testLog(message = "earlier noise"))
        advanceUntilIdle()
        patterns.value = listOf("noise")
        advanceUntilIdle()
        ConsoleLogger.log(log = testLog(message = "discarded noise"))
        ConsoleLogger.log(log = testLog(message = "useful"))
        advanceUntilIdle()
        patterns.value = emptyList()
        advanceUntilIdle()
        ConsoleLogger.log(log = testLog(message = "future noise"))
        advanceUntilIdle()
        ConsoleLogger.logs.map { it.message } shouldContainExactly listOf("earlier noise", "useful", "future noise")
        job.cancelAndJoin()
    }

    @Test
    fun `capture waits for saved rules and rules persist between collector lifetimes`() = runTest {
        val loaded = CompletableDeferred<Unit>()
        var captureStarted = false
        val patterns = flow {
            loaded.await()
            emit(value = listOf("noise"))
        }
        val job = launch {
            ConsoleLogger.collectInto(excludedPatterns = patterns) {
                captureStarted = true
                ConsoleLogger.log(log = testLog(message = "noise"))
                ConsoleLogger.log(log = testLog(message = "useful"))
            }
        }
        advanceUntilIdle()
        captureStarted shouldBe false
        loaded.complete(value = Unit)
        advanceUntilIdle()
        captureStarted shouldBe true
        ConsoleLogger.logs.map { it.message } shouldContainExactly listOf("useful")
        job.cancelAndJoin()
        ConsoleLogger.log(log = testLog(message = "noise between lifetimes"))
        val next = launch { ConsoleLogger.collectInto() }
        advanceUntilIdle()
        ConsoleLogger.logs.map { it.message } shouldContainExactly listOf("useful")
        next.cancelAndJoin()
    }

    private fun testLog(message: String): ConsoleLog = ConsoleLog(
        level = LogLevel.INFO, tag = "Test", message = message, timestamp = 1_700_000_000_000
    )
}
