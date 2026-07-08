package com.worldline.devview.networkmock.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import com.worldline.devview.networkmock.core.model.EndpointMockState
import kotlin.test.Test

class EndpointStateChipTest {

    @Test
    fun displaysNetworkLabel_forNetworkState() = runComposeUiTest {
        setChip(state = EndpointMockState.Network)

        onNodeWithTag(testTag = "endpoint_state_chip_label_Network", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun displaysStatusCode_forMockState_200() = runComposeUiTest {
        setChip(state = EndpointMockState.Mock(responseFile = "getUser-200.json"))

        onNodeWithTag(testTag = "endpoint_state_chip_label_200", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun displaysStatusCode_forMockState_404() = runComposeUiTest {
        setChip(state = EndpointMockState.Mock(responseFile = "getUser-404.json"))

        onNodeWithTag(testTag = "endpoint_state_chip_label_404", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun displaysStatusCode_forMockState_500() = runComposeUiTest {
        setChip(state = EndpointMockState.Mock(responseFile = "getUser-500.json"))

        onNodeWithTag(testTag = "endpoint_state_chip_label_500", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun displaysStatusCode_forMockState_withSuffix() = runComposeUiTest {
        setChip(state = EndpointMockState.Mock(responseFile = "getUser-404-simple.json"))

        onNodeWithTag(testTag = "endpoint_state_chip_label_404", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun chipIsDisplayed_forNetworkState() = runComposeUiTest {
        setChip(state = EndpointMockState.Network)

        onNodeWithTag(testTag = "endpoint_state_chip", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun chipIsDisplayed_forMockState() = runComposeUiTest {
        setChip(state = EndpointMockState.Mock(responseFile = "getUser-200.json"))

        onNodeWithTag(testTag = "endpoint_state_chip", useUnmergedTree = true).assertIsDisplayed()
    }

    private fun ComposeUiTest.setChip(
        state: EndpointMockState
    ) {
        setContent {
            MaterialTheme {
                EndpointStateChip(
                    endpointMockState = state
                )
            }
        }
    }
}
