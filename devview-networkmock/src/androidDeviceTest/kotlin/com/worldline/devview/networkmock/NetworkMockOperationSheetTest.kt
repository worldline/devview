package com.worldline.devview.networkmock

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.worldline.devview.networkmock.core.model.HttpMethod
import com.worldline.devview.networkmock.core.model.MockResponse
import com.worldline.devview.networkmock.core.model.Operation
import com.worldline.devview.networkmock.core.model.OperationDescriptor
import com.worldline.devview.networkmock.core.model.OperationKey
import com.worldline.devview.networkmock.core.model.OperationMockState
import com.worldline.devview.networkmock.model.OperationUiModel
import com.worldline.devview.networkmock.viewmodel.OperationSheetState
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.collections.immutable.toPersistentList

class NetworkMockOperationSheetTest {

    private val response200 = MockResponse(
        statusCode = 200,
        exampleName = "default",
        displayName = "200 - default",
        content = "{}"
    )
    private val response404 = MockResponse(
        statusCode = 404,
        exampleName = "default",
        displayName = "404 - default",
        content = "{}"
    )

    @Test
    fun displaysOperationHeaderAndBothResponses() = runComposeUiTest {
        setPickerPage(currentState = OperationMockState.Network)

        onNodeWithText(text = "Get User").assertIsDisplayed()
        onNodeWithTag(testTag = "operation_sheet_network_item").assertIsDisplayed()
        onNodeWithTag(testTag = "mock_item_200_default").assertIsDisplayed()
        onNodeWithTag(testTag = "mock_item_404_default").assertIsDisplayed()
    }

    @Test
    fun tappingNetworkItem_selectsNetwork() = runComposeUiTest {
        var selected: MockResponse? = response200
        setPickerPage(
            currentState = OperationMockState.Mock(statusCode = 200, exampleName = "default"),
            onSelectResponse = { selected = it }
        )

        onNodeWithTag(testTag = "operation_sheet_network_item").performClick()

        selected shouldBe null
    }

    @Test
    fun tappingMockItem_selectsThatResponse() = runComposeUiTest {
        var selected: MockResponse? = null
        setPickerPage(
            currentState = OperationMockState.Network,
            onSelectResponse = { selected = it }
        )

        onNodeWithTag(testTag = "mock_item_404_default").performClick()

        selected shouldBe response404
    }

    @Test
    fun tappingCloseButton_invokesOnClose() = runComposeUiTest {
        var closed = false
        setPickerPage(currentState = OperationMockState.Network, onClose = { closed = true })

        onNodeWithTag(testTag = "operation_sheet_close_button").performClick()

        closed shouldBe true
    }

    @Test
    fun previewButton_isHidden_whenNothingMarked() = runComposeUiTest {
        setPickerPage(currentState = OperationMockState.Network)

        onNodeWithTag(testTag = "open_preview_button").assertIsNotDisplayed()
    }

    @Test
    fun togglingOneResponse_showsPreviewButton_labeledWithThatResponse() = runComposeUiTest {
        setPickerPage(currentState = OperationMockState.Network)

        onNodeWithTag(testTag = "mock_item_200_default_preview_toggle").performClick()

        onNodeWithText(text = "Preview 200 - default").assertIsDisplayed()
    }

    @Test
    fun togglingTwoResponses_showsCompareButton() = runComposeUiTest {
        setPickerPage(currentState = OperationMockState.Network)

        onNodeWithTag(testTag = "mock_item_200_default_preview_toggle").performClick()
        onNodeWithTag(testTag = "mock_item_404_default_preview_toggle").performClick()

        onNodeWithText(text = "Compare 2 responses").assertIsDisplayed()
    }

    @Test
    fun tappingPreviewButton_invokesOnOpenPreview() = runComposeUiTest {
        var opened = false
        setPickerPage(currentState = OperationMockState.Network, onOpenPreview = { opened = true })

        onNodeWithTag(testTag = "mock_item_200_default_preview_toggle").performClick()
        onNodeWithTag(testTag = "open_preview_button").performClick()

        opened shouldBe true
    }

    private fun ComposeUiTest.setPickerPage(
        currentState: OperationMockState,
        onSelectResponse: (MockResponse?) -> Unit = {},
        onOpenPreview: () -> Unit = {},
        onClose: () -> Unit = {}
    ) {
        setContent {
            MaterialTheme {
                var marked: PreviewSheetState by remember {
                    mutableStateOf(value = PreviewSheetState.Hidden)
                }
                OperationPickerPage(
                    content = OperationSheetState.Content(
                        operationUiModel = OperationUiModel(
                            descriptor = OperationDescriptor(
                                key = OperationKey(specId = "test", operationId = "getUser"),
                                config = Operation(
                                    operationId = "getUser",
                                    name = "Get User",
                                    path = "/api/users/{userId}",
                                    method = HttpMethod.Get
                                )
                            ),
                            currentState = currentState
                        ),
                        responses = listOf(response200, response404).toPersistentList()
                    ),
                    markedForPreview = marked,
                    onSelectResponse = onSelectResponse,
                    onTogglePreview = { response -> marked = marked.transition(response = response) },
                    onOpenPreview = onOpenPreview,
                    onClose = onClose
                )
            }
        }
    }
}
