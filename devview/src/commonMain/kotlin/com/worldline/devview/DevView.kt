package com.worldline.devview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationEventHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.savedstate.serialization.SavedStateConfiguration
import com.worldline.devview.core.Module
import kotlinx.collections.immutable.ImmutableList
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * Main entry point for DevView.
 *
 * @param openDevView Callback that returns true when DevView should be shown
 * @param closeDevView Callback to close DevView
 * @param modules List of modules to display (use [com.worldline.devview.core.rememberModules] to build)
 * @param modifier Optional modifier
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

    AnimatedVisibility(
        visible = devViewIsOpen
    ) {
        Scaffold(
            modifier = modifier
        ) {
            NavDisplay(
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
