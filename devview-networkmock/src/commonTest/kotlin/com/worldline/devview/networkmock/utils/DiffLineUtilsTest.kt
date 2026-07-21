package com.worldline.devview.networkmock.utils

import com.worldline.devview.networkmock.model.DiffLine
import com.worldline.devview.networkmock.model.DisplayLine
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

class DiffLineUtilsTest {

    // region shouldUseInlineDiff

    @Test
    fun `shouldUseInlineDiff returns true for identical content`() {
        val content = "line1\nline2\nline3"

        shouldUseInlineDiff(contentLeft = content, contentRight = content) shouldBe true
    }

    @Test
    fun `shouldUseInlineDiff returns true for empty content`() {
        shouldUseInlineDiff(contentLeft = "", contentRight = "") shouldBe true
    }

    @Test
    fun `shouldUseInlineDiff returns true when ratio meets threshold`() {
        // 3 shared lines out of 4 max = 0.75 >= 0.4
        val left = "a\nb\nc\nd"
        val right = "a\nb\nc\ne"

        shouldUseInlineDiff(contentLeft = left, contentRight = right) shouldBe true
    }

    @Test
    fun `shouldUseInlineDiff returns false for completely different content`() {
        val left = "a\nb\nc"
        val right = "x\ny\nz"

        shouldUseInlineDiff(contentLeft = left, contentRight = right) shouldBe false
    }

    @Test
    fun `shouldUseInlineDiff respects custom threshold`() {
        // 1 shared line out of 2 max = 0.5
        val left = "a\nb"
        val right = "a\nc"

        // threshold 0.6 → 0.5 < 0.6 → false
        shouldUseInlineDiff(contentLeft = left, contentRight = right, threshold = 0.6f) shouldBe false
        // threshold 0.4 → 0.5 >= 0.4 → true
        shouldUseInlineDiff(contentLeft = left, contentRight = right, threshold = 0.4f) shouldBe true
    }

    // endregion

    // region computeLineDiff

    @Test
    fun `computeLineDiff with identical content produces all Unchanged`() {
        val content = "line1\nline2\nline3"

        val diff = computeLineDiff(contentLeft = content, contentRight = content)

        diff.shouldHaveSize(3)
        diff.forEach { it.shouldBeInstanceOf<DiffLine.Unchanged>() }
    }

    @Test
    fun `computeLineDiff with completely different content produces all Different`() {
        val left = "a\nb"
        val right = "x\ny"

        val diff = computeLineDiff(contentLeft = left, contentRight = right)

        diff.forEach { it.shouldBeInstanceOf<DiffLine.Different>() }
    }

    @Test
    fun `computeLineDiff with one changed line produces mixed result`() {
        val left = "a\nb\nc"
        val right = "a\nX\nc"

        val diff = computeLineDiff(contentLeft = left, contentRight = right)

        val unchanged = diff.filterIsInstance<DiffLine.Unchanged>()
        val different = diff.filterIsInstance<DiffLine.Different>()
        unchanged.shouldHaveSize(2)
        different.size shouldBe 2 // one removed left, one added right
    }

    @Test
    fun `computeLineDiff with single-line left and multi-line right`() {
        // "".lines() = [""] — one empty-string line
        val diff = computeLineDiff(contentLeft = "", contentRight = "x\ny")

        // "" does not match "x" or "y", so all are Different
        diff.forEach { it.shouldBeInstanceOf<DiffLine.Different>() }
    }

    @Test
    fun `computeLineDiff with multi-line left and single-line right`() {
        val diff = computeLineDiff(contentLeft = "x\ny", contentRight = "")

        diff.forEach { it.shouldBeInstanceOf<DiffLine.Different>() }
    }

    @Test
    fun `computeLineDiff with both empty strings produces one Unchanged empty line`() {
        // "".lines() = [""] on both sides → the two empty strings match
        val diff = computeLineDiff(contentLeft = "", contentRight = "")

        diff.shouldHaveSize(1)
        diff.first().shouldBeInstanceOf<DiffLine.Unchanged>()
    }

    @Test
    fun `computeLineDiff carries correct 1-based line numbers`() {
        val left = "a\nb\nc"
        val right = "a\nb\nc"

        val diff = computeLineDiff(contentLeft = left, contentRight = right)

        diff.forEachIndexed { index, line ->
            line.shouldBeInstanceOf<DiffLine.Unchanged>()
            line.lineLeft shouldBe index + 1
            line.lineRight shouldBe index + 1
        }
    }

    // endregion

    // region toDisplayLines

    @Test
    fun `toDisplayLines does not collapse short unchanged runs`() {
        // COLLAPSE_THRESHOLD = 7; a run of 6 should not collapse
        val lines = List(size = 6) { i ->
            DiffLine.Unchanged(text = "line$i", lineLeft = i + 1, lineRight = i + 1)
        }

        val display = lines.toDisplayLines()

        display.filterIsInstance<DisplayLine.Collapsed>() shouldHaveSize 0
        display shouldHaveSize 6
    }

    @Test
    fun `toDisplayLines collapses long unchanged runs`() {
        // 9 unchanged lines → should collapse the middle 3 (9 - 3 - 3 = 3)
        val lines = List(size = 9) { i ->
            DiffLine.Unchanged(text = "line$i", lineLeft = i + 1, lineRight = i + 1)
        }

        val display = lines.toDisplayLines()

        val collapsed = display.filterIsInstance<DisplayLine.Collapsed>()
        collapsed shouldHaveSize 1
        collapsed.first().count shouldBe 3 // 9 - 3 context - 3 context
    }

    @Test
    fun `toDisplayLines keeps CONTEXT_LINES on each side of collapsed region`() {
        // 10 unchanged lines: first 3 shown, 4 collapsed, last 3 shown
        val lines = List(size = 10) { i ->
            DiffLine.Unchanged(text = "line$i", lineLeft = i + 1, lineRight = i + 1)
        }

        val display = lines.toDisplayLines()

        // 3 before + 1 Collapsed + 3 after = 7 items
        display shouldHaveSize 7
        display[0].shouldBeInstanceOf<DisplayLine.Unchanged>()
        display[1].shouldBeInstanceOf<DisplayLine.Unchanged>()
        display[2].shouldBeInstanceOf<DisplayLine.Unchanged>()
        display[3].shouldBeInstanceOf<DisplayLine.Collapsed>().count shouldBe 4
        display[4].shouldBeInstanceOf<DisplayLine.Unchanged>()
        display[5].shouldBeInstanceOf<DisplayLine.Unchanged>()
        display[6].shouldBeInstanceOf<DisplayLine.Unchanged>()
    }

    @Test
    fun `toDisplayLines handles Different lines with left side only`() {
        val lines = listOf(
            DiffLine.Different(textLeft = "removed", lineLeft = 1, textRight = null, lineRight = null)
        )

        val display = lines.toDisplayLines()

        display shouldHaveSize 1
        display.first().shouldBeInstanceOf<DisplayLine.Left>().text shouldBe "removed"
    }

    @Test
    fun `toDisplayLines handles Different lines with right side only`() {
        val lines = listOf(
            DiffLine.Different(textLeft = null, lineLeft = null, textRight = "added", lineRight = 1)
        )

        val display = lines.toDisplayLines()

        display shouldHaveSize 1
        display.first().shouldBeInstanceOf<DisplayLine.Right>().text shouldBe "added"
    }

    @Test
    fun `toDisplayLines handles empty input`() {
        val display = emptyList<DiffLine>().toDisplayLines()

        display shouldHaveSize 0
    }

    // endregion
}
