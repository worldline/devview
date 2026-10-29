package com.worldline.devview.timecapsule

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.text.TextRange
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

internal class TextFieldRecordingTest {
    @Test
    fun `typing and selection changes record without a screen state emission`() = runTest {
        val owner = FormOwner()
        val capsule = capsule(owner = owner)
        backgroundScope.launch { capsule.collectStates() }
        runCurrent()

        owner.state.value.name.edit {
            replace(start = 0, end = length, text = "Alice")
            selection = TextRange(start = 1, end = 4)
        }
        Snapshot.sendApplyNotifications()
        runCurrent()

        owner.state.value.name
            .edit { selection = TextRange(index = 3) }
        Snapshot.sendApplyNotifications()
        runCurrent()

        capsule.entries shouldHaveSize 3
        capsule.entries.map { it.textFields.first().text } shouldBe listOf("", "Alice", "Alice")
        capsule.entries.map { it.textFields.first().selection } shouldBe
            listOf(TextRange.Zero, TextRange(start = 1, end = 4), TextRange(index = 3))
    }

    @Test
    fun `capture and label stay detached after further editing and repeated restores`() {
        val owner = FormOwner(initial = FormState(name = TextFieldState(initialText = "Alice")))
        val capsule = capsule(owner = owner)
        capsule.record(state = owner.state.value)
        val first = capsule.entries.first()
        val originalLabel = first.label
        val originalDisplay = first.displayLabel

        owner.state.value.name.edit {
            replace(start = 0, end = length, text = "Bob")
        }
        capsule.record(state = owner.state.value)
        val second = capsule.entries.last()

        capsule.restore(id = first.id)
        owner.state.value.name.text
            .toString() shouldBe "Alice"
        capsule.restore(id = second.id)
        owner.state.value.name.text
            .toString() shouldBe "Bob"
        capsule.restore(id = first.id)
        owner.state.value.name.text
            .toString() shouldBe "Alice"
        first.textFields.first().text shouldBe "Alice"
        first.label shouldBe originalLabel
        first.displayLabel shouldBe originalDisplay
    }

    @Test
    fun `restore updates shared fields even when screen state is equal`() = runTest {
        val owner = FormOwner(initial = FormState(name = TextFieldState(initialText = "Alice")))
        val capsule = capsule(owner = owner)
        backgroundScope.launch { capsule.collectStates() }
        runCurrent()
        val first = capsule.entries.first()
        owner.state.value.name.edit {
            replace(start = 0, end = length, text = "Bob")
        }
        Snapshot.sendApplyNotifications()
        runCurrent()

        capsule.restore(id = first.id)
        Snapshot.sendApplyNotifications()
        runCurrent()

        owner.state.value shouldBe first.state
        capsule.entries.map { it.textFields.first().text } shouldBe listOf("Alice", "Bob", "Alice")
    }

    @Test
    fun `new screen states switch observation to their own field instances`() = runTest {
        val owner = FormOwner()
        val original = owner.state.value
        val capsule = capsule(owner = owner)
        backgroundScope.launch { capsule.collectStates() }
        runCurrent()

        val replacement = FormState(name = TextFieldState(initialText = "Replacement"), count = 1)
        owner.restoreState(state = replacement)
        runCurrent()
        capsule.entries
            .last()
            .textFields
            .first()
            .text shouldBe "Replacement"
        capsule.entries
            .last()
            .state.count shouldBe 1

        original.name.edit { replace(start = 0, end = length, text = "Obsolete") }
        Snapshot.sendApplyNotifications()
        runCurrent()
        capsule.entries shouldHaveSize 2

        replacement.name.edit { replace(start = 0, end = length, text = "Current") }
        Snapshot.sendApplyNotifications()
        runCurrent()
        capsule.entries shouldHaveSize 3
        capsule.entries
            .last()
            .textFields
            .first()
            .text shouldBe "Current"
    }

    @Test
    fun `restoring replaced fields rebinds observation to the saved screen`() = runTest {
        val owner = FormOwner(initial = FormState(name = TextFieldState(initialText = "Original")))
        val capsule = capsule(owner = owner)
        backgroundScope.launch { capsule.collectStates() }
        runCurrent()
        val first = capsule.entries.first()
        val replacement = FormState(name = TextFieldState(initialText = "Replacement"), count = 1)
        owner.restoreState(state = replacement)
        runCurrent()
        first.state.name.edit { replace(start = 0, end = length, text = "Stale") }
        Snapshot.sendApplyNotifications()
        runCurrent()

        capsule.restore(id = first.id)
        Snapshot.sendApplyNotifications()
        runCurrent()

        owner.state.value.name shouldBe first.state.name
        owner.state.value.name.text
            .toString() shouldBe "Original"
        replacement.name.text.toString() shouldBe "Replacement"
        owner.state.value.name
            .edit { replace(start = 0, end = length, text = "Resumed") }
        Snapshot.sendApplyNotifications()
        runCurrent()
        capsule.entries
            .last()
            .textFields
            .first()
            .text shouldBe "Resumed"
    }

