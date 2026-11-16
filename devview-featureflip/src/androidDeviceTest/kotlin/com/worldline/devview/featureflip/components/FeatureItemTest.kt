package com.worldline.devview.featureflip.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.worldline.devview.featureflip.model.Feature
import com.worldline.devview.featureflip.model.FeatureState
import org.junit.Assert.assertEquals
import org.junit.Test

class FeatureItemTest {
    @Test
    fun remote_choices_report_states_and_expose_selection() = runComposeUiTest {
        var received: FeatureState? = null
        setContent {
            var state by remember { mutableStateOf(value = FeatureState.REMOTE) }
            MaterialTheme {
                FeatureItem(
                    feature = Feature.RemoteFeature(name = "checkout", description = null, defaultRemoteValue = false, state = state),
                    onStateChange = { received = it; state = it }
                )
            }
        }
        onNodeWithContentDescription(label = "checkout: Remote").assertIsSelected()
        onNodeWithText(text = "Remote value: off").assertExists()
        listOf("Off" to FeatureState.LOCAL_OFF, "On" to FeatureState.LOCAL_ON, "Remote" to FeatureState.REMOTE)
            .forEach { (label, state) ->
                onNodeWithContentDescription(label = "checkout: $label").performClick().assertIsSelected()
                runOnIdle { assertEquals(state, received) }
            }
        onNodeWithTag(testTag = "feature_type_checkout").assertDoesNotExist()
    }

    @Test
    fun local_switch_is_named_and_reports_boolean_state() = runComposeUiTest {
        var received: FeatureState? = null
        setContent {
            var enabled by remember { mutableStateOf(value = false) }
            MaterialTheme {
                FeatureItem(
                    feature = Feature.LocalFeature(name = "dark_mode", description = "Theme", isEnabled = enabled),
                    onStateChange = { received = it; enabled = it == FeatureState.LOCAL_ON }
                )
            }
        }
        onNodeWithContentDescription(label = "dark_mode").performClick().assertIsOn()
        runOnIdle { assertEquals(FeatureState.LOCAL_ON, received) }
        onNodeWithTag(testTag = "feature_type_dark_mode").assertDoesNotExist()
        onNodeWithText(text = "Remote value: off").assertDoesNotExist()
    }
}
