package com.worldline.devview.featureflip.components

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.worldline.devview.featureflip.model.Feature
import com.worldline.devview.featureflip.model.FeatureState
import kotlin.test.assertEquals
import org.junit.Test

class FeatureTriStateSwitchTest {

    @Test
    fun triStateSwitch_shows_all_states_and_notifies_state_changes() = runComposeUiTest {
        var latestState: FeatureState? = null

        setContent {
            val state = remember { mutableStateOf(FeatureState.REMOTE) }
            val feature = Feature.RemoteFeature(
                name = "new_checkout",
                description = null,
                defaultRemoteValue = false,
                state = state.value
            )

            FeatureTriStateSwitch(
                feature = feature,
                onStateChange = {
                    state.value = it
                    latestState = it
                }
            )
        }

        onNodeWithContentDescription(label = "REMOTE").assertIsDisplayed()
        onNodeWithContentDescription(label = "LOCAL_OFF").assertIsDisplayed()
        onNodeWithContentDescription(label = "LOCAL_ON").assertIsDisplayed()

        onNodeWithContentDescription(label = "LOCAL_ON").performClick()

        runOnIdle {
            assertEquals(FeatureState.LOCAL_ON, latestState)
        }
    }
}

