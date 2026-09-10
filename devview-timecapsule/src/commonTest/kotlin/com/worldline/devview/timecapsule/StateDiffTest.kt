package com.worldline.devview.timecapsule

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class StateDiffTest {

    @Test
    fun `diffLabels finds a single changed field with correct range`() {
        val current = "CounterState(count=4, name=foo)"
        val previous = "CounterState(count=3, name=foo)"

        val changes = diffLabels(current = current, previous = previous)

        changes shouldHaveSize 1
        val change = changes.single()
        change.key shouldBe "count"
        change.old shouldBe "3"
        change.new shouldBe "4"
        current.substring(range = change.newValueRange) shouldBe "4"
    }

    @Test
    fun `diffLabels keeps list and nested parenthesis values intact`() {
        val current = "ListState(items=[a, b, c], addr=Address(city=Paris, zip=75000), loading=false)"
        val previous = "ListState(items=[a, b], addr=Address(city=Paris, zip=75000), loading=false)"

        val changes = diffLabels(current = current, previous = previous)

        changes shouldHaveSize 1
        val change = changes.single()
        change.key shouldBe "items"
        change.old shouldBe "[a, b]"
        change.new shouldBe "[a, b, c]"
        current.substring(range = change.newValueRange) shouldBe "[a, b, c]"
    }

    @Test
    fun `diffLabels parses spaced key equals value form`() {
        val changes = diffLabels(current = "Count = 4", previous = "Count = 3")

        changes shouldHaveSize 1
        val change = changes.single()
        change.key shouldBe "Count"
        change.old shouldBe "3"
        change.new shouldBe "4"
    }

    @Test
    fun `diffLabels returns empty list when label has no key value pairs`() {
        diffLabels(current = "Logged in", previous = "Logged out").shouldBeEmpty()
    }

    @Test
    fun `diffLabels returns empty list when there is no previous label`() {
        diffLabels(current = "CounterState(count=4)", previous = null).shouldBeEmpty()
    }

    @Test
    fun `diffLabels returns empty list for identical labels`() {
        val label = "CounterState(count=4, name=foo)"

        diffLabels(current = label, previous = label).shouldBeEmpty()
    }

    @Test
    fun `diffLabels ignores keys present on only one side`() {
        val current = "CounterState(count=4, name=foo)"
        val previous = "CounterState(count=3)"

        val changes = diffLabels(current = current, previous = previous)

        changes shouldHaveSize 1
        changes.single().key shouldBe "count"
    }

    @Test
    fun `shownChanges caps at the limit when collapsed`() {
        val changes = fieldChanges(count = 7)

        changes.shownChanges(expanded = false).map { it.key } shouldBe listOf("f1", "f2", "f3")
    }

    @Test
    fun `shownChanges returns every change when expanded`() {
        val changes = fieldChanges(count = 7)

        changes.shownChanges(expanded = true) shouldBe changes
    }

    @Test
    fun `shownChanges returns everything when the count is at or under the limit`() {
        val changes = fieldChanges(count = 3)

        changes.shownChanges(expanded = false) shouldBe changes
    }

    @Test
    fun `shownChanges stays empty for an empty list`() {
        emptyList<FieldChange>().shownChanges(expanded = false).shouldBeEmpty()
    }

    private fun fieldChanges(count: Int): List<FieldChange> = (1..count).map { index ->
        FieldChange(key = "f$index", old = "$index", new = "${index + 1}", newValueRange = 0 until 1)
    }
}
