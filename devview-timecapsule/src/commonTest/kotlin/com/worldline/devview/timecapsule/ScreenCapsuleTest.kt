package com.worldline.devview.timecapsule

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class ScreenCapsuleTest {

    @Test
    fun `record appends a labelled entry per call`() {
        val capsule = ScreenCapsule(
            owner = FakeOwner(initial = CounterState(count = 0)),
            label = { "Count=${it.count}" },
            maxEntries = 10
        )

        capsule.record(state = CounterState(count = 1))
        capsule.record(state = CounterState(count = 2))

        capsule.entries.map { it.label } shouldBe listOf("Count=1", "Count=2")
        capsule.entries.map { it.state } shouldBe listOf(CounterState(count = 1), CounterState(count = 2))
    }

    @Test
    fun `retention cap drops oldest entry`() {
        val capsule = ScreenCapsule(
            owner = FakeOwner(initial = CounterState(count = 0)),
            label = { it.count.toString() },
            maxEntries = 2
        )

        capsule.record(state = CounterState(count = 1))
        capsule.record(state = CounterState(count = 2))
        capsule.record(state = CounterState(count = 3))

        capsule.entries.map { it.state.count } shouldBe listOf(2, 3)
    }

    @Test
    fun `restore pushes the recorded state back to the owner`() {
        val owner = FakeOwner(initial = CounterState(count = 0))
        val capsule = ScreenCapsule(owner = owner, label = { it.count.toString() }, maxEntries = 10)

        capsule.record(state = CounterState(count = 5))
        capsule.record(state = CounterState(count = 10))

        capsule.restore(id = capsule.entries.first().id)

        owner.restoredTo shouldBe CounterState(count = 5)
    }

    @Test
    fun `restore with an unknown id is a no-op`() {
        val owner = FakeOwner(initial = CounterState(count = 0))
        val capsule = ScreenCapsule(owner = owner, label = { it.count.toString() }, maxEntries = 10)

        capsule.record(state = CounterState(count = 5))
        capsule.restore(id = 999L)

        owner.restoredTo.shouldBeNull()
    }

    @Test
    fun `clear empties the timeline`() {
        val capsule = ScreenCapsule(
            owner = FakeOwner(initial = CounterState(count = 0)),
            label = { it.count.toString() },
            maxEntries = 10
        )

        capsule.record(state = CounterState(count = 1))
        capsule.clear()

        capsule.entries.shouldBeEmpty()
    }

    private data class CounterState(val count: Int)

    private class FakeOwner(initial: CounterState) : TimeCapsuleOwner<CounterState> {
        private val _state = MutableStateFlow(value = initial)
        override val state: StateFlow<CounterState> = _state.asStateFlow()

        var restoredTo: CounterState? = null
            private set

        override fun restoreState(state: CounterState) {
            restoredTo = state
            _state.value = state
        }
    }
}
