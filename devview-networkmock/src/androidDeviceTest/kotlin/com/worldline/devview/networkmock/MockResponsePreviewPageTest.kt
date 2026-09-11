package com.worldline.devview.networkmock

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.worldline.devview.networkmock.core.model.MockResponse
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class MockResponsePreviewPageTest {

    private val response200 = MockResponse(
        statusCode = 200,
        exampleName = "default",
        displayName = "200 - default",
        content = "{\"ok\":true}"
    )
    private val response404 = MockResponse(
        statusCode = 404,
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

        onNodeWithText(text = "200 - default").assertIsDisplayed()
        onNodeWithText(text = "404 - not-found").assertIsDisplayed()
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

    private fun ComposeUiTest.setPage(
        previewSheetState: PreviewSheetState.HasResponse,
        onBack: () -> Unit = {}
    ) {
        setContent {
            MaterialTheme {
                MockResponsePreviewPage(
                    previewSheetState = previewSheetState,
                    onBack = onBack
                )
            }
        }
    }
}
