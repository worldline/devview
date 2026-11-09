package com.worldline.devview.networkmock.components

import androidx.compose.runtime.BroadcastFrameClock
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LineWidthMeasurementTest {
    @Test
    fun widthsYieldBetweenBatchesAndPrioritizeNewlyVisibleLines() = runTest {
        val clock = BroadcastFrameClock()
        val measured = mutableListOf<String>()
        val widths = mutableListOf<Int>()
        var visible = listOf(0)
        val job = launch(context = clock) {
            measureLineWidths(
                lines = List(size = 5_000) { it.toString() },
                visibleIndices = { visible },
                measure = { measured.add(it); it.toInt() },
                onWidth = { widths.add(it) }
            )
        }
        runCurrent()
        assertTrue(measured.isEmpty(), "Initial composition must not scan the body")
        clock.sendFrame(timeNanos = 1)
        runCurrent()
        assertEquals(expected = 32, actual = measured.size)
        assertFalse(job.isCompleted)
        visible = listOf(4_999)
        clock.sendFrame(timeNanos = 2)
        runCurrent()
        assertEquals(expected = "4999", actual = measured[32])
        assertEquals(expected = 4_999, actual = widths.last())
        var frame = 3L
        while (!job.isCompleted) { clock.sendFrame(timeNanos = frame++); runCurrent() }
        assertEquals(expected = 5_000, actual = measured.toSet().size)
        assertEquals(expected = 5_000, actual = measured.size)
    }

    @Test
    fun cancelledInputCannotPublishWidthsForItsReplacement() = runTest {
        val clock = BroadcastFrameClock()
        val widths = mutableListOf<Int>()
        val old = launch(context = clock) {
            measureLineWidths(List(size = 100) { "old" }, { emptyList() }, { 1_000 }, { widths.add(it) })
        }
        runCurrent()
        clock.sendFrame(timeNanos = 1)
        runCurrent()
        assertEquals(expected = listOf(1_000), actual = widths)
        old.cancel()
        runCurrent()
        val replacement = launch(context = clock) {
            measureLineWidths(listOf("new"), { listOf(0) }, { 10 }, { widths.add(it) })
        }
        runCurrent()
        clock.sendFrame(timeNanos = 2)
        runCurrent()
        assertTrue(replacement.isCompleted)
        assertEquals(expected = listOf(1_000, 10), actual = widths)
    }

    @Test
    fun emptyBodiesNeedNoFrameOrMeasurement() = runTest {
        measureLineWidths(emptyList(), { error("No viewport needed") }, { error("No line") }, { error("No width") })
    }
}
