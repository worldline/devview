package com.worldline.devview.networkmock.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.worldline.devview.networkmock.core.model.StatusCodeFamily
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertTrue

class MockColorSchemeTest {

    @Test
    fun `every built in text and container pair meets normal text contrast`() {
        listOf(MockColorScheme.Light, MockColorScheme.Dark).forEach { scheme ->
            val pairs = StatusCodeFamily.entries.map { scheme[it] } + scheme.network + scheme.failure
            pairs.forEach { colors ->
                val background = colors.container.luminance()
                val foreground = colors.content.luminance()
                val contrast = (maxOf(a = background, b = foreground) + 0.05f) /
                    (minOf(a = background, b = foreground) + 0.05f)
                assertTrue(actual = contrast >= 4.5f, message = "Status text contrast: $contrast")
            }
        }
    }

    @Test
    fun `get returns the matching slot for every family`() {
        val scheme = MockColorScheme.Light

        scheme[StatusCodeFamily.INFORMATIONAL] shouldBe scheme.informational
        scheme[StatusCodeFamily.SUCCESSFUL] shouldBe scheme.successful
        scheme[StatusCodeFamily.REDIRECTION] shouldBe scheme.redirection
        scheme[StatusCodeFamily.CLIENT_ERROR] shouldBe scheme.clientError
        scheme[StatusCodeFamily.SERVER_ERROR] shouldBe scheme.serverError
        scheme[StatusCodeFamily.UNKNOWN] shouldBe scheme.unknown
    }

    @Test
    fun `copy of one family leaves the others untouched`() {
        val original = MockColorScheme.Dark
        val overridden = original.copy(
            serverError = original.serverError.copy(content = Color.Magenta)
        )

        overridden.serverError.content shouldBe Color.Magenta
        overridden.informational shouldBe original.informational
        overridden.successful shouldBe original.successful
        overridden.redirection shouldBe original.redirection
        overridden.clientError shouldBe original.clientError
        overridden.unknown shouldBe original.unknown
        overridden.network shouldBe original.network
    }
}
