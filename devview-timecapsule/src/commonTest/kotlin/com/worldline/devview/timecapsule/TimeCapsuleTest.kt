package com.worldline.devview.timecapsule

import com.worldline.devview.core.Section
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class TimeCapsuleTest {

    @Test
    fun `time capsule module exposes expected metadata`() {
        TimeCapsule.section shouldBe Section.LOGGING
        TimeCapsule.destinations.keys.shouldContain(TimeCapsule.entryDestination::class)

        val metadata = TimeCapsule.destinations[TimeCapsuleDestination.Main::class].shouldNotBeNull()
        metadata.title shouldBe "Time Capsule"
        metadata.actions shouldHaveSize 1
    }

    @Test
    fun `current is null when no screen is recording`() {
        TimeCapsule.current.shouldBeNull()
    }

    @Test
    fun `current returns the most recently registered capsule`() {
        val capsuleA = newCapsule()
        val capsuleB = newCapsule()

        TimeCapsule.register(capsule = capsuleA)
        TimeCapsule.register(capsule = capsuleB)
        TimeCapsule.unregister(capsule = capsuleA)

        TimeCapsule.current shouldBe capsuleB

        TimeCapsule.unregister(capsule = capsuleB)
    }

    @Test
    fun `unregistering the last capsule clears current`() {
        val capsule = newCapsule()
        TimeCapsule.register(capsule = capsule)

        TimeCapsule.unregister(capsule = capsule)

        TimeCapsule.current.shouldBeNull()
    }

    private fun newCapsule(): ScreenCapsule<Int> = ScreenCapsule(
        owner = FakeOwner(),
        label = { it.toString() },
        maxEntries = 10
    )

    private class FakeOwner : TimeCapsuleOwner<Int> {
        private val _state = MutableStateFlow(value = 0)
        override val state: StateFlow<Int> = _state.asStateFlow()
        override fun restoreState(state: Int) {
            _state.value = state
        }
    }
}
