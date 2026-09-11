package com.worldline.devview.networkmock.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import com.worldline.devview.networkmock.core.model.OperationMockState
import kotlin.test.Test

class EndpointStateChipTest {

    @Test
    fun displaysNetworkLabel_forNetworkState() = runComposeUiTest {
        setChip(state = OperationMockState.Network)

        onNodeWithTag(testTag = "endpoint_state_chip_label_Network", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun displaysBareStatusCode_forMockState_200() = runComposeUiTest {
        setChip(state = OperationMockState.Mock(statusCode = 200, exampleName = "default"))

        onNodeWithTag(testTag = "endpoint_state_chip_label_200", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun displaysBareStatusCode_forMockState_404() = runComposeUiTest {
        setChip(state = OperationMockState.Mock(statusCode = 404, exampleName = "default"))

        onNodeWithTag(testTag = "endpoint_state_chip_label_404", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun displaysBareStatusCode_forMockState_500() = runComposeUiTest {
        setChip(state = OperationMockState.Mock(statusCode = 500, exampleName = "default"))

        onNodeWithTag(testTag = "endpoint_state_chip_label_500", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun displaysBareStatusCode_ignoringExampleName() = runComposeUiTest {
        setChip(state = OperationMockState.Mock(statusCode = 404, exampleName = "simple"))

        onNodeWithTag(testTag = "endpoint_state_chip_label_404", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun displaysExplicitLabel_whenOverridden() = runComposeUiTest {
        setChip(
            state = OperationMockState.Mock(statusCode = 404, exampleName = "simple"),
            label = "404 - simple"
        )

        onNodeWithTag(testTag = "endpoint_state_chip_label_404 - simple", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun chipIsDisplayed_forNetworkState() = runComposeUiTest {
        setChip(state = OperationMockState.Network)

        onNodeWithTag(testTag = "endpoint_state_chip", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun chipIsDisplayed_forMockState() = runComposeUiTest {
        setChip(state = OperationMockState.Mock(statusCode = 200, exampleName = "default"))

        onNodeWithTag(testTag = "endpoint_state_chip", useUnmergedTree = true).assertIsDisplayed()
    }

    private fun ComposeUiTest.setChip(
        state: OperationMockState,
        label: String? = null
    ) {
        setContent {
            MaterialTheme {
                if (label != null) {
                    EndpointStateChip(endpointMockState = state, label = label)
                } else {
                    EndpointStateChip(endpointMockState = state)
                }
            }
        }
    }
}
