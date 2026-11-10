package com.worldline.devview.crashhistory.core

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class MetricKitMappingTest {
    private val diagnostic = SystemCrashDiagnostic(
        occurrenceId = "a".repeat(64),
        periodEndMillis = 123,
        signal = 6,
        terminationReason = "Namespace SIGNAL, Code 6 Abort trap: 6",
        callStackTreeJson = "{\"callStacks\":[]}",
        appVersion = "1.0",
        appBuildVersion = "12",
        osVersion = "18.0",
        deviceType = "iPhone17,1"
    )

    @Test
    fun `diagnostic uses stable identity and historical metadata`() {
        val record = crashDiagnosticToRecord(diagnostic, metadata = testMetadata).shouldNotBeNull()

        record.id shouldBe "metrickit-${diagnostic.occurrenceId}"
        crashDiagnosticToRecord(diagnostic, metadata = testMetadata) shouldBe record
        record.kind shouldBe CrashRecordKind.SYSTEM_REPORTED
        record.source shouldBe "metrickit"
        record.exceptionType shouldBe "Crash (SIGABRT)"
        record.message shouldBe diagnostic.terminationReason
        record.timestampMillis shouldBe diagnostic.periodEndMillis
        record.stackTrace shouldBe diagnostic.callStackTreeJson
        record.metadata.appVersion shouldBe "1.0"
        record.metadata.buildNumber shouldBe "12"
        record.metadata.osVersion shouldBe "18.0"
        record.metadata.deviceModel shouldBe "iPhone17,1"
    }

    @Test
    fun `Darwin signal numbers map correctly including unknown signals`() {
        val signals = mapOf(7 to "SIGEMT", 10 to "SIGBUS", 12 to "SIGSYS", 999 to "signal 999")
        signals.forEach { (number, name) ->
            crashDiagnosticToRecord(diagnostic.copy(signal = number), metadata = testMetadata)
                .shouldNotBeNull().exceptionType shouldBe "Crash ($name)"
        }
    }

    @Test
    fun `missing termination reason falls back to the mach exception`() {
        val value = diagnostic.copy(terminationReason = null, signal = null, exceptionType = 1, exceptionCode = "2")
        val record = crashDiagnosticToRecord(value, metadata = testMetadata).shouldNotBeNull()
        record.message shouldBe "Mach exception 1, code 2"
        record.exceptionType shouldBe "Crash"
    }

    @Test
    fun `unknown historical versions are not replaced with current launch metadata`() {
        val value = SystemCrashDiagnostic(occurrenceId = diagnostic.occurrenceId, periodEndMillis = 0)
        val metadata = crashDiagnosticToRecord(value, metadata = testMetadata).shouldNotBeNull().metadata
        metadata.appVersion shouldBe "unknown"
        metadata.buildNumber shouldBe "unknown"
        metadata.osVersion shouldBe "unknown"
        metadata.deviceModel shouldBe "unknown"
    }

    @Test
    fun `invalid occurrence ids cannot become filenames`() {
        listOf("", "../escape", "a".repeat(63), "z".repeat(64)).forEach { id ->
            crashDiagnosticToRecord(diagnostic.copy(occurrenceId = id), metadata = testMetadata).shouldBeNull()
        }
    }

    @Test
    fun `native diagnostics still honor crash record size limits`() {
        val value = diagnostic.copy(
            terminationReason = "m".repeat(CrashRecorder.MAX_MESSAGE_LENGTH + 1),
            callStackTreeJson = "s".repeat(CrashRecorder.MAX_STACK_TRACE_LENGTH + 1)
        )
        val record = crashDiagnosticToRecord(value, metadata = testMetadata).shouldNotBeNull()
        record.message?.length shouldBe CrashRecorder.MAX_MESSAGE_LENGTH
        record.stackTrace?.length shouldBe CrashRecorder.MAX_STACK_TRACE_LENGTH
        record.truncated shouldBe true
    }
}
