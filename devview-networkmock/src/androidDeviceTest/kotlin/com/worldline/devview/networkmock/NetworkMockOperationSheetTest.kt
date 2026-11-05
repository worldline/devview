package com.worldline.devview.networkmock

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
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
        setSheet(currentState = OperationMockState.Network)

        onNodeWithText(text = "Get User").assertIsDisplayed()
        onNodeWithTag(testTag = "operation_sheet_network_item").assertIsDisplayed()
        onNodeWithTag(testTag = "mock_item_200_application/json_default").assertIsDisplayed()
        onNodeWithTag(testTag = "operation_sheet_tab_responses").assertIsSelected()
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
        setSheet(currentState = OperationMockState.Network, onClose = { closed = true })

        onNodeWithTag(testTag = "operation_sheet_close_button").performClick()
        waitUntil(timeoutMillis = 5_000) { closed }

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
    fun tabsAndPreview_keepHeaderAndHeight_andCancelReturnsToResponses() = runComposeUiTest {
        setSheet(currentState = OperationMockState.Network)
        val height = onNodeWithTag(testTag = "operation_sheet_content").fetchSemanticsNode().boundsInRoot.height
        onNodeWithText(text = "Build a Sequence").assertDoesNotExist()
        onNodeWithText(text = "SIMULATE FAILURE").assertDoesNotExist()
        onNodeWithTag(testTag = "sequence_builder_list").assertDoesNotExist()
        FailureKind.entries.forEach { kind ->
            onNodeWithTag(testTag = "failure_item_${kind.name}").assertDoesNotExist()
        }
        onNodeWithTag(testTag = "operation_sheet_tab_sequence").performClick().assertIsSelected()
        onNodeWithText(text = "Get User").assertIsDisplayed()
        onNodeWithTag(testTag = "sequence_page_back_button").assertDoesNotExist()
        onNodeWithTag(testTag = "operation_sheet_content").fetchSemanticsNode().boundsInRoot.height shouldBe height
        onNodeWithTag(testTag = "sequence_cancel_button").performClick()
        onNodeWithTag(testTag = "operation_sheet_tab_responses").assertIsSelected()
        onNodeWithTag(testTag = "operation_sheet_tab_failure").performClick().assertIsSelected()
        onNodeWithText(text = "SIMULATE FAILURE").assertIsDisplayed()
        onNodeWithTag(testTag = "operation_sheet_network_item").assertDoesNotExist()
        onNodeWithTag(testTag = "sequence_builder_list").assertDoesNotExist()
        onNodeWithTag(testTag = "operation_sheet_content").fetchSemanticsNode().boundsInRoot.height shouldBe height
        onNodeWithTag(testTag = "operation_sheet_tab_responses").performClick()
        onNodeWithTag(testTag = "mock_item_200_application/json_default_preview_toggle").performClick()
        onNodeWithTag(testTag = "preview_content").assertIsDisplayed()
        onNodeWithText(text = "Get User").assertIsDisplayed()
        onNodeWithTag(testTag = "operation_sheet_content").fetchSemanticsNode().boundsInRoot.height shouldBe height
        onNodeWithTag(testTag = "preview_page_back_button").performClick()
        onNodeWithTag(testTag = "operation_sheet_tab_responses").assertIsSelected()
        onNodeWithTag(testTag = "operation_sheet_network_item").assertIsDisplayed()
    }

    @Test
    fun activeSequence_statusAndResetAppearOnlyInSequenceTab() = runComposeUiTest {
        var reset = false
        setSheet(
            currentState = OperationMockState.Sequence(
                responses = listOf(
                    OperationMockState.Mock(statusCode = 200, mediaType = "application/json", exampleName = "default"),
                    OperationMockState.Mock(statusCode = 404, mediaType = "application/json", exampleName = "default")
                ),
                currentIndex = 1
            ),
            onResetSequencePosition = { reset = true }
        )

        onNodeWithTag(testTag = "sequence_position").assertDoesNotExist()
        onNodeWithTag(testTag = "sequence_reset_button").assertDoesNotExist()
        onNodeWithTag(testTag = "operation_sheet_tab_sequence").performClick()
        onNodeWithText(text = "Step 2 of 2").assertIsDisplayed()
        onNodeWithTag(testTag = "sequence_reset_button").performClick()
        reset shouldBe true
    }

    @Test
    fun failureRow_preservesFailureCallbackAndCloses() = runComposeUiTest {
        var selected: FailureKind? = null
        var closed = false
        setSheet(
            currentState = OperationMockState.Network,
            onSelectFailure = { selected = it },
            onClose = { closed = true }
        )
        val kind = FailureKind.entries.first()
        onNodeWithTag(testTag = "failure_item_${kind.name}").assertDoesNotExist()
        onNodeWithTag(testTag = "operation_sheet_tab_failure").performClick()
        onNodeWithTag(testTag = "operation_failure_list").performScrollToNode(
            matcher = hasTestTag(testTag = "failure_item_${kind.name}")
        )
        onNodeWithTag(testTag = "failure_item_${kind.name}").performClick()
        selected shouldBe kind
        waitUntil(timeoutMillis = 5_000) { closed }
        closed shouldBe true
    }

    @Test
    fun configuredFailureRate_isOnlyInFailureFooter() = runComposeUiTest {
        setSheet(currentState = OperationMockState.Network, failureRate = 0.25)
        onNodeWithTag(testTag = "operation_sheet_failure_rate").assertDoesNotExist()
        onNodeWithTag(testTag = "operation_sheet_tab_failure").performClick()
        onNodeWithTag(testTag = "operation_failure_list").performScrollToNode(
            matcher = hasTestTag(testTag = "operation_sheet_failure_rate")
        )
        onNodeWithTag(testTag = "operation_sheet_failure_rate").assertIsDisplayed()
        onNodeWithText(
            text = "Also configured to fail 25% of mocked requests on its own (x-devview.failureRate)"
        ).assertIsDisplayed()
        onNodeWithTag(testTag = "operation_sheet_tab_responses").performClick()
        onNodeWithTag(testTag = "operation_sheet_failure_rate").assertDoesNotExist()
    }

    @Test
    fun savingSequence_commitsAndCloses() = runComposeUiTest {
        var saved: List<MockResponse> = emptyList()
        var closed = false
        setSheet(
            currentState = OperationMockState.Network,
            onCommitSequence = { saved = it },
            onClose = { closed = true }
        )
        onNodeWithTag(testTag = "operation_sheet_tab_sequence").performClick()
        repeat(times = 2) {
            onNodeWithTag(testTag = "sequence_builder_list").performScrollToNode(
                matcher = hasTestTag(testTag = "sequence_response_200_application/json_default")
            )
            onNodeWithTag(testTag = "sequence_response_200_application/json_default").performClick()
        }
        onNodeWithTag(testTag = "sequence_save_button").performClick()
        saved shouldBe listOf(response200, response200)
        waitUntil(timeoutMillis = 5_000) { closed }
        closed shouldBe true
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
                    onPreview = {}
                )
            }
        }

        onNodeWithText(text = "application/json").assertIsDisplayed()
        onNodeWithText(text = "application/xml").assertIsDisplayed()
    }

    private fun ComposeUiTest.setPickerPage(
        currentState: OperationMockState,
        onSelectResponse: (MockResponse?) -> Unit = {},
        onPreview: (MockResponse) -> Unit = {}
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
                    onPreview = onPreview
                )
            }
        }
    }

    private fun ComposeUiTest.setSheet(
        currentState: OperationMockState,
        onSelectFailure: (FailureKind) -> Unit = {},
        onResetSequencePosition: () -> Unit = {},
        onCommitSequence: (List<MockResponse>) -> Unit = {},
        onClose: () -> Unit = {},
        failureRate: Double? = null
    ) {
        setContent {
            val navigationOwner = rememberNavigationEventDispatcherOwner(parent = null)
            CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides navigationOwner) {
                MaterialTheme {
                    NetworkMockOperationSheet(
                        sheetState = OperationSheetState.Content(
                            operationUiModel = OperationUiModel(
                                descriptor = OperationDescriptor(
                                    key = OperationKey(specId = "test", operationId = "getUser"),
                                    config = Operation(
                                        operationId = "getUser",
                                        name = "Get User",
                                        path = "/api/users/{userId}",
                                        method = HttpMethod.Get,
                                        failureRate = failureRate
                                    )
                                ),
                                currentState = currentState
                            ),
                            responses = listOf(response200, response404).toPersistentList()
                        ),
                        onDismissRequest = onClose,
                        onSelectResponse = {},
                        onSelectFailure = onSelectFailure,
                        onResetSequencePosition = onResetSequencePosition,
                        onCommitSequence = onCommitSequence
                    )
                }
            }
        }
    }
}
