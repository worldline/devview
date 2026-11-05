package com.worldline.devview.crashhistory.core

import android.app.ApplicationExitInfo
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ExitInfoMappingTest {
    private fun map(reason: Int, trace: String? = null) = exitInfoToRecord(
        reason = reason,
        description = "desc",
        timestampMillis = 1_000,
        processId = 7,
        trace = trace,
        metadata = testMetadata
    )

    @Test
    fun `crash reasons are system-reported crashes`() {
        listOf(
            ApplicationExitInfo.REASON_CRASH to "exit-info:CRASH",
            ApplicationExitInfo.REASON_CRASH_NATIVE to "exit-info:CRASH_NATIVE",
            ApplicationExitInfo.REASON_ANR to "exit-info:ANR",
            ApplicationExitInfo.REASON_INITIALIZATION_FAILURE to "exit-info:INITIALIZATION_FAILURE"
        ).forEach { (reason, source) ->
            val record = map(reason).shouldNotBeNull()
            record.kind shouldBe CrashRecordKind.SYSTEM_REPORTED
            record.isCrash shouldBe true
            record.source shouldBe source
            record.processId shouldBe 7
        }
    }

    @Test
    fun `system kills are flagged as not a crash`() {
        listOf(ApplicationExitInfo.REASON_LOW_MEMORY, ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE)
            .forEach { map(it).shouldNotBeNull().isCrash shouldBe false }
    }

    @Test
    fun `routine exits are ignored`() {
        listOf(
            ApplicationExitInfo.REASON_USER_REQUESTED,
            ApplicationExitInfo.REASON_EXIT_SELF,
            ApplicationExitInfo.REASON_USER_STOPPED,
            ApplicationExitInfo.REASON_SIGNALED,
            ApplicationExitInfo.REASON_UNKNOWN
        ).forEach { map(it).shouldBeNull() }
    }

    @Test
    fun `exit IDs are stable and include the reason`() {
        val first = map(ApplicationExitInfo.REASON_ANR).shouldNotBeNull()
        first.id shouldBe "exit-7-1000-${ApplicationExitInfo.REASON_ANR}"
        map(ApplicationExitInfo.REASON_ANR).shouldNotBeNull().id shouldBe first.id
        isAlreadyRecorded(first, listOf(first)) shouldBe true
        (map(ApplicationExitInfo.REASON_CRASH).shouldNotBeNull().id == first.id) shouldBe false
    }

    @Test
    fun `the trace becomes the stack trace`() {
        map(ApplicationExitInfo.REASON_ANR, trace = "main thread blocked").shouldNotBeNull().stackTrace shouldBe
            "main thread blocked"
    }

    @Test
    fun `a java crash already recorded in-process is a duplicate`() {
        val inProcess = testRecord(timestampMillis = 1_500).copy(processId = 7)
        val systemCrash = map(ApplicationExitInfo.REASON_CRASH).shouldNotBeNull()
        val systemAnr = map(ApplicationExitInfo.REASON_ANR).shouldNotBeNull()

        isAlreadyRecorded(systemCrash, listOf(inProcess)) shouldBe true
        isAlreadyRecorded(systemCrash, emptyList()) shouldBe false
        isAlreadyRecorded(systemCrash, listOf(inProcess.copy(processId = 8))) shouldBe false
        isAlreadyRecorded(systemAnr, listOf(inProcess)) shouldBe false
    }
}
