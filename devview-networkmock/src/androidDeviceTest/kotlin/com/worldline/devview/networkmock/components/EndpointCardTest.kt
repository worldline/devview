package com.worldline.devview.networkmock.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.worldline.devview.networkmock.core.model.EndpointConfig
import com.worldline.devview.networkmock.core.model.EndpointDescriptor
import com.worldline.devview.networkmock.core.model.EndpointMockState
import com.worldline.devview.networkmock.core.model.MockResponse
import com.worldline.devview.networkmock.viewmodel.EndpointUiModel
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class EndpointCardTest {

    @Test
    fun displaysEndpointName() = runComposeUiTest {
        setEndpointCard(endpoint = networkEndpoint())

        onNodeWithTag(testTag = "endpoint_name_getUser", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun displaysHttpMethod() = runComposeUiTest {
        setEndpointCard(endpoint = networkEndpoint())

        onNodeWithTag(testTag = "endpoint_method_getUser", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun displaysEndpointPath() = runComposeUiTest {
        setEndpointCard(endpoint = networkEndpoint())

        onNodeWithTag(testTag = "endpoint_path_getUser", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun displaysStateChip() = runComposeUiTest {
        setEndpointCard(endpoint = networkEndpoint())

        onNodeWithTag(testTag = "endpoint_state_chip_getUser", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun clickInvokesOpenBottomSheetCallback() = runComposeUiTest {
        var clicked = false

        setEndpointCard(
            endpoint = networkEndpoint(),
            openEndpointBottomSheet = { clicked = true }
        )

        onNodeWithTag(testTag = "endpoint_state_chip_getUser", useUnmergedTree = true).performClick()

        clicked shouldBe true
    }

    @Test
    fun showFileName_isFalse_stateTextIsNotDisplayed() = runComposeUiTest {
        setEndpointCard(
            endpoint = mockEndpoint(),
            showFileName = false
        )

        onNodeWithTag(testTag = "endpoint_state_getUser", useUnmergedTree = true).assertIsNotDisplayed()
    }

    @Test
    fun showFileName_isTrue_stateTextIsDisplayed() = runComposeUiTest {
        setEndpointCard(
            endpoint = mockEndpoint(),
            showFileName = true
        )

        onNodeWithTag(testTag = "endpoint_state_getUser", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun stateChipIsDisplayed_forNetworkState() = runComposeUiTest {
        setEndpointCard(endpoint = networkEndpoint())

        onNodeWithTag(testTag = "endpoint_state_chip_getUser", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun stateChipIsDisplayed_forMockState() = runComposeUiTest {
        setEndpointCard(endpoint = mockEndpoint())

        onNodeWithTag(testTag = "endpoint_state_chip_getUser", useUnmergedTree = true).assertIsDisplayed()
    }

    private fun networkEndpoint() = EndpointUiModel(
        descriptor = EndpointDescriptor(
            hostId = "staging",
            endpointId = "getUser",
            config = EndpointConfig(
                id = "getUser",
                name = "Get User",
                path = "/api/users/{userId}",
                method = "GET"
            ),
            availableResponses = listOf(
                MockResponse(
                    statusCode = 200,
                    fileName = "getUser-200.json",
                    displayName = "Success (200)",
                    content = "{}"
                )
            )
        ),
        currentState = EndpointMockState.Network
    )

    private fun mockEndpoint() = EndpointUiModel(
        descriptor = EndpointDescriptor(
            hostId = "staging",
            endpointId = "getUser",
            config = EndpointConfig(
                id = "getUser",
                name = "Get User",
                path = "/api/users/{userId}",
                method = "GET"
            ),
            availableResponses = listOf(
                MockResponse(
                    statusCode = 200,
                    fileName = "getUser-200.json",
                    displayName = "Success (200)",
                    content = "{}"
                )
            )
        ),
        currentState = EndpointMockState.Mock(responseFile = "getUser-200.json")
    )

    private fun ComposeUiTest.setEndpointCard(
        endpoint: EndpointUiModel,
        openEndpointBottomSheet: () -> Unit = {},
        showFileName: Boolean = false
    ) {
        setContent {
            MaterialTheme {
                EndpointCard(
                    endpoint = endpoint,
                    openEndpointBottomSheet = openEndpointBottomSheet,
                    showFileName = showFileName
                )
            }
        }
    }
}
