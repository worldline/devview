package com.worldline.devview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import com.worldline.devview.core.DestinationMetadata
import com.worldline.devview.core.Module
import com.worldline.devview.core.Section
import com.worldline.devview.core.withTitle
import kotlin.reflect.KClass
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    @Test
    fun devView_open_handles_back_before_host_navigation() = runComposeUiTest {
        lateinit var navigationInput: DirectNavigationEventInput
        var devViewIsOpen by mutableStateOf(value = false)
        var hostBackCount = 0

        setContent {
            val navigationOwner = rememberNavigationEventDispatcherOwner(parent = null)
            navigationInput = remember { DirectNavigationEventInput() }
            val hostHandler = remember {
                object : NavigationEventHandler<NavigationEventInfo>(
                    initialInfo = NavigationEventInfo.None,
                    isBackEnabled = true
                ) {
                    override fun onBackCompleted() {
                        hostBackCount++
                    }
                }
            }

            CompositionLocalProvider(
                LocalNavigationEventDispatcherOwner provides navigationOwner
            ) {
                DisposableEffect(Unit) {
                    navigationOwner.navigationEventDispatcher.addHandler(handler = hostHandler)
                    navigationOwner.navigationEventDispatcher.addInput(
                        input = navigationInput,
                        priority = NavigationEventDispatcher.PRIORITY_DEFAULT
                    )
                    onDispose {
                        hostHandler.remove()
                        navigationOwner.navigationEventDispatcher.removeInput(
                            input = navigationInput
                        )
                    }
                }
                DevView(
                    devViewIsOpen = devViewIsOpen,
                    closeDevView = { devViewIsOpen = false },
                    modules = persistentListOf(DevViewModule)
                )
            }
        }

        runOnIdle { devViewIsOpen = true }
        onNodeWithTag(testTag = "module_item_${DevViewModule.moduleName}").performClick()
        runOnIdle { navigationInput.backCompleted() }

        onNodeWithTag(testTag = "module_item_${DevViewModule.moduleName}").assertIsDisplayed()
        runOnIdle {
            assertEquals(expected = 0, actual = hostBackCount)
            navigationInput.backCompleted()
        }

        runOnIdle {
            assertFalse(actual = devViewIsOpen)
            assertEquals(expected = 0, actual = hostBackCount)
            navigationInput.backCompleted()
            assertEquals(expected = 1, actual = hostBackCount)
        }
    }
}

@Serializable
private data object DevViewDestination : NavKey

private data object DevViewModule : Module {
    override val moduleName: String = "Network Mock"
    override val section: Section = Section.NETWORK
    override val destinations: PersistentMap<KClass<out NavKey>, DestinationMetadata> =
        persistentMapOf(DevViewDestination.withTitle(title = "Network Mock Screen"))
    override val entryDestination: NavKey
        get() = DevViewDestination
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
