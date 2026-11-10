package com.worldline.devview.timecapsule

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.test.StateRestorationTester
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test

class TimeCapsuleScreenTest {
    @Test
    fun current_entry_has_no_restore_but_earlier_entries_restore_the_owner() = runComposeUiTest {
        val owner = FakeOwner()
        val capsule = capsule(owner = owner)
        TimeCapsule.register(capsule = capsule)
        try {
            setContent { MaterialTheme { TimeCapsuleScreen() } }
            onNode(
                matcher = hasText(text = "Restore") and hasAnyAncestor(
                    matcher = hasTestTag(testTag = "time_capsule_row_1")
                )
            ).assertDoesNotExist()
            onNode(
                matcher = hasText(text = "Restore") and hasAnyAncestor(
                    matcher = hasTestTag(testTag = "time_capsule_row_0")
                )
            ).assertIsDisplayed().performClick()
            runOnIdle { owner.state.value shouldBe 1 }
        } finally {
            TimeCapsule.unregister(capsule = capsule)
        }
    }

    @Test
    fun expand_control_has_a_48dp_target_and_toggles_details() = runComposeUiTest {
        val capsule = capsule()
        TimeCapsule.register(capsule = capsule)
        try {
            setContent { MaterialTheme { TimeCapsuleScreen() } }
            onNodeWithTag(testTag = "time_capsule_expand_1")
                .assertHeightIsAtLeast(expectedMinHeight = 48.dp)
                .assertWidthIsAtLeast(expectedMinWidth = 48.dp)
                .performClick()
            onNodeWithContentDescription(label = "Collapse").assertIsDisplayed().performClick()
            onNodeWithContentDescription(label = "Expand").assertIsDisplayed()
        } finally {
            TimeCapsule.unregister(capsule = capsule)
        }
    }

    @Test
    fun expanded_entry_survives_saved_state_restoration() = runComposeUiTest {
        val capsule = capsule()
        TimeCapsule.register(capsule = capsule)
        try {
            val restoration = StateRestorationTester(this)
            restoration.setContent { MaterialTheme { TimeCapsuleScreen() } }
            onNodeWithTag(testTag = "time_capsule_expand_1").performClick()
            onNodeWithContentDescription(label = "Collapse").assertIsDisplayed()
            restoration.emulateSaveAndRestore()
            onNodeWithContentDescription(label = "Collapse").assertIsDisplayed()
            onNodeWithText(text = "Counter(count=2)").assertIsDisplayed()
        } finally {
            TimeCapsule.unregister(capsule = capsule)
        }
    }

    @Test
    fun a_new_recording_does_not_inherit_expansion_for_reused_entry_ids() = runComposeUiTest {
        val first = capsule()
        val second = capsule()
        TimeCapsule.register(capsule = first)
        try {
            setContent { MaterialTheme { TimeCapsuleScreen() } }
            onNodeWithTag(testTag = "time_capsule_expand_1").performClick()
            onNodeWithContentDescription(label = "Collapse").assertIsDisplayed()
            runOnIdle {
                TimeCapsule.register(capsule = second)
                TimeCapsule.unregister(capsule = first)
            }
            onNodeWithContentDescription(label = "Expand").assertIsDisplayed()
            onNodeWithText(text = "Counter(count=2)").assertDoesNotExist()
        } finally {
            TimeCapsule.unregister(capsule = first)
            TimeCapsule.unregister(capsule = second)
        }
    }

    @Test
    fun returning_after_a_recording_changed_does_not_restore_unrelated_expansion() = runComposeUiTest {
        val first = capsule()
        val second = capsule()
        val showScreen = mutableStateOf(value = true)
        TimeCapsule.register(capsule = first)
        try {
            setContent {
                val holder = rememberSaveableStateHolder()
                if (showScreen.value) {
                    holder.SaveableStateProvider(key = "timeline") {
                        MaterialTheme { TimeCapsuleScreen() }
                    }
                }
            }
            onNodeWithTag(testTag = "time_capsule_expand_1").performClick()
            onNodeWithContentDescription(label = "Collapse").assertIsDisplayed()
            runOnIdle { showScreen.value = false }
            runOnIdle {
                TimeCapsule.unregister(capsule = first)
                TimeCapsule.register(capsule = second)
                showScreen.value = true
            }
            onNodeWithContentDescription(label = "Expand").assertIsDisplayed()
            onNodeWithText(text = "Counter(count=2)").assertDoesNotExist()
        } finally {
            TimeCapsule.unregister(capsule = first)
            TimeCapsule.unregister(capsule = second)
        }
    }

    @Test
    fun restored_long_free_form_label_can_still_be_collapsed() = runComposeUiTest {
        val capsule = ScreenCapsule(
            owner = FakeOwner(),
            label = { "A long free-form state description ".repeat(n = 30) },
            maxEntries = 10
        ).apply { record(state = 1) }
        TimeCapsule.register(capsule = capsule)
        try {
            val restoration = StateRestorationTester(this)
            restoration.setContent { MaterialTheme { TimeCapsuleScreen() } }
            onNodeWithTag(testTag = "time_capsule_expand_0").performClick()
            restoration.emulateSaveAndRestore()
            onNodeWithContentDescription(label = "Collapse").performScrollTo().performClick()
            onNodeWithContentDescription(label = "Expand").assertIsDisplayed()
        } finally {
            TimeCapsule.unregister(capsule = capsule)
        }
    }

    @Test
    fun ordinary_recomposition_keeps_expansion() = runComposeUiTest {
        val capsule = capsule()
        val bottomPadding = mutableStateOf(value = 0.dp)
        TimeCapsule.register(capsule = capsule)
        try {
            setContent { MaterialTheme { TimeCapsuleScreen(bottomPadding = bottomPadding.value) } }
            onNodeWithTag(testTag = "time_capsule_expand_1").performClick()
            runOnIdle { bottomPadding.value = 16.dp }
            onNodeWithContentDescription(label = "Collapse").assertIsDisplayed()
        } finally {
            TimeCapsule.unregister(capsule = capsule)
        }
    }

    @Test
    fun empty_recording_explains_when_states_will_appear() = runComposeUiTest {
        val capsule = ScreenCapsule(owner = FakeOwner(), label = { it.toString() }, maxEntries = 10)
        TimeCapsule.register(capsule = capsule)
        try {
            setContent { MaterialTheme { TimeCapsuleScreen() } }
            onNodeWithText(text = "No states captured yet").assertIsDisplayed()
            onNodeWithText(
                text = "State changes will appear here while this screen is recording."
            ).assertIsDisplayed()
        } finally {
            TimeCapsule.unregister(capsule = capsule)
        }
    }

    @Test
    fun no_recording_explains_how_to_start() = runComposeUiTest {
        setContent { MaterialTheme { TimeCapsuleScreen() } }
        onNodeWithText(text = "No screen is recording").assertIsDisplayed()
        onNodeWithText(
            text = "Add TimeCapsuleEffect(owner) to a screen to record its state history."
        ).assertIsDisplayed()
    }

    private fun capsule(owner: FakeOwner = FakeOwner()): ScreenCapsule<Int> = ScreenCapsule(
        owner = owner,
        label = { "Counter(count=$it)" },
        maxEntries = 10
    ).apply {
        record(state = 1)
        record(state = 2)
    }

    private class FakeOwner : TimeCapsuleOwner<Int> {
        override val state = MutableStateFlow(value = 2)

        override fun restoreState(state: Int) {
            this.state.value = state
        }
    }
}
