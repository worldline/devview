package com.worldline.devview.networkmock.theme

import androidx.compose.ui.graphics.Color
import com.worldline.devview.networkmock.core.model.StatusCodeFamily
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class MockColorSchemeTest {

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
