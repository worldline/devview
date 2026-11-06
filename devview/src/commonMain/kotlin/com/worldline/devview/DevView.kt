package com.worldline.devview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import androidx.savedstate.serialization.SavedStateConfiguration
import com.worldline.devview.core.Module
import com.worldline.devview.internal.DESTINATION_METADATA_KEY
import com.worldline.devview.internal.LocalEntryBottomPadding
import com.worldline.devview.internal.destinationScaffoldDecorator
import kotlinx.collections.immutable.ImmutableList
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * Main entry point for the DevView developer tools UI.
 *
 * DevView provides an in-app developer menu that displays a collection of modules
 * for debugging, testing, and development. It uses an animated visibility wrapper
 * and manages its own navigation stack with type-safe navigation.
 *
 * ## Features
 * - **Modular Architecture**: Display any combination of modules
 * - **Type-Safe Navigation**: Uses Navigation3 with kotlinx.serialization
 * - **Animated Transitions**: Smooth show/hide animations
 * - **Persistent State**: Navigation state survives configuration changes
 * - **Section-Based Organization**: Modules grouped by logical sections
 *
 * ## Architecture
 *
 * DevView manages a navigation backstack that includes:
 * 1. **Home screen** ([Home]): Displays all modules grouped by section
 * 2. **Module destinations**: Screens registered by individual modules
 *
 * When a user:
 * - Opens DevView → Shows the home screen
 * - Taps a module → Navigates to that module's first destination
 * - Navigates back from home → Closes DevView
 * - Navigates within a module → Uses module-specific navigation
 *
 * ## Usage
 *
 * ### Basic Setup
 * ```kotlin
 * @Composable
 * fun App() {
 *     var isDevViewOpen by remember { mutableStateOf(false) }
 *
 *     val modules = rememberModules {
 *         module(AppInfo)
 *         module(FeatureFlip)
 *         module(Analytics)
 *     }
 *
 *     Box {
 *         // Your main app content
 *         MainAppContent()
 *
 *         // DevView overlay
 *         DevView(
 *             devViewIsOpen = isDevViewOpen,
 *             closeDevView = { isDevViewOpen = false },
 *             modules = modules
 *         )
 *
 *         // Trigger to open DevView (e.g., debug button)
 *         if (BuildConfig.DEBUG) {
 *             FloatingActionButton(
 *                 onClick = { isDevViewOpen = true }
 *             ) {
 *                 Icon(Icons.Default.DeveloperMode, "DevView")
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ### With Gesture Detection
 * ```kotlin
 * @Composable
 * fun App() {
 *     var isDevViewOpen by remember { mutableStateOf(false) }
 *     var tapCount by remember { mutableStateOf(0) }
 *
 *     val modules = rememberModules {
 *         module(FeatureFlip)
 *         module(Analytics)
 *     }
 *
 *     Box(
 *         modifier = Modifier
 *             .fillMaxSize()
 *             .pointerInput(Unit) {
 *                 detectTapGestures(
 *                     onDoubleTap = {
 *                         tapCount++
 *                         if (tapCount >= 3) {
 *                             isDevViewOpen = true
 *                             tapCount = 0
 *                         }
 *                     }
 *                 )
 *             }
 *     ) {
 *         MainContent()
 *
 *         DevView(
 *             devViewIsOpen = isDevViewOpen,
 *             closeDevView = { isDevViewOpen = false },
 *             modules = modules
 *         )
 *     }
 * }
 * ```
 *
 * ### Conditional Module Loading
 * ```kotlin
 * @Composable
 * fun App() {
 *     val modules = rememberModules {
 *         // Always include these
 *         module(AppInfo)
 *
 *         // Debug-only modules
 *         if (BuildConfig.DEBUG) {
 *             modules(DebugConsole, NetworkMonitor)
 *         }
 *
 *         // Feature-flag controlled
 *         if (isFeatureEnabled("dev_tools")) {
 *             module(FeatureFlip)
 *         }
 *     }
 *
 *     DevView(
 *         devViewIsOpen = devViewState,
 *         closeDevView = { devViewState = false },
 *         modules = modules
 *     )
 * }
 * ```
 *
 * ## Navigation Behavior
 *
 * - **Opening**: Displays with animation when [devViewIsOpen] becomes true
 * - **Module Selection**: Navigates to the module's first destination
 * - **Back Navigation**: Pops the backstack; closes DevView when at home
 * - **State Persistence**: Navigation state survives configuration changes
 *
 * ## Performance Notes
 *
 * - DevView uses [AnimatedVisibility], so content is composed/decomposed
 * - Module list should be remembered to avoid recreating on recomposition
 * - Navigation state is automatically saved and restored
 *
 * @param devViewIsOpen Whether DevView should be visible. Control this state
 *        from your app to show/hide DevView.
 * @param closeDevView Callback invoked when DevView should be closed, typically
 *        when navigating back from the home screen. Set [devViewIsOpen] to false
 *        in this callback.
 * @param modules The list of modules to display in DevView. Use the rememberModules
 *        function to build this list in a Composable, or buildModules for non-Composable contexts.
 * @param modifier Optional [Modifier] to apply to the root DevView container.
 *
 * @see Module
 * @see com.worldline.devview.core.rememberModules
 * @see com.worldline.devview.core.buildModules
 * @see Home
 */
@Composable
public fun DevView(
    devViewIsOpen: Boolean,
    closeDevView: () -> Unit,
    modules: ImmutableList<Module>,
    modifier: Modifier = Modifier
) {
    val backstack = rememberNavBackStack(
        configuration = SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(baseClass = NavKey::class) {
                    subclass(subclass = Home::class, serializer = Home.serializer())
                    // Register all module destination serializers
                    modules.forEach { module ->
                        module.registerSerializers(this)
                    }
                }
            }
        },
        Home
    )

    val overlayNavigationOwner = rememberNavigationEventDispatcherOwner()
    SideEffect { overlayNavigationOwner.navigationEventDispatcher.isEnabled = devViewIsOpen }
    OverlayBackHandler(
        dispatcher = overlayNavigationOwner.navigationEventDispatcher,
        isEnabled = devViewIsOpen && backstack.size == 1,
        onBack = closeDevView
    )

    val currentModules by rememberUpdatedState(newValue = modules)
    val onNavigateBack: () -> Unit = {
        if (backstack.size > 1) backstack.removeLastOrNull()
    }

    AnimatedVisibility(visible = devViewIsOpen) {
        CompositionLocalProvider(
            value = LocalNavigationEventDispatcherOwner provides overlayNavigationOwner
        ) {
            NavDisplay(
                modifier = modifier,
                backStack = backstack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                    destinationScaffoldDecorator(
                        modules = modules,
                        activeDestination = backstack.last(),
                        onNavigateBack = onNavigateBack,
                        closeDevView = closeDevView
                    )
                ),
                onBack = onNavigateBack,
                entryProvider = { destination ->
                    if (destination == Home) {
                        NavEntry(
                            key = destination,
                            metadata = mapOf(
                                pairs = arrayOf(DESTINATION_METADATA_KEY to destination)
                            )
                        ) {
                            HomeScreen(
                                modules = modules,
                                openModule = { module ->
                                    backstack.add(
                                        element = module.entryDestination
                                    )
                                }
                            )
                        }
                    } else {
                        fun moduleProvider(bottomPadding: Dp): (NavKey) -> NavEntry<NavKey> =
                            entryProvider(builder = {
                                with(
                                    receiver = currentModules.first {
                                        destination::class in
                                            it.destinations
                                    }
                                ) {
                                    registerContent(
                                        onNavigateBack = onNavigateBack,
                                        onNavigate = { backstack.add(element = it) },
                                        bottomPadding = bottomPadding
                                    )
                                }
                            })
                        val template = moduleProvider(bottomPadding = 0.dp)(destination)
                        NavEntry(
                            key = destination,
                            contentKey = template.contentKey,
                            metadata = template.metadata + (DESTINATION_METADATA_KEY to destination)
                        ) { key ->
                            // Registration captures Dp, so resolve it inside the decorated entry's composition.
                            moduleProvider(
                                bottomPadding = LocalEntryBottomPadding.current
                            )(key).Content()
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun OverlayBackHandler(
    dispatcher: NavigationEventDispatcher,
    isEnabled: Boolean,
    onBack: () -> Unit
) {
    val currentOnBack by rememberUpdatedState(newValue = onBack)
    val handler = remember {
        object : NavigationEventHandler<NavigationEventInfo>(
            initialInfo = NavigationEventInfo.None,
            isBackEnabled = false
        ) {
            override fun onBackCompleted() {
                currentOnBack()
            }
        }
    }

    SideEffect {
        handler.isBackEnabled = isEnabled
    }

    DisposableEffect(key1 = dispatcher, key2 = handler) {
        dispatcher.addHandler(
            handler = handler,
            priority = NavigationEventDispatcher.PRIORITY_OVERLAY
        )
        onDispose { handler.remove() }
    }
}
