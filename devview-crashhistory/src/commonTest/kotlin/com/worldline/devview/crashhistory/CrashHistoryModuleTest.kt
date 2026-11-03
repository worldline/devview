package com.worldline.devview.crashhistory

import com.worldline.devview.core.Section
import com.worldline.devview.crashhistory.core.CrashMetadata
import com.worldline.devview.crashhistory.core.CrashRecord
import com.worldline.devview.crashhistory.core.CrashRecordKind
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class CrashHistoryModuleTest {

    @Test
    fun `module exposes expected metadata and destinations`() {
        val module = CrashHistory()

        module.section shouldBe Section.LOGGING
        module.destinations.keys.shouldContain(CrashHistoryDestination.Main::class)
        module.destinations.keys.shouldContain(CrashHistoryDestination.Detail::class)
        module.entryDestination::class shouldBe CrashHistoryDestination.Main::class

        val main = module.destinations[CrashHistoryDestination.Main::class].shouldNotBeNull()
        main.title shouldBe "Crash History"
        main.actions shouldHaveSize 1
        main.actions.single().popup.shouldNotBeNull().apply {
            title shouldBe "Clear Crash History"
            confirmButton shouldBe "Clear"
            dismissButton shouldBe "Cancel"
        }
        module.destinations[CrashHistoryDestination.Detail::class].shouldNotBeNull().actions shouldHaveSize 0
    }

    @Test
    fun `constructor keeps maxRecords`() {
        CrashHistory(maxRecords = 7).maxRecords shouldBe 7
    }

    @Test
    fun `kind label tells confirmed crashes from system kills`() {
        val metadata = CrashMetadata(
            platform = "Android",
            osVersion = "14",
            deviceModel = "Pixel",
            appVersion = "1.0",
            buildNumber = "1"
        )
        fun record(kind: CrashRecordKind, isCrash: Boolean = true) = CrashRecord(
            id = "id",
            timestampMillis = 0L,
            kind = kind,
            isCrash = isCrash,
            metadata = metadata
        )

        record(kind = CrashRecordKind.UNCAUGHT_EXCEPTION).kindLabel shouldBe "Crash"
        record(kind = CrashRecordKind.APP_REPORTED).kindLabel shouldBe "Reported"
        record(kind = CrashRecordKind.SYSTEM_REPORTED).kindLabel shouldBe "System report"
        record(kind = CrashRecordKind.SYSTEM_REPORTED, isCrash = false).kindLabel shouldBe "Killed by system"
    }
}
