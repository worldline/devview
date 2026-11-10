package com.worldline.devview.consolelogger

import com.worldline.devview.consolelogger.model.LogLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import platform.posix.F_GETFL
import platform.posix.F_SETFL
import platform.posix.O_NONBLOCK
import platform.posix.close
import platform.posix.dup
import platform.posix.fcntl
import platform.posix.pipe
import platform.posix.read
import platform.posix.write

@OptIn(ExperimentalForeignApi::class)
class ConsoleCaptureTest {
    @Test
    fun idle_capture_cancels_and_restores_the_original_destination_on_reentry() = runBlocking {
        val (input, output) = memScoped {
            val descriptors = allocArray<IntVar>(2)
            assertEquals(expected = 0, actual = pipe(descriptors))
            descriptors[0] to descriptors[1]
        }
        val target = dup(output)
        try {
            assertTrue(actual = target >= 0)
            val inputFlags = fcntl(input, F_GETFL)
            assertTrue(actual = inputFlags >= 0)
            assertEquals(expected = 0, actual = fcntl(input, F_SETFL, inputFlags or O_NONBLOCK))
            repeat(times = 2) {
                // Darwin can add a write-history flag after the preceding sentinel write.
                val originalFlags = fcntl(target, F_GETFL)
                assertTrue(actual = originalFlags >= 0 && originalFlags and O_NONBLOCK == 0)
                val capture = launch { redirectStream(fd = target, level = LogLevel.INFO) }
                try {
                    withTimeout(timeMillis = 5_000) {
                        while (fcntl(target, F_GETFL) and O_NONBLOCK == 0) delay(timeMillis = 10)
                    }
                    assertEquals(expected = originalFlags, actual = fcntl(output, F_GETFL))
                    withTimeout(timeMillis = 5_000) { capture.cancelAndJoin() }
                    assertEquals(expected = originalFlags, actual = fcntl(target, F_GETFL))
                    val sentinel = "restored".encodeToByteArray()
                    val received = ByteArray(size = sentinel.size)
                    sentinel.usePinned { pinned ->
                        assertEquals(
                            expected = sentinel.size.toLong(),
                            actual = write(
                                target,
                                pinned.addressOf(index = 0),
                                sentinel.size.convert()
                            )
                        )
                    }
                    received.usePinned { pinned ->
                        assertEquals(
                            expected = received.size.toLong(),
                            actual = read(
                                input,
                                pinned.addressOf(index = 0),
                                received.size.convert()
                            )
                        )
                    }
                    assertEquals(expected = "restored", actual = received.decodeToString())
                } finally {
                    cancelCapture(capture = capture, target = target)
                }
            }
        } finally {
            if (target >= 0) close(target)
            close(input)
            close(output)
        }
    }

    private suspend fun cancelCapture(capture: Job, target: Int) {
        capture.cancel()
        if (!capture.isCompleted) {
            // Wake a regressed blocking reader so a timeout can finish cleanup.
            byteArrayOf(10).usePinned { pinned ->
                write(target, pinned.addressOf(index = 0), 1.convert())
            }
        }
        capture.join()
    }
}
