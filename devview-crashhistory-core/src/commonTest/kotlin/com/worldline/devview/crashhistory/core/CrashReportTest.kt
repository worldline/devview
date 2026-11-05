package com.worldline.devview.crashhistory.core

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlin.test.Test

internal val testMetadata = CrashMetadata(
    platform = "android",
    osVersion = "14 (API 34)",
    deviceModel = "Google Pixel",
    appVersion = "1.2.3",
    buildNumber = "45"
)

internal fun testRecord(
    id: String = "1-abc",
    timestampMillis: Long = 1_700_000_000_000,
    kind: CrashRecordKind = CrashRecordKind.UNCAUGHT_EXCEPTION,
    isCrash: Boolean = true
) = CrashRecord(
    id = id,
    timestampMillis = timestampMillis,
    kind = kind,
    exceptionType = "java.lang.IllegalStateException",
    message = "boom",
    stackTrace = "at Foo.bar(Foo.kt:1)",
    threadName = "main",
    processId = 42,
    isCrash = isCrash,
    metadata = testMetadata
)

class CrashReportTest {
    @Test
    fun `report text contains type and message and metadata and stack trace`() {
        val text = testRecord().toReportText()

        text shouldContain "java.lang.IllegalStateException"
        text shouldContain "boom"
        text shouldContain "App: 1.2.3 (45)"
        text shouldContain "Device: Google Pixel, android 14 (API 34)"
        text shouldContain "Thread: main"
        text shouldContain "at Foo.bar(Foo.kt:1)"
    }

    @Test
    fun `MetricKit timestamp is labelled as the reporting period end`() {
        testRecord().copy(source = "metrickit").toReportText() shouldContain "Reporting period end:"
        testRecord().toReportText() shouldContain "Time:"
    }

    @Test
    fun `system terminations are labelled as not a crash`() {
        testRecord(isCrash = false).toReportText() shouldContain "not a crash"
        testRecord(isCrash = true).toReportText() shouldNotContain "not a crash"
    }

    @Test
    fun `json export is a round-trippable array`() {
        val records = listOf(testRecord(id = "1-a"), testRecord(id = "2-b"))

        val json = records.toJson()

        json shouldContain "\"id\": \"1-a\""
        json shouldContain "\"id\": \"2-b\""
        kotlinx.serialization.json.Json.decodeFromString(
            kotlinx.serialization.builtins.ListSerializer(CrashRecord.serializer()),
            json
        ) shouldBe records
    }
}
