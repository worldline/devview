package com.worldline.devview.networkmock.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class GlobalMockToggleTest {

    @Test
    fun switchIsDisplayed() = runComposeUiTest {
        setToggle(enabled = false)

        onNodeWithTag(testTag = "global_mock_toggle_switch").assertIsDisplayed()
    }

    @Test
    fun switchIsOn_whenEnabled() = runComposeUiTest {
        setToggle(enabled = true)

        onNodeWithTag(testTag = "global_mock_toggle_switch").assertIsOn()
    }

    @Test
    fun switchIsOff_whenDisabled() = runComposeUiTest {
        setToggle(enabled = false)

        onNodeWithTag(testTag = "global_mock_toggle_switch").assertIsOff()
    }

    @Test
    fun displaysEnabledMessage_whenEnabled() = runComposeUiTest {
        setToggle(enabled = true)

        onNodeWithText(text = "Mock responses enabled", substring = true).assertIsDisplayed()
    }

    @Test
    fun displaysDisabledMessage_whenDisabled() = runComposeUiTest {
        setToggle(enabled = false)

        onNodeWithText(text = "Mocking disabled", substring = true).assertIsDisplayed()
    }

    @Test
    fun toggleCallback_isInvoked_withTrue_whenSwitchedOn() = runComposeUiTest {
        var callbackValue: Boolean? = null

        setToggle(
            enabled = false,
            onToggle = { value -> callbackValue = value }
        )

        onNodeWithTag(testTag = "global_mock_toggle_switch").performClick()

        callbackValue shouldBe true
    }

    @Test
    fun toggleCallback_isInvoked_withFalse_whenSwitchedOff() = runComposeUiTest {
        var callbackValue: Boolean? = null

        setToggle(
            enabled = true,
            onToggle = { value -> callbackValue = value }
        )

        onNodeWithTag(testTag = "global_mock_toggle_switch").performClick()

        callbackValue shouldBe false
    }

    private fun ComposeUiTest.setToggle(
        enabled: Boolean,
        onToggle: (Boolean) -> Unit = {}
    ) {
        setContent {
            MaterialTheme {
                GlobalMockToggle(
                    enabled = enabled,
                    onToggle = onToggle
                )
            }
        }
    }
}

