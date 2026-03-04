package com.worldline.devview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults.exitUntilCollapsedScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationEventHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.savedstate.serialization.SavedStateConfiguration
import com.worldline.devview.core.HasTitle
import com.worldline.devview.core.Module
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
@OptIn(ExperimentalMaterial3Api::class)
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

    val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)

    NavigationEventHandler(
        state = navigationState,
        onBackCompleted = {
            if (backstack.size == 1 && backstack.first() == Home) {
                // Close DevView if we're at the root
                closeDevView()
            }
        }
    )

    val scrollBehaviour = exitUntilCollapsedScrollBehavior()

    val title: String? by remember(key1 = backstack) {
        derivedStateOf {
            if (backstack.last() is HasTitle) {
                (backstack.last() as HasTitle).title
            } else {
                null
            }
        }
    }

    AnimatedVisibility(
        visible = devViewIsOpen
    ) {
        Scaffold(
            modifier = modifier,
            topBar = {
                AnimatedVisibility(
                    visible = title != null
                ) {
                    title?.let {
                        MediumTopAppBar(
                            title = {
                                Text(text = it)
                            },
                            scrollBehavior = scrollBehaviour
                        )
                    }
                }
            }
        ) { padding ->
            val layoutDirection = LocalLayoutDirection.current
            val newPaddingValues = PaddingValues(
                start = padding.calculateStartPadding(layoutDirection = layoutDirection),
                top = padding.calculateTopPadding(),
                end = padding.calculateEndPadding(layoutDirection = layoutDirection)
            )

            NavDisplay(
                modifier = Modifier
                    .padding(
                        paddingValues = newPaddingValues
                    ).consumeWindowInsets(
                        paddingValues = newPaddingValues
                    ).nestedScroll(connection = scrollBehaviour.nestedScrollConnection),
                backStack = backstack,
                entryProvider = entryProvider {
                    // Home screen entry
                    entry<Home> {
                        HomeScreen(
                            modules = modules,
                            openModule = { module ->
                                // Navigate to the module's first destination
                                val firstDestination = module.destinations.firstOrNull()
                                if (firstDestination != null) {
                                    backstack.add(element = firstDestination)
                                }
                            }
                        )
                    }

                    // Register each module's content
                    modules.forEach { module ->
                        with(receiver = module) {
                            this@entryProvider.registerContent(
                                onNavigateBack = {
                                    backstack.removeLastOrNull()
                                },
                                onNavigate = { destination ->
                                    backstack.add(element = destination)
                                }
                            )
                        }
                    }
                }
            )
        }
    }
}
