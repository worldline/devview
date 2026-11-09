package com.worldline.devview.networkmock.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.worldline.devview.networkmock.core.model.MockResponse
import com.worldline.devview.networkmock.model.DiffLine
import io.kotest.matchers.shouldBe
import kotlinx.collections.immutable.toPersistentList
import kotlin.test.Test
import kotlin.test.assertTrue

class MockResponseDiffContentTest {

    private val firstLine = "first " + "x".repeat(200)
    private val secondLine = "second " + "y".repeat(200)

    @Test
    fun singlePane_keepsLongLogicalLinesUnwrapped_andScrollsRowsAndGutterTogether() = runComposeUiTest {
        setPreview {
            SplitDiffContent(first = response("$firstLine\n$secondLine\nshort"), second = null)
        }

        assertSingleLine(text = firstLine)
        onNodeWithText(text = firstLine).fetchSemanticsNode().boundsInRoot.height shouldBe
            onNodeWithText(text = "short").fetchSemanticsNode().boundsInRoot.height
        assertRowsScrollTogether(tag = "preview_first_pane")
    }

    @Test
    fun splitPanes_scrollIndependently_withUnwrappedRows() = runComposeUiTest {
        val otherLine = "other " + "z".repeat(200)
        setPreview {
            SplitDiffContent(
                first = response("$firstLine\n$secondLine"),
                second = response("$otherLine\nlast")
            )
        }

        assertSingleLine(text = firstLine)
        assertSingleLine(text = otherLine)
        val otherBefore = onNodeWithText(text = otherLine).getUnclippedBoundsInRoot()
        assertRowsScrollTogether(tag = "preview_first_pane")
        onNodeWithText(text = otherLine).getUnclippedBoundsInRoot() shouldBe otherBefore
        val lastBefore = onNodeWithText(text = "last").getUnclippedBoundsInRoot().left
        onNodeWithTag(testTag = "preview_second_pane").performTouchInput {
            swipeLeftSlowly()
        }
        assertTrue(onNodeWithText(text = otherLine).getUnclippedBoundsInRoot().left < otherBefore.left)
        assertTrue(onNodeWithText(text = "last").getUnclippedBoundsInRoot().left < lastBefore)
    }

    @Test
    fun inlinePane_keepsLongLinesUnwrapped_andScrollsRowsAndGutterTogether() = runComposeUiTest {
        setPreview {
            InlineDiffContent(
                diff = listOf(
                    DiffLine.Unchanged(text = firstLine, lineLeft = 1, lineRight = 1),
                    DiffLine.Different(textLeft = secondLine, lineLeft = 2, textRight = "short", lineRight = 2)
                ).toPersistentList(),
                leftLabel = "First",
                rightLabel = "Second"
            )
        }

        assertSingleLine(text = firstLine)
        assertSingleLine(text = secondLine)
        assertRowsScrollTogether(tag = "preview_inline_pane")
        onNodeWithText(text = "First").assertIsDisplayed()
    }

    @Test
    fun singlePane_composes5000LinesLazily() = runComposeUiTest {
        setPreview {
            SplitDiffContent(first = response(numberedLines()), second = null)
        }

        assertLazyEnd(tag = "preview_first_pane_rows", lastIndex = 4999)
    }

    @Test
    fun splitPanes_compose5000LinesLazily() = runComposeUiTest {
        setPreview {
            SplitDiffContent(first = response("first pane"), second = response(numberedLines()))
        }

        assertLazyEnd(tag = "preview_second_pane_rows", lastIndex = 4999)
        onNodeWithText(text = "first pane").assertIsDisplayed()
    }

    @Test
    fun inlinePane_composes5000LinesLazily() = runComposeUiTest {
        setPreview {
            InlineDiffContent(
                diff = (1..5000).map { number ->
                    DiffLine.Different(
                        textLeft = "logical line $number",
                        lineLeft = number,
                        textRight = null,
                        lineRight = null
                    )
                }.toPersistentList()
            )
        }

        assertLazyEnd(tag = "preview_inline_pane_rows", lastIndex = 4999)
    }

