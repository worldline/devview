package com.worldline.devview.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import kotlin.reflect.KClass
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import org.junit.Test

class ModuleRegistryUiTest {

    @Test
    fun rememberModules_builds_modules_in_order_and_calls_initModule() = runComposeUiTest {
        val initCalls = mutableListOf<String>()
        lateinit var builtModules: List<Module>

        setContent {
            builtModules = rememberModules {
                module(TrackingModule(name = "A", initCalls = initCalls))
                module(TrackingModule(name = "B", initCalls = initCalls))
            }
        }

        runOnIdle {
            builtModules.map { it.moduleName }.shouldContainExactly("A", "B")
            initCalls.shouldContainExactly("A", "B")
        }
    }

    @Test
    fun rememberModules_keeps_effects_active_on_recomposition_and_disposes_on_removal() = runComposeUiTest {
        val initCalls = mutableListOf<String>()
        lateinit var builtModules: List<Module>
        lateinit var triggerRecompose: () -> Unit
        val visible = mutableStateOf(value = true)

        setContent {
            val recomposeTick = remember { mutableIntStateOf(value = 0) }
            triggerRecompose = { recomposeTick.intValue++ }
            recomposeTick.intValue

            if (visible.value) {
                builtModules = rememberModules {
                    module(TrackingModule(name = "A", initCalls = initCalls))
                    module(TrackingModule(name = "B", initCalls = initCalls))
                }
            }
        }

        runOnIdle {
            initCalls.shouldContainExactly("A", "B")
            triggerRecompose()
        }

        waitForIdle()

        runOnIdle {
            builtModules.map { it.moduleName }.shouldContainExactly("A", "B")
            initCalls.shouldContainExactly("A", "B")
            visible.value = false
        }
        waitForIdle()
        runOnIdle {
            initCalls.shouldContainExactlyInAnyOrder("A", "B", "dispose A", "dispose B")
            visible.value = true
        }
        waitForIdle()
        runOnIdle {
            initCalls.shouldContainExactlyInAnyOrder("A", "B", "dispose A", "dispose B", "A", "B")
        }
    }
}

private data object UiTestDestination : NavKey

private class TrackingModule(
    name: String,
    private val initCalls: MutableList<String>
) : Module {
    override val moduleName: String = name
    override val section: Section = Section.CUSTOM
    override val destinations: PersistentMap<KClass<out NavKey>, DestinationMetadata> =
        persistentMapOf(UiTestDestination::class to DestinationMetadata())
    override val entryDestination: NavKey
        get() = UiTestDestination
    override val registerSerializers: PolymorphicModuleBuilder<NavKey>.() -> Unit = {}

    override fun EntryProviderScope<NavKey>.registerContent(
        onNavigateBack: () -> Unit,
        onNavigate: (NavKey) -> Unit,
        bottomPadding: androidx.compose.ui.unit.Dp
    ) = Unit

    @Composable
    override fun initModule() {
        DisposableEffect(key1 = Unit) {
            initCalls.add(moduleName)
            onDispose { initCalls.add("dispose $moduleName") }
        }
    }
}
