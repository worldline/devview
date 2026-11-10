package com.worldline.devview.consolelogger

import com.worldline.devview.consolelogger.model.ConsoleLog
import com.worldline.devview.consolelogger.model.LogLevel
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test

class ConsoleCaptureTest {
    @Test(timeout = 15_000)
    fun cancelling_an_idle_native_reader_releases_the_collector_for_manual_logs() = runBlocking {
        ConsoleLogger.clear()
        val process = ProcessBuilder(
            "/system/bin/sh", "-c",
            "printf 'I/DevViewTest( 123): capture ready\n'; exec sleep 60"
        ).redirectErrorStream(true).start()
        val first = launch(context = Dispatchers.Default) {
            ConsoleLogger.collectInto(capture = { collectLogcatProcess(process = process) })
        }
        val second = launch(context = Dispatchers.Default, start = CoroutineStart.LAZY) { ConsoleLogger.collectInto() }
        try {
            withTimeout(timeMillis = 5_000) {
                while (ConsoleLogger.logs.none { it.message == "capture ready" }) delay(timeMillis = 10)
            }
            process.isAlive shouldBe true
            second.start()
            withTimeout(timeMillis = 5_000) { first.cancelAndJoin() }
            ConsoleLogger.log(log = ConsoleLog(
                level = LogLevel.INFO,
                tag = "Test", message = "after cancellation", timestamp = 0
            ))
            withTimeout(timeMillis = 5_000) {
                while (ConsoleLogger.logs.none { it.message == "after cancellation" }) delay(timeMillis = 10)
            }
        } finally {
            process.destroy()
            first.cancelAndJoin()
            second.cancelAndJoin()
            ConsoleLogger.clear()
        }
    }
}