    @Test
    fun `restore preserves multiple fields and reversed selection`() {
        val owner = FormOwner(
            initial = FormState(
                name = TextFieldState(
                    initialText = "Alice",
                    initialSelection = TextRange(start = 4, end = 1)
                ),
                description = TextFieldState(
                    initialText = "Notes",
                    initialSelection = TextRange(index = 2)
                )
            )
        )
        val capsule = capsule(owner = owner)
        capsule.record(state = owner.state.value)
        owner.state.value.name
            .edit { replace(start = 0, end = length, text = "") }
        owner.state.value.description
            .edit { replace(start = 0, end = length, text = "Changed") }

        capsule.restore(id = capsule.entries.first().id)

        owner.state.value.name.text
            .toString() shouldBe "Alice"
        owner.state.value.name.selection shouldBe TextRange(start = 4, end = 1)
        owner.state.value.description.text
            .toString() shouldBe "Notes"
        owner.state.value.description.selection shouldBe TextRange(index = 2)
    }

    @Test
    fun `named diffs use detached values even for empty or punctuation filled text`() {
        val owner = FormOwner()
        val capsule = capsule(owner = owner)
        capsule.record(state = owner.state.value)
        owner.state.value.name.edit {
            replace(start = 0, end = length, text = "a, (b) \"c\"\n\\d")
        }
        capsule.record(state = owner.state.value)
        val current = capsule.entries.last()
        val changes = current.changesFrom(previous = capsule.entries.first())

        changes shouldHaveSize 1
        changes.single().key shouldBe "name"
        changes.single().old shouldBe "\"\" (selection=0..0)"
        current.displayLabel.substring(range = changes.single().newValueRange) shouldBe
            changes.single().new

        owner.state.value.name
            .edit { replace(start = 0, end = length, text = "") }
        capsule.record(state = owner.state.value)
        capsule.entries
            .last()
            .changesFrom(previous = current)
            .single()
            .new shouldBe
            "\"\" (selection=0..0)"
    }

    @Test
    fun `custom screen labels and ordinary diffs coexist with named text diffs`() {
        val owner = FormOwner()
        val capsule = ScreenCapsule(
            owner = owner,
            label = { "count=${it.count}" },
            maxEntries = 10,
            textFields = listOf(FormState::name, FormState::description)
        )
        capsule.record(state = owner.state.value)
        owner.state.value.description.edit {
            replace(start = 0, end = length, text = "Notes")
        }
        owner.restoreState(state = owner.state.value.copy(count = 1))
        capsule.record(state = owner.state.value)
        val current = capsule.entries.last()
        val changes = current.changesFrom(previous = capsule.entries.first())

        changes.map { it.key } shouldBe listOf("count", "description")
        changes.forEach { change ->
            current.displayLabel.substring(range = change.newValueRange) shouldBe change.new
        }
    }

    @Test
    fun `recording stops when its job is cancelled`() = runTest {
        val owner = FormOwner()
        val capsule = capsule(owner = owner)
        val job = backgroundScope.launch { capsule.collectStates() }
        runCurrent()
        job.cancel()
        runCurrent()

        owner.state.value.name
            .edit { replace(start = 0, end = length, text = "Unobserved") }
        Snapshot.sendApplyNotifications()
        runCurrent()
        capsule.entries shouldHaveSize 1
    }

    @Test
    fun `unchanged editing values do not create duplicate entries`() = runTest {
        val owner = FormOwner()
        val capsule = capsule(owner = owner)
        backgroundScope.launch { capsule.collectStates() }
        runCurrent()

        owner.state.value.name
            .edit { selection = TextRange.Zero }
        Snapshot.sendApplyNotifications()
        runCurrent()

        capsule.entries shouldHaveSize 1
    }

    @Test
    fun `recording without declared fields still follows ordinary screen state`() = runTest {
        val owner = FormOwner()
        val capsule = ScreenCapsule(owner = owner, label = { "count=${it.count}" }, maxEntries = 10)
        backgroundScope.launch { capsule.collectStates() }
        runCurrent()
        owner.restoreState(state = owner.state.value.copy(count = 1))
        runCurrent()

        capsule.entries.map { it.label } shouldBe listOf("count=0", "count=1")
        capsule.entries.forEach { it.textFields shouldHaveSize 0 }
    }

    @Test
    fun `text history obeys retention and clear`() {
        val owner = FormOwner()
        val capsule = capsule(owner = owner, maxEntries = 2)
        repeat(times = 3) { index ->
            owner.state.value.name
                .edit { replace(start = 0, end = length, text = "$index") }
            capsule.record(state = owner.state.value)
        }
        capsule.entries.map { it.textFields.first().text } shouldBe listOf("1", "2")
        capsule.clear()
        capsule.entries shouldHaveSize 0
    }

    @Test
    fun `duplicate text field names are rejected`() {
        shouldThrow<IllegalArgumentException> {
            ScreenCapsule(
                owner = FormOwner(),
                label = { it.toString() },
                maxEntries = 10,
                textFields = listOf(FormState::name, FormState::name)
            )
        }
    }

    private fun capsule(owner: FormOwner, maxEntries: Int = 10) = ScreenCapsule(
        owner = owner,
        label = { it.toString() },
        maxEntries = maxEntries,
        textFields = listOf(FormState::name, FormState::description)
    )

    private data class FormState(
        val name: TextFieldState = TextFieldState(),
        val description: TextFieldState = TextFieldState(),
        val count: Int = 0
    )

    private class FormOwner(initial: FormState = FormState()) : TimeCapsuleOwner<FormState> {
        private val mutableState = MutableStateFlow(value = initial)
        override val state = mutableState.asStateFlow()

        override fun restoreState(state: FormState) {
            mutableState.value = state
        }
    }
}
