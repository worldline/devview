package com.worldline.devview.networkmock

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.worldline.devview.networkmock.core.model.MockResponse
import com.worldline.devview.networkmock.viewmodel.asState
import com.worldline.devview.networkmock.viewmodel.SequenceDraftStep
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class SequenceBuilderScreenTest {
    private val jsonResponse = MockResponse(200, "application/json", "default", "200 - default", "{}")
    private val xmlResponse = jsonResponse.copy(mediaType = "application/xml", content = "<user/>")
    private val namedResponse = jsonResponse.copy(exampleName = "other", displayName = "200 - other")

    @Test
    fun repeatedResponsesAreAllowedAndSaveRequiresTwoSteps() = runComposeUiTest {
        var saved: List<SequenceDraftStep> = emptyList()
        setPage(onSave = { saved = it })
        onNodeWithTag("sequence_save_button").assertIsNotEnabled()
        onNodeWithText("Add 2 more steps").assertIsDisplayed()
        add("sequence_response_200_application/json_default")
        onNodeWithTag("sequence_save_button").assertIsNotEnabled()
        onNodeWithText("Add 1 more step").assertIsDisplayed()
        add("sequence_response_200_application/json_default")
        onNodeWithTag("sequence_save_button").assertIsEnabled().performClick()
        saved.map { it.response } shouldBe listOf(jsonResponse.asState(), jsonResponse.asState())
        saved.map { it.id }.distinct().size shouldBe 2
    }

    @Test
    fun reorderingAndRemovalPreserveStepIdsAndVariantIdentity() = runComposeUiTest {
        var saved: List<SequenceDraftStep> = emptyList()
        setPage(
            steps = listOf(step("a", xmlResponse), step("b", namedResponse), step("c", xmlResponse)),
            onSave = { saved = it }
        )
        onNodeWithTag("sequence_up_a").assertIsNotEnabled()
        scrollTo("sequence_up_b")
        onNodeWithTag("sequence_up_b").performClick()
        scrollTo("sequence_remove_a")
        onNodeWithTag("sequence_remove_a").performClick()
        onNodeWithTag("sequence_save_button").performClick()
        saved shouldBe listOf(step("b", namedResponse), step("c", xmlResponse))
        onNodeWithTag("sequence_step_a").assertDoesNotExist()
        scrollTo("sequence_step_b")
        onNodeWithTag("sequence_step_b").assertIsDisplayed()
        scrollTo("sequence_step_c")
        onNodeWithTag("sequence_step_c").assertIsDisplayed()
    }

    @Test
    fun removingBelowTwoDisablesSaveWithoutRenamingRemainingStep() = runComposeUiTest {
        setPage(steps = listOf(step("first", jsonResponse), step("second", xmlResponse)))
        scrollTo("sequence_remove_first")
        onNodeWithTag("sequence_remove_first").performClick()
        onNodeWithTag("sequence_save_button").assertIsNotEnabled()
        onNodeWithTag("sequence_step_second").assertIsDisplayed()
        onNodeWithTag("sequence_step_first").assertDoesNotExist()
    }

    @Test
    fun cancelReturnsWithoutSavingDraft() = runComposeUiTest {
        var cancelled = false
        var saved = false
        setPage(onBack = { cancelled = true }, onSave = { saved = true })
        add("sequence_response_200_application/json_default")
        onNodeWithTag("sequence_cancel_button").performClick()
        cancelled shouldBe true
        saved shouldBe false
    }

    @Test
    fun unavailableStepsFreezeEditingAndSaveButAllowReloadAndCancel() = runComposeUiTest {
        var reloaded = false
        var cancelled = false
        val missing = SequenceDraftStep("missing", jsonResponse.asState().copy(exampleName = "removed"))
        setPage(steps = listOf(step("first", jsonResponse), missing), onReload = { reloaded = true }, onBack = { cancelled = true })
        scrollTo("sequence_remove_missing")
        onNodeWithTag("sequence_remove_missing").assertIsNotEnabled()
        onNodeWithTag("sequence_save_button").assertIsNotEnabled()
        scrollToText("Reload specification")
        onNodeWithText("Reload specification").performClick()
        reloaded shouldBe true
        onNodeWithTag("sequence_cancel_button").performClick()
        cancelled shouldBe true
    }

    @Test
    fun availableResponsesRemainReachableWithManySteps() = runComposeUiTest {
        setPage(steps = List(30) { step("step_$it", jsonResponse) })
        add("sequence_response_200_application/xml_default")
        onNodeWithText("Save sequence (31)").assertIsDisplayed()
    }

    private fun step(id: String, response: MockResponse) = SequenceDraftStep(id, response.asState())

    private fun ComposeUiTest.scrollTo(tag: String) {
        onNodeWithTag("sequence_builder_list").performScrollToNode(hasTestTag(tag))
    }

    private fun ComposeUiTest.scrollToText(text: String) {
        onNodeWithTag("sequence_builder_list").performScrollToNode(androidx.compose.ui.test.hasText(text))
    }

    private fun ComposeUiTest.add(tag: String) {
        scrollTo(tag)
        onNodeWithTag(tag).performClick()
    }

    private fun ComposeUiTest.setPage(
        steps: List<SequenceDraftStep> = emptyList(),
        onSave: (List<SequenceDraftStep>) -> Unit = {},
        onBack: () -> Unit = {},
        onReload: () -> Unit = {}
    ) {
        setContent {
            var draft by remember { mutableStateOf(steps) }
            var nextId by remember { mutableStateOf(0) }
            MaterialTheme {
                SequenceBuilderContent(
                    responses = listOf(jsonResponse, xmlResponse, namedResponse), steps = draft, enabled = true,
                    onAdd = { draft = draft + step("added_${nextId++}", it) },
                    onRemove = { id -> draft = draft.filterNot { it.id == id } },
                    onMove = { id, offset ->
                        val reordered = draft.toMutableList()
                        val index = reordered.indexOfFirst { it.id == id }
                        reordered.add(index + offset, reordered.removeAt(index))
                        draft = reordered
                    },
                    onSave = { onSave(draft) }, onBack = onBack, onReload = onReload
                )
            }
        }
    }
}
