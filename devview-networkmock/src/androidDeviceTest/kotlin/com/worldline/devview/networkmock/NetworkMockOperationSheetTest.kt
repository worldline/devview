package com.worldline.devview.networkmock

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.worldline.devview.networkmock.core.model.FailureKind
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
        mediaType = "application/json",
        exampleName = "default",
        displayName = "200 - default",
        content = "{}"
    )
    private val response404 = MockResponse(
        statusCode = 404,
        mediaType = "application/json",
        exampleName = "default",
        displayName = "404 - default",
        content = "{}"
    )

    @Test
    fun displaysOperationHeaderAndBothResponses() = runComposeUiTest {
        setPickerPage(currentState = OperationMockState.Network)

        onNodeWithText(text = "Get User").assertIsDisplayed()
        onNodeWithTag(testTag = "operation_sheet_network_item").assertIsDisplayed()
        onNodeWithTag(testTag = "mock_item_200_application/json_default").assertIsDisplayed()
        onNodeWithTag(testTag = "mock_item_404_application/json_default").assertIsDisplayed()
    }

    @Test
    fun groupsResponsesByStatusFamily_showingAStickyHeaderPerFamily() = runComposeUiTest {
        // response200 (2xx) and response404 (4xx) fall into distinct StatusCodeFamily groups -
        // each must get its own sticky header, not be lumped under one.
        setPickerPage(currentState = OperationMockState.Network)

        onNodeWithText(text = "SUCCESSFUL MOCKS").assertIsDisplayed()
        onNodeWithText(text = "CLIENT ERROR MOCKS").assertIsDisplayed()
    }

    @Test
    fun tappingNetworkItem_selectsNetwork() = runComposeUiTest {
        var selected: MockResponse? = response200
        setPickerPage(
            currentState = OperationMockState.Mock(
                statusCode = 200,
                mediaType = "application/json",
                exampleName = "default"
            ),
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

        onNodeWithTag(testTag = "mock_item_404_application/json_default").performClick()

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
    fun eye_opensResponseDirectly_withoutSelectingOrMarking() = runComposeUiTest {
        var preview: MockResponse? = null
        var selected: MockResponse? = null
        setPickerPage(
            currentState = OperationMockState.Network,
            onPreview = { preview = it },
            onSelectResponse = { selected = it }
        )

        onNodeWithTag(testTag = "mock_item_200_application/json_default_preview_toggle").performClick()

        preview shouldBe response200
        selected shouldBe null
        onNodeWithTag(testTag = "open_preview_button").assertDoesNotExist()
    }

    @Test
    fun sequenceAction_opensBuilder() = runComposeUiTest {
        var opened = false
        setPickerPage(currentState = OperationMockState.Network, onOpenSequence = { opened = true })

        onNodeWithTag(testTag = "sequence_build_button").performClick()

        opened shouldBe true
    }

    @Test
    fun activeSequence_showsPositionAndEditAndReset() = runComposeUiTest {
        var reset = false
        setPickerPage(
            currentState = OperationMockState.Sequence(
                responses = listOf(
                    OperationMockState.Mock(statusCode = 200, mediaType = "application/json", exampleName = "default"),
                    OperationMockState.Mock(statusCode = 404, mediaType = "application/json", exampleName = "default")
                ),
                currentIndex = 1
            ),
            onResetSequencePosition = { reset = true }
        )

        onNodeWithText(text = "Step 2 of 2").assertIsDisplayed()
        onNodeWithText(text = "Edit").assertIsDisplayed()
        onNodeWithTag(testTag = "sequence_reset_button").performClick()
        reset shouldBe true
    }

    @Test
    fun failureRow_preservesFailureCallback() = runComposeUiTest {
        var selected: FailureKind? = null
        setPickerPage(currentState = OperationMockState.Network, onSelectFailure = { selected = it })
        val kind = FailureKind.entries.first()
        onNodeWithTag(testTag = "operation_picker_list").performScrollToNode(
            matcher = hasTestTag(testTag = "failure_item_${kind.name}")
        )
        onNodeWithTag(testTag = "failure_item_${kind.name}").performClick()
        selected shouldBe kind
    }

    @Test
    fun disambiguatesResponsesByMediaType_whenSameStatusCodeHasMultipleMediaTypes() = runComposeUiTest {
        val jsonVariant = MockResponse(
            statusCode = 200,
            mediaType = "application/json",
            exampleName = "default",
            displayName = "200 - default",
            content = "{}"
        )
        val xmlVariant = MockResponse(
            statusCode = 200,
            mediaType = "application/xml",
            exampleName = "default",
            displayName = "200 - default",
            content = "<user/>"
        )

        setContent {
            MaterialTheme {
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
                            currentState = OperationMockState.Network
                        ),
                        responses = listOf(jsonVariant, xmlVariant).toPersistentList()
                    ),
                    onSelectResponse = {},
                    onSelectFailure = {},
                    onResetSequencePosition = {},
                    onPreview = {},
                    onOpenSequence = {},
                    onClose = {}
                )
            }
        }

        onNodeWithText(text = "application/json").assertIsDisplayed()
        onNodeWithText(text = "application/xml").assertIsDisplayed()
    }

    private fun ComposeUiTest.setPickerPage(
        currentState: OperationMockState,
        onSelectResponse: (MockResponse?) -> Unit = {},
        onSelectFailure: (FailureKind) -> Unit = {},
        onResetSequencePosition: () -> Unit = {},
        onPreview: (MockResponse) -> Unit = {},
        onOpenSequence: () -> Unit = {},
        onClose: () -> Unit = {}
    ) {
        setContent {
            MaterialTheme {
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
                    onSelectResponse = onSelectResponse,
                    onSelectFailure = onSelectFailure,
                    onResetSequencePosition = onResetSequencePosition,
                    onPreview = onPreview,
                    onOpenSequence = onOpenSequence,
                    onClose = onClose
                )
            }
        }
    }
}
