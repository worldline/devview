package com.worldline.devview.networkmock

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
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

class SequenceBuilderPageTest {

    private val jsonResponse = MockResponse(
        statusCode = 200,
        mediaType = "application/json",
        exampleName = "default",
        displayName = "200 - default",
        content = "{}"
    )
    private val xmlResponse = jsonResponse.copy(mediaType = "application/xml", content = "<user/>")
    private val namedResponse = jsonResponse.copy(exampleName = "other", displayName = "200 - other")

    @Test
    fun appendAllowsRepeats_andSaveRequiresTwoSteps() = runComposeUiTest {
        var saved: List<MockResponse> = emptyList()
        setPage(onCommitSequence = { saved = it })

        onNodeWithTag(testTag = "sequence_save_button").assertIsNotEnabled()
        onNodeWithTag(testTag = "sequence_response_200_application/json_default").performClick()
        onNodeWithTag(testTag = "sequence_save_button").assertIsNotEnabled()
        onNodeWithTag(testTag = "sequence_response_200_application/json_default").performClick()
        onNodeWithTag(testTag = "sequence_save_button").assertIsEnabled().performClick()

        saved shouldBe listOf(jsonResponse, jsonResponse)
    }

    @Test
    fun activeSequenceSeedsOrderRepeatsAndExactVariantIdentity() = runComposeUiTest {
        var saved: List<MockResponse> = emptyList()
        setPage(
            currentState = OperationMockState.Sequence(
                responses = listOf(xmlResponse.asStep(), namedResponse.asStep(), xmlResponse.asStep()),
                currentIndex = 2
            ),
            onCommitSequence = { saved = it }
        )

        onNodeWithTag(testTag = "sequence_step_0").assertIsDisplayed()
        onNodeWithTag(testTag = "sequence_step_2").assertIsDisplayed()
        onNodeWithTag(testTag = "sequence_save_button").assertIsEnabled().performClick()

        saved shouldBe listOf(xmlResponse, namedResponse, xmlResponse)
    }

    @Test
    fun removeStep_preservesOrderAndDisablesSaveBelowTwo() = runComposeUiTest {
        var saved: List<MockResponse> = emptyList()
        setPage(
            currentState = OperationMockState.Sequence(
                responses = listOf(jsonResponse.asStep(), xmlResponse.asStep(), namedResponse.asStep())
            ),
            onCommitSequence = { saved = it }
        )

        onNodeWithTag(testTag = "sequence_remove_step_1").performClick()
        onNodeWithTag(testTag = "sequence_save_button").performClick()
        saved shouldBe listOf(jsonResponse, namedResponse)
        onNodeWithTag(testTag = "sequence_remove_step_0").performClick()
        onNodeWithTag(testTag = "sequence_save_button").assertIsNotEnabled()
        onNodeWithTag(testTag = "sequence_step_1").assertDoesNotExist()
    }

    @Test
    fun backAndCancelReturnWithoutCommittingDraft() = runComposeUiTest {
        var backCount = 0
        var saved = false
        setPage(onBack = { backCount++ }, onCommitSequence = { saved = true })

        onNodeWithTag(testTag = "sequence_response_200_application/json_default").performClick()
        onNodeWithTag(testTag = "sequence_page_back_button").performClick()
        onNodeWithTag(testTag = "sequence_cancel_button").performClick()

        backCount shouldBe 2
        saved shouldBe false
    }

    @Test
    fun unavailableActiveStepsAreNotSilentlyDropped() = runComposeUiTest {
        val unavailable = jsonResponse.asStep().copy(exampleName = "removed")
        var saved: List<MockResponse> = emptyList()
        setPage(
            currentState = OperationMockState.Sequence(
                responses = listOf(jsonResponse.asStep(), unavailable, xmlResponse.asStep())
            ),
            onCommitSequence = { saved = it }
        )

        onNodeWithText(text = "Remove unavailable steps before saving (3 added).").assertIsDisplayed()
        onNodeWithTag(testTag = "sequence_save_button").assertIsNotEnabled()
        onNodeWithTag(testTag = "sequence_remove_step_1").performClick()
        onNodeWithTag(testTag = "sequence_save_button").assertIsEnabled().performClick()
        saved shouldBe listOf(jsonResponse, xmlResponse)
    }

    @Test
    fun availableResponseListRemainsScrollableWithManySteps() = runComposeUiTest {
        setPage(
            currentState = OperationMockState.Sequence(responses = List(size = 30) { jsonResponse.asStep() })
        )

        onNodeWithTag(testTag = "sequence_builder_list").performScrollToNode(
            matcher = hasTestTag(testTag = "sequence_response_200_application/xml_default")
        )
        onNodeWithTag(testTag = "sequence_response_200_application/xml_default").performClick()
        onNodeWithText(text = "Save Sequence (31)").assertIsDisplayed()
    }

    private fun MockResponse.asStep(): OperationMockState.Mock = OperationMockState.Mock(
        statusCode = statusCode,
        mediaType = mediaType,
        exampleName = exampleName
    )

    private fun ComposeUiTest.setPage(
        currentState: OperationMockState = OperationMockState.Network,
        onBack: () -> Unit = {},
        onCommitSequence: (List<MockResponse>) -> Unit = {}
    ) {
        setContent {
            MaterialTheme {
                SequenceBuilderPage(
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
                        responses = listOf(jsonResponse, xmlResponse, namedResponse).toPersistentList()
                    ),
                    onBack = onBack,
                    onCommitSequence = onCommitSequence
                )
            }
        }
    }
}
