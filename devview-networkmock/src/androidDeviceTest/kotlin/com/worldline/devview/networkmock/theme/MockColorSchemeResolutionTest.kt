package com.worldline.devview.networkmock.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.matchers.shouldBe
import org.junit.Test

class MockColorSchemeResolutionTest {

    @Test
    fun rememberMockColorScheme_honours_explicit_LocalMockColorScheme() = runComposeUiTest {
        var resolved: MockColorScheme? = null

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                CompositionLocalProvider(LocalMockColorScheme provides MockColorScheme.Light) {
                    resolved = rememberMockColorScheme()
                }
            }
        }
        waitForIdle()

        resolved shouldBe MockColorScheme.Light
    }

    @Test
    fun rememberMockColorScheme_falls_back_to_dark_when_none_provided_and_theme_is_dark() =
        runComposeUiTest {
            var resolved: MockColorScheme? = null

            setContent {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    resolved = rememberMockColorScheme()
                }
            }
            waitForIdle()

            resolved shouldBe MockColorScheme.Dark
        }
}
