package com.worldline.devview.crashhistory.core

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

class MetricKitMappingTest {
    private val sampleJson = """
        {
          "diagnosticMetaData": {
            "exceptionType": 6,
            "exceptionCode": 0,
            "signal": "6",
            "terminationReason": "Namespace SIGNAL, Code 6 Abort trap: 6",
            "appBuildVersion": "45"
          },
          "callStackTree": { "callStacks": [ { "threadAttributed": true } ], "callStackPerThread": false }
        }
    """.trimIndent()

    @Test
    fun `crash diagnostic maps to a system-reported record`() {
        val record = crashDiagnosticToRecord(sampleJson, timestampMillis = 123, metadata = testMetadata)

        record.shouldNotBeNull()
        record.kind shouldBe CrashRecordKind.SYSTEM_REPORTED
        record.source shouldBe "metrickit"
        record.exceptionType shouldBe "Crash (SIGABRT)"
        record.message shouldBe "Namespace SIGNAL, Code 6 Abort trap: 6"
        record.timestampMillis shouldBe 123
        record.stackTrace.shouldNotBeNull() shouldContain "callStacks"
    }

    @Test
    fun `missing termination reason falls back to the mach exception`() {
        val record = crashDiagnosticToRecord(
            """{"diagnosticMetaData":{"exceptionType":1,"exceptionCode":2}}""",
            timestampMillis = 0,
            metadata = testMetadata
        )

        record.shouldNotBeNull().message shouldBe "Mach exception 1, code 2"
    }

    @Test
    fun `invalid json is ignored`() {
        crashDiagnosticToRecord("not json", timestampMillis = 0, metadata = testMetadata).shouldBeNull()
    }
}
