package com.worldline.devview.internal.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.worldline.devview.TestModule
import com.worldline.devview.core.Module
import io.kotest.matchers.shouldBe
import org.junit.Test

class ModuleItemUiTest {

    @Test
    fun moduleItem_displays_title_and_subtitle_and_handles_click() = runComposeUiTest {
        var opened: Module? = null
        val module = TestModule(name = "Network Mock", subtitle = "Inspect requests")

        setContent {
            ModuleItem(
                module = module,
                position = ModulePosition.SINGLE,
                openModule = { opened = it }
            )
        }

        onNodeWithTag(testTag = "module_name_Network Mock", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithTag(testTag = "module_subtitle_Network Mock", useUnmergedTree = true).assertIsDisplayed()

        onNodeWithTag(testTag = "module_name_Network Mock", useUnmergedTree = true).performClick()

        runOnIdle {
            opened shouldBe module
        }
    }

    @Test
    fun moduleItem_hides_subtitle_when_not_provided() = runComposeUiTest {
        val module = TestModule(name = "Analytics", subtitle = null)

        setContent {
            ModuleItem(
                module = module,
                position = ModulePosition.SINGLE,
                openModule = {}
            )
        }

        onNodeWithTag(testTag = "module_name_Analytics", useUnmergedTree = true).assertIsDisplayed()
        onAllNodesWithTag(testTag = "module_subtitle_Analytics", useUnmergedTree = true).assertCountEquals(0)
    }
}
