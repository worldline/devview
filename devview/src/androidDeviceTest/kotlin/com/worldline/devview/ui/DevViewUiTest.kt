package com.worldline.devview.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.matchers.shouldBe
import org.junit.Test

class DevViewUiTest {

    @Test
    fun filters_expose_count_and_clear_state() = runComposeUiTest {
        val count = mutableStateOf(value = 2)
        val expanded = mutableStateOf(value = false)
        setContent {
            MaterialTheme {
                DevViewFilterBar(
                    query = "",
                    onQueryChange = {},
                    placeholder = "Search events",
                    activeFilterCount = count.value,
                    expanded = expanded.value,
                    onExpandedChange = { expanded.value = it },
                    onClearFilters = { count.value = 0 }
                ) { Text(text = "Category filters") }
            }
        }
        onNodeWithContentDescription(label = "Show filters, 2 active").performClick()
        onNodeWithText(text = "Category filters").assertIsDisplayed()
        onNodeWithContentDescription(label = "Hide filters, 2 active").assertIsDisplayed()
        onNodeWithText(text = "Clear filters").assertIsEnabled().performClick()
        onNodeWithText(text = "Clear filters").assertIsNotEnabled()
        onNodeWithContentDescription(label = "Hide filters, 0 active").performClick()
        onNodeWithContentDescription(label = "Show filters, 0 active").assertIsDisplayed()
    }

    @Test
    fun section_header_exposes_heading_semantics() = runComposeUiTest {
        setContent { MaterialTheme { DevViewSectionHeader(text = "Categories") } }
        onNodeWithText(text = "Categories").assert(matcher = SemanticsMatcher.keyIsDefined(key = SemanticsProperties.Heading))
    }

    @Test
    fun empty_state_has_a_recovery_action() = runComposeUiTest {
        var cleared = false
        setContent {
            MaterialTheme {
                DevViewEmptyState(
                    icon = Icons.Rounded.Search,
                    title = "No results",
                    message = "Clear the filters to see all events.",
                    action = { TextButton(onClick = { cleared = true }) { Text(text = "Clear filters") } }
                )
            }
        }
        onNodeWithText(text = "No results").assertIsDisplayed()
        onNodeWithText(text = "Clear filters").performClick()
        runOnIdle { cleared shouldBe true }
    }
}
