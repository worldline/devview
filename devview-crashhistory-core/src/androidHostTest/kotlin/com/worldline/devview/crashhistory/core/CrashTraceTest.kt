package com.worldline.devview.crashhistory.core

import io.kotest.matchers.shouldBe
import java.io.StringReader
import kotlin.test.Test

class CrashTraceTest {
    @Test
    fun `bounded read preserves short traces and closes the reader`() {
        var closed = false
        val reader = object : StringReader("trace") {
            override fun close() { closed = true; super.close() }
        }
        readCrashTrace(reader) shouldBe "trace"
        closed shouldBe true
    }

    @Test
    fun `bounded read keeps one extra character for truncation detection`() {
        val trace = readCrashTrace(StringReader("x".repeat(CrashRecorder.MAX_STACK_TRACE_LENGTH + 100)))
        trace.length shouldBe CrashRecorder.MAX_STACK_TRACE_LENGTH + 1
        val record = exitInfoToRecord(
            reason = android.app.ApplicationExitInfo.REASON_ANR,
            description = null, timestampMillis = 1, processId = 1, trace = trace, metadata = testMetadata
        )!!
        record.stackTrace!!.length shouldBe CrashRecorder.MAX_STACK_TRACE_LENGTH
        record.truncated shouldBe true
    }
}
