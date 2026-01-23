package com.worldline.devview

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.collections.immutable.ImmutableList
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Composable
public fun DevView(
    openDevView: () -> Boolean,
    closeDevView: () -> Unit,
    modules: ImmutableList<Module>,
    modifier: Modifier = Modifier
) {
    val backstack = rememberNavBackStack(
        configuration = SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(baseClass = NavKey::class) {
                    subclass(subclass = Home::class, serializer = Home.serializer())
                    modules.forEach { module ->
                        module.asSubclass
                    }
                }
            }
        },
        Home
    )

    if (openDevView()) {
        Scaffold(
            modifier = modifier
        ) {
            NavDisplay(
                backStack = backstack,
                onBack = {
                    backstack.removeLastOrNull()
                },
                entryProvider = entryProvider {
                    entry<Home> {
                        HomeScreen(
                            modules = modules,
                            openModule = { module ->
                                backstack.add(element = module)
                            }
                        )
                    }
                }
            )
        }
    }
}
