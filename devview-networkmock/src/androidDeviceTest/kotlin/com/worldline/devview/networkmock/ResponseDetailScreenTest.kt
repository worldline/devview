package com.worldline.devview.networkmock

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.worldline.devview.networkmock.core.model.MockResponse
import com.worldline.devview.networkmock.viewmodel.asState
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertNotSame

class ResponseDetailScreenTest {

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
    fun comparison_cache_survives_unrelated_recompositions_and_invalidates_changed_bodies() = runComposeUiTest {
        val progress = mutableStateOf(value = 0)
        val primary = mutableStateOf(value = response200)
        val compared = mutableStateOf<MockResponse?>(value = response404)
        lateinit var observed: PreviewSheetState.HasResponse
        setContent {
            val preview = rememberResponsePreview(primary = primary.value, second = compared.value)
            val tick = progress.value
            SideEffect { observed = preview; check(tick >= 0) }
        }
        val initial = observed as PreviewSheetState.Compare
        val diff = initial.lineDiff
        runOnIdle { progress.value++ }
        runOnIdle {
            assertSame(initial, observed)
            assertSame(diff, (observed as PreviewSheetState.Compare).lineDiff)
            primary.value = response200.copy(content = "{\"ok\":false}")
        }
        lateinit var changedPrimary: PreviewSheetState.HasResponse
        runOnIdle {
            changedPrimary = observed
            assertNotSame(initial, changedPrimary)
            compared.value = response404.copy(content = "{\"error\":\"updated\"}")
        }
        runOnIdle {
            assertNotSame(changedPrimary, observed)
            compared.value = null
        }
        runOnIdle { check(observed is PreviewSheetState.Single) }
    }

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

        onNodeWithTag(testTag = "response_back").performClick()

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
    fun noOtherResponses_hidesCompare() = runComposeUiTest {
        setPage(previewSheetState = PreviewSheetState.Single(response = response200))

        onNodeWithTag(testTag = "preview_compare_button").assertDoesNotExist()
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

    @Test
    fun useInvokesSelectionAndCopyBodyIsAvailable() = runComposeUiTest {
        var used = false
        setPage(PreviewSheetState.Single(response200), onUse = { used = true })
        onNodeWithTag("response_copy_body").assertIsDisplayed().performClick()
        val clipboard = InstrumentationRegistry.getInstrumentation().targetContext
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        waitUntil(timeoutMillis = 10_000) {
            clipboard.primaryClip?.getItemAt(0)?.text?.toString() == response200.content
        }
        onNodeWithTag("response_use").performClick()
        used shouldBe true
    }

    @Test
    fun keyboardCanReachAndActivateCompare() = runComposeUiTest {
        setPage(PreviewSheetState.Single(response200), otherResponses = listOf(response404), keyboard = true)

        onNodeWithTag("response_copy_body").performSemanticsAction(SemanticsActions.RequestFocus)
        onNodeWithTag("response_copy_body").assertIsFocused()
        onRoot().performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Tab) } }
        onNodeWithTag("preview_compare_button").assertIsFocused()
        onRoot().performKeyInput { pressKey(Key.Enter) }
        onNodeWithTag("preview_compare_404_application/json_not-found").assertIsDisplayed()
    }

    @Test
    fun currentResponseDisablesUseAndReadsInUse() = runComposeUiTest {
        setPage(PreviewSheetState.Single(response200), currentState = response200.asState())
        onNodeWithText("In use").assertIsDisplayed()
        onNodeWithTag("response_use").assertIsNotEnabled()
    }

    private fun ComposeUiTest.setPage(
        previewSheetState: PreviewSheetState.HasResponse,
        otherResponses: List<MockResponse> = emptyList(),
        onCompareWith: (MockResponse) -> Unit = {},
        onBack: () -> Unit = {},
        currentState: com.worldline.devview.networkmock.core.model.OperationMockState = com.worldline.devview.networkmock.core.model.OperationMockState.Network,
        onUse: () -> Unit = {},
        keyboard: Boolean = false
    ) {
        setContent {
            val inputMode = LocalInputModeManager.current
            SideEffect { if (keyboard) inputMode.requestInputMode(InputMode.Keyboard) }
            MaterialTheme {
                ResponseDetailContent(
                    preview = previewSheetState,
                    otherResponses = otherResponses,
                    currentState = currentState,
                    enabled = true,
                    onCompare = onCompareWith,
                    onUse = onUse,
                    onBack = onBack
                )
            }
        }
    }
}
