package com.worldline.devview

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.worldline.devview.core.Module
import com.worldline.devview.core.Section
import io.kotest.matchers.shouldBe
import org.junit.Test

class HomeScreenTest {

    @Test
    fun homeScreen_displays_section_headers_and_module_names() = runComposeUiTest {
        val modules = listOf(
            TestModule(name = "App Info", section = Section.SETTINGS),
            TestModule(name = "Feature Flags", section = Section.FEATURES),
            TestModule(name = "Console", section = Section.LOGGING)
        )

        setContent {
            HomeScreen(
                modules = modules,
                openModule = {}
            )
        }

        onNodeWithTag(testTag = "section_header_SETTINGS").assertIsDisplayed()
        onNodeWithTag(testTag = "section_header_FEATURES").assertIsDisplayed()
        onNodeWithTag(testTag = "section_header_LOGGING").assertIsDisplayed()

        onNodeWithText("App Info").assertIsDisplayed()
        onNodeWithText("Feature Flags").assertIsDisplayed()
        onNodeWithText("Console").assertIsDisplayed()
    }

    @Test
    fun homeScreen_calls_openModule_with_clicked_module() = runComposeUiTest {
        val analytics = TestModule(name = "Analytics", section = Section.LOGGING)
        val featureFlip = TestModule(name = "Feature Flip", section = Section.FEATURES)
        var opened: Module? = null

        setContent {
            HomeScreen(
                modules = listOf(featureFlip, analytics),
                openModule = { opened = it }
            )
        }

        onNodeWithTag(testTag = "module_item_Analytics").performClick()

        runOnIdle {
            opened shouldBe analytics
        }
    }
}