    @Test
    fun shorter_wide_unicode_lines_are_not_clipped_and_their_end_is_reachable() = runComposeUiTest {
        val narrow = "i".repeat(32)
        val wide = "界🙂".repeat(10)
        val body = List(size = 5_000) { if (it == 4_999) wide else narrow }.joinToString("\n")
        setPreview { SplitDiffContent(first = response(body), second = null) }
        assertTrue(wide.length < narrow.length)
        onNodeWithTag("preview_first_pane_rows").performScrollToIndex(index = 4_999)
        assertSingleLine(text = wide)
        val wideLayout = mutableListOf<TextLayoutResult>()
        val narrowLayout = mutableListOf<TextLayoutResult>()
        onNodeWithText(wide).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(wideLayout) }
        onAllNodesWithText(narrow)[0].performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(narrowLayout) }
        assertTrue(wideLayout.single().getLineRight(0) > narrowLayout.single().getLineRight(0))
        onNodeWithTag("preview_first_pane").performSemanticsAction(SemanticsActions.ScrollBy) { it(100_000f, 0f) }
        val viewport = onNodeWithTag("preview_first_pane").getUnclippedBoundsInRoot()
        val line = onNodeWithText(wide).getUnclippedBoundsInRoot()
        assertTrue(line.right <= viewport.right + 1.dp, "The widest rendered line must remain horizontally reachable")
    }

    @Test
    fun singlePane_preservesJsonPrettyPrinting() = runComposeUiTest {
        setPreview {
            SplitDiffContent(
                first = response("{\"value\":\"unchanged\"}").copy(contentType = "application/json"),
                second = null
            )
        }

        onNodeWithText(text = "{").assertIsDisplayed()
        onNodeWithText(text = "    \"value\": \"unchanged\"").assertIsDisplayed()
        onNodeWithText(text = "}").assertIsDisplayed()
    }

    private fun ComposeUiTest.assertSingleLine(text: String) {
        val results = mutableListOf<TextLayoutResult>()
        onNodeWithText(text = text).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
            action(results)
        }
        results.single().lineCount shouldBe 1
        results.single().hasVisualOverflow shouldBe false
    }

    private fun ComposeUiTest.assertRowsScrollTogether(tag: String) {
        val firstBefore = onNodeWithText(text = firstLine).getUnclippedBoundsInRoot().left
        val secondBefore = onNodeWithText(text = secondLine).getUnclippedBoundsInRoot().left
        val gutter = onAllNodesWithText(text = "1", useUnmergedTree = true)[0]
        val gutterBefore = gutter.getUnclippedBoundsInRoot().left

        onNodeWithTag(testTag = tag).performTouchInput { swipeLeftSlowly() }

        val delta = onNodeWithText(text = firstLine).getUnclippedBoundsInRoot().left - firstBefore
        assertTrue(delta < 0.dp)
        onNodeWithText(text = secondLine).getUnclippedBoundsInRoot().left - secondBefore shouldBe delta
        val gutterDelta = gutter.getUnclippedBoundsInRoot().left - gutterBefore
        assertTrue(kotlin.math.abs(gutterDelta.value - delta.value) < 0.01f)
    }

    private fun TouchInjectionScope.swipeLeftSlowly() {
        swipe(
            start = Offset(x = width * 0.75f, y = height / 2f),
            end = Offset(x = width * 0.75f - 80f, y = height / 2f),
            durationMillis = 1000
        )
    }

    private fun ComposeUiTest.assertLazyEnd(tag: String, lastIndex: Int) {
        onNodeWithText(text = "logical line 5000").assertDoesNotExist()
        onNodeWithTag(testTag = tag).performScrollToIndex(index = lastIndex)
        onNodeWithText(text = "logical line 5000").assertIsDisplayed()
    }

    private fun numberedLines() = (1..5000).joinToString(separator = "\n") { "logical line $it" }

    private fun response(content: String) = MockResponse(
        statusCode = 200,
        mediaType = "text/plain",
        exampleName = "default",
        displayName = "Test response",
        content = content,
        contentType = "text/plain"
    )

    private fun ComposeUiTest.setPreview(content: @Composable () -> Unit) {
        setContent {
            MaterialTheme {
                Box(modifier = Modifier.size(width = 240.dp, height = 320.dp)) {
                    content()
                }
            }
        }
    }
}
