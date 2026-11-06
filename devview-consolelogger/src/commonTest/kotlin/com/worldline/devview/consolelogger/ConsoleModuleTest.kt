package com.worldline.devview.consolelogger

import com.worldline.devview.consolelogger.model.ConsoleLog
import com.worldline.devview.consolelogger.model.LogLevel
import com.worldline.devview.core.Section
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.AfterTest
import kotlin.test.Test

class ConsoleModuleTest {

    @AfterTest
    fun tearDown() {
        ConsoleLogger.clear()
    }

    @Test
    fun `console module exposes expected metadata and destination action`() {
        val module = Console()

        module.section shouldBe Section.LOGGING
        module.destinations.keys.shouldContain(ConsoleDestination.Main::class)
        module.entryDestination::class shouldBe ConsoleDestination.Main::class

        val mainMetadata = module.destinations[ConsoleDestination.Main::class].shouldNotBeNull()

        mainMetadata.title shouldBe "Console"
        mainMetadata.actions shouldHaveSize 1

        val clearAction = mainMetadata.actions.single()

        clearAction.popup.shouldNotBeNull().apply {
            title shouldBe "Clear Logs"
            confirmButton shouldBe "Clear"
            dismissButton shouldBe "Cancel"
        }
    }

    @Test
    fun `clear action removes existing logs`() {
        val module = Console()

        val clearAction = module.destinations[ConsoleDestination.Main::class]
            .shouldNotBeNull()
            .actions
            .single()

        ConsoleLogger.logs.add(
            ConsoleLog(level = LogLevel.INFO, tag = "tag", message = "message", timestamp = 1_700_000_000_000)
        )

        clearAction.action(ConsoleDestination.Main)

        ConsoleLogger.hasLogs shouldBe false
    }

    @Test
    fun `constructor keeps maxEntries and captureNativeConsole`() {
        val module = Console(maxEntries = 42, captureNativeConsole = false)

        module.maxEntries shouldBe 42
        module.captureNativeConsole shouldBe false
    }
}
