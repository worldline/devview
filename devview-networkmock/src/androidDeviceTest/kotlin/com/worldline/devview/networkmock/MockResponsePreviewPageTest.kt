package com.worldline.devview.networkmock

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.worldline.devview.networkmock.core.model.MockResponse
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class MockResponsePreviewPageTest {

    private val response200 = MockResponse(
        statusCode = 200,
        mediaType = "application/json",
        exampleName = "default",
        displayName = "200 - default",
        content = "{\"ok\":true}"
    )
    private val response404 = MockResponse(
        statusCode = 404,
        mediaType = "application/json",
        exampleName = "not-found",
        displayName = "404 - not-found",
        content = "{\"error\":\"not found\"}"
    )

    @Test
    fun single_showsOneResponseChip() = runComposeUiTest {
        setPage(previewSheetState = PreviewSheetState.Single(response = response200))

        onNodeWithText(text = "200 - default").assertIsDisplayed()
    }

    @Test
    fun compare_showsBothResponseChipsAndVsLabel() = runComposeUiTest {
        setPage(
            previewSheetState = PreviewSheetState.Compare(first = response200, second = response404)
        )

        onAllNodesWithText(text = "200 - default")[0].assertIsDisplayed()
        onAllNodesWithText(text = "404 - not-found")[0].assertIsDisplayed()
        onNodeWithText(text = "vs").assertIsDisplayed()
    }

    @Test
    fun tappingBackButton_invokesOnBack() = runComposeUiTest {
        var backTapped = false
        setPage(
            previewSheetState = PreviewSheetState.Single(response = response200),
            onBack = { backTapped = true }
        )

        onNodeWithTag(testTag = "preview_page_back_button").performClick()

        backTapped shouldBe true
    }

    @Test
    fun compareDropdown_selectsResponse_withoutLeavingPreview() = runComposeUiTest {
        var compared: MockResponse? = null
        var backTapped = false
        setPage(
            previewSheetState = PreviewSheetState.Single(response = response200),
            otherResponses = listOf(response404),
            onCompareWith = { compared = it },
            onBack = { backTapped = true }
        )

        onNodeWithTag(testTag = "preview_compare_button").performClick()
        onNodeWithTag(testTag = "preview_compare_404_application/json_not-found").performClick()

        compared shouldBe response404
        backTapped shouldBe false
        onNodeWithTag(testTag = "preview_content").assertIsDisplayed()
    }

    @Test
    fun noOtherResponses_disablesCompare() = runComposeUiTest {
        setPage(previewSheetState = PreviewSheetState.Single(response = response200))

        onNodeWithTag(testTag = "preview_compare_button").assertIsNotEnabled()
    }

    @Test
    fun comparisonCanBeChanged() = runComposeUiTest {
        val response500 = response404.copy(statusCode = 500, displayName = "500 - not-found")
        var compared: MockResponse? = null
        setPage(
            previewSheetState = PreviewSheetState.Compare(first = response200, second = response404),
            otherResponses = listOf(response404, response500),
            onCompareWith = { compared = it }
        )

        onNodeWithTag(testTag = "preview_compare_button").performClick()
        onNodeWithTag(testTag = "preview_compare_500_application/json_not-found").performClick()
        compared shouldBe response500
    }

    @Test
    fun mediaVariantsAreDistinguishableInHeaderAndDropdown() = runComposeUiTest {
        val xml = response200.copy(mediaType = "application/xml", contentType = "application/xml", content = "<ok/>")
        setPage(
            previewSheetState = PreviewSheetState.Compare(first = response200, second = xml),
            otherResponses = listOf(xml)
        )
        onNodeWithText(text = "200 - default · application/json").assertIsDisplayed()
        onNodeWithText(text = "200 - default · application/xml").assertIsDisplayed()
        onNodeWithTag(testTag = "preview_compare_button").performClick()
        onAllNodesWithText(text = "200 - default · application/xml")[1].assertIsDisplayed()
    }

    private fun ComposeUiTest.setPage(
        previewSheetState: PreviewSheetState.HasResponse,
        otherResponses: List<MockResponse> = emptyList(),
        onCompareWith: (MockResponse) -> Unit = {},
        onBack: () -> Unit = {}
    ) {
        setContent {
            MaterialTheme {
                MockResponsePreviewPage(
                    previewSheetState = previewSheetState,
                    otherResponses = otherResponses,
                    onCompareWith = onCompareWith,
                    onBack = onBack
                )
            }
        }
    }
}
