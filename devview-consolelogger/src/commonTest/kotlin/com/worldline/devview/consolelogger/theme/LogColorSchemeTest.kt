package com.worldline.devview.consolelogger.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.worldline.devview.consolelogger.model.LogLevel
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class LogColorSchemeTest {

    @Test
    fun `every severity label contrasts with its default theme surface`() {
        listOf(LogColorScheme.Light to Color(0xFFF7F7F7), LogColorScheme.Dark to Color(0xFF242424)).forEach { (scheme, surface) ->
            LogLevel.entries.forEach { level ->
                val label = scheme[level].label.luminance()
                val background = surface.luminance()
                ((maxOf(label, background) + 0.05f) / (minOf(label, background) + 0.05f) >= 4.5f) shouldBe true
            }
        }
    }

    @Test
    fun `get returns the matching slot for every level`() {
        val scheme = LogColorScheme.Light

        scheme[LogLevel.VERBOSE] shouldBe scheme.verbose
        scheme[LogLevel.DEBUG] shouldBe scheme.debug
        scheme[LogLevel.INFO] shouldBe scheme.info
        scheme[LogLevel.WARNING] shouldBe scheme.warning
        scheme[LogLevel.ERROR] shouldBe scheme.error
        scheme[LogLevel.ASSERT] shouldBe scheme.assert
        scheme[LogLevel.UNKNOWN] shouldBe scheme.unknown
    }

    @Test
    fun `copy of one level leaves the others untouched`() {
        val original = LogColorScheme.Dark
        val overridden = original.copy(
            error = original.error.copy(label = Color.Magenta)
        )

        overridden.error.label shouldBe Color.Magenta
        overridden.verbose shouldBe original.verbose
        overridden.debug shouldBe original.debug
        overridden.info shouldBe original.info
        overridden.warning shouldBe original.warning
        overridden.assert shouldBe original.assert
        overridden.unknown shouldBe original.unknown
    }
}
