package com.worldline.devview.networkmock.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.worldline.devview.networkmock.core.model.HttpMethod
import com.worldline.devview.networkmock.core.model.Operation
import com.worldline.devview.networkmock.core.model.OperationDescriptor
import com.worldline.devview.networkmock.core.model.OperationKey
import com.worldline.devview.networkmock.core.model.OperationMockState
import com.worldline.devview.networkmock.model.OperationUiModel
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

        onNodeWithTag(
            testTag = "endpoint_method_getUser",
            useUnmergedTree = true
        ).assertIsDisplayed()
    }

    @Test
    fun displaysEndpointPath() = runComposeUiTest {
        setEndpointCard(endpoint = networkEndpoint())

        onNodeWithTag(testTag = "endpoint_path_getUser", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun displaysStateChip() = runComposeUiTest {
        setEndpointCard(endpoint = networkEndpoint())

        onNodeWithTag(
            testTag = "endpoint_state_chip_getUser",
            useUnmergedTree = true
        ).assertIsDisplayed()
    }

    @Test
    fun clickInvokesOpenBottomSheetCallback() = runComposeUiTest {
        var clicked = false

        setEndpointCard(
            endpoint = networkEndpoint(),
            openEndpointDetails = { clicked = true }
        )

        onNodeWithTag(
            testTag = "endpoint_state_chip_getUser",
            useUnmergedTree = true
        ).performClick()

        clicked shouldBe true
    }

    @Test
    fun versionChip_isDisplayed_whenOperationHasAVersion() = runComposeUiTest {
        setEndpointCard(endpoint = networkEndpoint(version = "v1"))

        onNodeWithTag(
            testTag = "endpoint_version_getUser",
            useUnmergedTree = true
        ).assertIsDisplayed()
    }

    @Test
    fun versionChip_isNotDisplayed_whenOperationHasNoVersion() = runComposeUiTest {
        setEndpointCard(endpoint = networkEndpoint(version = null))

        onAllNodesWithTag(
            testTag = "endpoint_version_getUser",
            useUnmergedTree = true
        ).assertCountEquals(expectedSize = 0)
    }

    @Test
    fun stateChipIsDisplayed_forNetworkState() = runComposeUiTest {
        setEndpointCard(endpoint = networkEndpoint())

        onNodeWithTag(
            testTag = "endpoint_state_chip_getUser",
            useUnmergedTree = true
        ).assertIsDisplayed()
    }

    @Test
    fun stateChipIsDisplayed_forMockState() = runComposeUiTest {
        setEndpointCard(endpoint = mockEndpoint())

        onNodeWithTag(
            testTag = "endpoint_state_chip_getUser",
            useUnmergedTree = true
        ).assertIsDisplayed()
    }

    private fun networkEndpoint(version: String? = null) = OperationUiModel(
        descriptor = OperationDescriptor(
            key = OperationKey(specId = "test", operationId = "getUser"),
            config = Operation(
                operationId = "getUser",
                name = "Get User",
                path = "/api/users/{userId}",
                method = HttpMethod.Get,
                version = version
            )
        ),
        currentState = OperationMockState.Network
    )

    private fun mockEndpoint() = OperationUiModel(
        descriptor = OperationDescriptor(
            key = OperationKey(specId = "test", operationId = "getUser"),
            config = Operation(
                operationId = "getUser",
                name = "Get User",
                path = "/api/users/{userId}",
                method = HttpMethod.Get
            )
        ),
        currentState = OperationMockState.Mock(statusCode = 200, exampleName = "default")
    )

    private fun ComposeUiTest.setEndpointCard(
        endpoint: OperationUiModel,
        openEndpointDetails: () -> Unit = {}
    ) {
        setContent {
            MaterialTheme {
                EndpointCard(
                    endpoint = endpoint,
                    openEndpointDetails = openEndpointDetails
                )
            }
        }
    }
}
