package com.worldline.devview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import com.worldline.devview.core.ModuleDestinationActionPopup
import com.worldline.devview.core.Section
import com.worldline.devview.core.withActions
import com.worldline.devview.core.withTitle
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    @Test
    fun devView_navigation_buttons_pop_and_close() = runComposeUiTest {
        var closeCount = 0
        setContent {
            DevView(
                devViewIsOpen = true,
                closeDevView = { closeCount++ },
                modules = persistentListOf(DevViewModule)
            )
        }
        onNodeWithTag(testTag = "module_item_${DevViewModule.moduleName}").performClick()
        onNodeWithContentDescription(label = "Back").performClick()
        onNodeWithTag(testTag = "module_item_${DevViewModule.moduleName}").assertIsDisplayed()
        onNodeWithContentDescription(label = "Close DevView").performClick()
        runOnIdle { assertEquals(expected = 1, actual = closeCount) }
    }

    @Test
    fun devView_labelled_action_receives_displayed_key_after_confirmation() = runComposeUiTest {
        var receivedKey: NavKey? = null
        val module = ActionModule(onAction = { receivedKey = it })
        setContent {
            DevView(devViewIsOpen = true, closeDevView = {}, modules = persistentListOf(module))
        }
        onNodeWithTag(testTag = "module_item_${module.moduleName}").performClick()
        onNodeWithContentDescription(label = "Refresh item").performClick()
        runOnIdle { assertEquals(expected = null, actual = receivedKey) }
        onNodeWithText(text = "Refresh now?").assertIsDisplayed()
        onNodeWithText(text = "Confirm").performClick()
        runOnIdle { assertEquals(expected = ActionDestination("selected"), actual = receivedKey) }
    }

    @Test
    fun devView_outgoing_title_stays_with_content_during_transition() = runComposeUiTest {
        val module = ActionModule(onAction = {})
        setContent {
            DevView(devViewIsOpen = true, closeDevView = {}, modules = persistentListOf(module))
        }
        onNodeWithTag(testTag = "module_item_${module.moduleName}").performClick()
        onNodeWithText(text = "Item details").assertIsDisplayed()
        onNodeWithText(text = "Selected item").assertIsDisplayed()
        mainClock.autoAdvance = false
        onNodeWithContentDescription(label = "Back").performClick()
        mainClock.advanceTimeBy(milliseconds = 32)
        onNodeWithText(text = "Item details").assertExists()
        onNodeWithText(text = "Selected item").assertExists()
        mainClock.autoAdvance = true
        waitForIdle()
        onNodeWithText(text = "Item details").assertDoesNotExist()
        onNodeWithTag(testTag = "module_item_${module.moduleName}").assertIsDisplayed()
    }

    @Test
    fun devView_module_replacement_updates_content_and_toolbar_together() = runComposeUiTest {
        var receivedKey: NavKey? = null
        var module by mutableStateOf(value = ActionModule(onAction = {}, contentLabel = "Original item"))
        setContent {
            DevView(devViewIsOpen = true, closeDevView = {}, modules = persistentListOf(module))
        }
        onNodeWithTag(testTag = "module_item_${module.moduleName}").performClick()
        onNodeWithText(text = "Original item").assertIsDisplayed()
        runOnIdle {
            module = ActionModule(onAction = { receivedKey = it }, contentLabel = "Replacement item")
        }
        onNodeWithText(text = "Replacement item").assertIsDisplayed()
        onNodeWithText(text = "Original item").assertDoesNotExist()
        onNodeWithContentDescription(label = "Refresh item").performClick()
        onNodeWithText(text = "Confirm").performClick()
        runOnIdle { assertEquals(expected = ActionDestination(id = "selected"), actual = receivedKey) }
    }

    @Test
    fun devView_navigation_dismisses_outgoing_entry_confirmation() = runComposeUiTest {
        lateinit var navigate: (NavKey) -> Unit
        val module = ActionModule(onAction = {}, onNavigationReady = { navigate = it })
        setContent {
            DevView(devViewIsOpen = true, closeDevView = {}, modules = persistentListOf(module))
        }
        onNodeWithTag(testTag = "module_item_${module.moduleName}").performClick()
        onNodeWithContentDescription(label = "Refresh item").performClick()
        onNodeWithText(text = "Refresh now?").assertIsDisplayed()
        runOnIdle { navigate(ActionDestination(id = "next")) }
        onNodeWithText(text = "Refresh now?").assertDoesNotExist()
        onNodeWithContentDescription(label = "Back").performClick()
        onNodeWithText(text = "Selected item").assertIsDisplayed()
        onNodeWithText(text = "Refresh now?").assertDoesNotExist()
    }

    @Test
    fun devView_menu_action_opens_dropdown_and_invokes_selected_item() = runComposeUiTest {
        var selectedLabel: String? = null

        setContent {
            DevView(
                devViewIsOpen = true,
                closeDevView = {},
                modules = persistentListOf(MenuModule(onItemSelected = { selectedLabel = it }))
            )
        }

        onNodeWithTag(testTag = "module_item_${MenuModule.MODULE_NAME}").performClick()

        // The menu items are not part of the tree until the toolbar icon is tapped
        onAllNodesWithText(text = "Path").assertCountEquals(0)

        onNodeWithTag(testTag = "top_bar_action_0").performClick()

        onNodeWithText(text = "Path").assertIsDisplayed()
        onNodeWithText(text = "Method").assertIsDisplayed()

        onNodeWithText(text = "Method").performClick()

        // Selecting an item collapses the menu and invokes its callback exactly once
        onAllNodesWithText(text = "Path").assertCountEquals(0)
        runOnIdle { assertEquals(expected = "Method", actual = selectedLabel) }
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

@Serializable
private data class ActionDestination(val id: String) : NavKey

private class ActionModule(
    private val onAction: (NavKey) -> Unit,
    private val contentLabel: String = "Selected item",
    private val onNavigationReady: ((NavKey) -> Unit) -> Unit = {}
) : Module {
    override val moduleName: String = "Action module"
    override val section: Section = Section.CUSTOM
    override val destinations: PersistentMap<KClass<out NavKey>, DestinationMetadata> = persistentMapOf(
        ActionDestination::class.withTitle(title = "Item details") {
            action(
                label = "Refresh item",
                icon = Icons.Rounded.Refresh,
                popup = ModuleDestinationActionPopup(
                    title = "Refresh now?",
                    confirmButton = "Confirm",
                    dismissButton = "Cancel"
                ),
                action = onAction
            )
        }
    )
    override val entryDestination: NavKey = ActionDestination("selected")
    override val registerSerializers: PolymorphicModuleBuilder<NavKey>.() -> Unit = {
        subclass(ActionDestination::class, ActionDestination.serializer())
    }

    override fun EntryProviderScope<NavKey>.registerContent(
        onNavigateBack: () -> Unit,
        onNavigate: (NavKey) -> Unit,
        bottomPadding: Dp
    ) {
        entry<ActionDestination> {
            LaunchedEffect(key1 = onNavigate) { onNavigationReady(onNavigate) }
            Text(text = contentLabel)
        }
    }
}

@Serializable
private data object MenuDestination : NavKey

private class MenuModule(private val onItemSelected: (String) -> Unit) : Module {
    override val moduleName: String = MODULE_NAME
    override val section: Section = Section.NETWORK
    override val destinations: PersistentMap<KClass<out NavKey>, DestinationMetadata> =
        persistentMapOf(
            MenuDestination.withActions {
                menu(label = "Sort", icon = Icons.AutoMirrored.Default.Sort) {
                    item(label = "Path") { onItemSelected("Path") }
                    item(label = "Method") { onItemSelected("Method") }
                }
            }
        )
    override val entryDestination: NavKey
        get() = MenuDestination
    override val registerSerializers: PolymorphicModuleBuilder<NavKey>.() -> Unit = {}

    override fun EntryProviderScope<NavKey>.registerContent(
        onNavigateBack: () -> Unit,
        onNavigate: (NavKey) -> Unit,
        bottomPadding: Dp
    ) {
        entry<MenuDestination> {
            Box(modifier = Modifier.fillMaxSize())
        }
    }

    companion object {
        const val MODULE_NAME: String = "Menu Module"
    }
}

