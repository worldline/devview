package com.worldline.devview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToLog
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.worldline.devview.core.DestinationMetadata
import com.worldline.devview.core.Module
import com.worldline.devview.core.Section
import com.worldline.devview.core.withTitle
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import org.junit.Test

class DevViewTest {

    @Test
    fun devView_hidden_does_not_render_home_content() = runComposeUiTest {
        setContent {
            DevView(
                devViewIsOpen = false,
                closeDevView = {},
                modules = persistentListOf(DevViewModule)
            )
        }

        onAllNodesWithTag(testTag = "module_item_${DevViewModule.moduleName}").assertCountEquals(0)
    }

    @Test
    fun devView_open_navigates_to_module_first_destination() = runComposeUiTest {
        setContent {
            DevView(
                devViewIsOpen = true,
                closeDevView = {},
                modules = persistentListOf(DevViewModule)
            )
        }

        onNodeWithTag(testTag = "module_item_${DevViewModule.moduleName}").assertIsDisplayed()

        onNodeWithTag(testTag = "module_item_${DevViewModule.moduleName}").performClick()

        onNodeWithText(text = "Network Mock Screen").assertIsDisplayed()
    }
}

@Serializable
private data object DevViewDestination : NavKey

private data object DevViewModule : Module {
    override val moduleName: String = "Network Mock"
    override val section: Section = Section.NETWORK
    override val destinations: PersistentMap<NavKey, DestinationMetadata> =
        persistentMapOf(DevViewDestination.withTitle(title = "Network Mock Screen"))
    override val registerSerializers: PolymorphicModuleBuilder<NavKey>.() -> Unit = {}

    override fun EntryProviderScope<NavKey>.registerContent(
        onNavigateBack: () -> Unit,
        onNavigate: (NavKey) -> Unit,
        bottomPadding: Dp
    ) {
        entry<DevViewDestination> {
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}
