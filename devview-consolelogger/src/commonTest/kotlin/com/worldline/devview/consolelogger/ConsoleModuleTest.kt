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
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

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

        mainMetadata.title shouldBe null
        module.moduleName shouldBe "Console"
        mainMetadata.actions shouldHaveSize 1

        val clearAction = mainMetadata.actions.single()

        clearAction.popup.shouldNotBeNull().apply {
            title shouldBe "Clear Logs"
            confirmButton shouldBe "Clear"
            dismissButton shouldBe "Cancel"
        }
    }

    @Test
    fun `clear action removes existing logs`() = runTest {
        val module = Console()

        val clearAction = module.destinations[ConsoleDestination.Main::class]
            .shouldNotBeNull()
            .actions
            .single()

        val collector = launch { ConsoleLogger.collectInto() }
        ConsoleLogger.log(log =
            ConsoleLog(level = LogLevel.INFO, tag = "tag", message = "message", timestamp = 1_700_000_000_000)
        )

        advanceUntilIdle()
        ConsoleLogger.hasLogs shouldBe true
        clearAction.action(ConsoleDestination.Main)

        ConsoleLogger.hasLogs shouldBe false
        collector.cancelAndJoin()
    }

    @Test
    fun `constructor keeps maxEntries and captureNativeConsole`() {
        val module = Console(maxEntries = 42, captureNativeConsole = false)

        module.maxEntries shouldBe 42
        module.captureNativeConsole shouldBe false
    }
}
