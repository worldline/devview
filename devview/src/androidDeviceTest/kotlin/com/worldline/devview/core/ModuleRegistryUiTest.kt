package com.worldline.devview.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.kotest.matchers.collections.shouldContainExactly
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
    fun rememberModules_does_not_call_initModule_again_when_parent_recomposes() = runComposeUiTest {
        val initCalls = mutableListOf<String>()
        lateinit var builtModules: List<Module>
        lateinit var triggerRecompose: () -> Unit

        setContent {
            val recomposeTick = remember { mutableIntStateOf(value = 0) }
            triggerRecompose = { recomposeTick.intValue++ }
            recomposeTick.intValue

            builtModules = rememberModules {
                module(TrackingModule(name = "A", initCalls = initCalls))
                module(TrackingModule(name = "B", initCalls = initCalls))
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
    override val destinations: PersistentMap<NavKey, DestinationMetadata> =
        persistentMapOf(UiTestDestination to DestinationMetadata())
    override val registerSerializers: PolymorphicModuleBuilder<NavKey>.() -> Unit = {}

    override fun EntryProviderScope<NavKey>.registerContent(
        onNavigateBack: () -> Unit,
        onNavigate: (NavKey) -> Unit,
        bottomPadding: androidx.compose.ui.unit.Dp
    ) = Unit

    @Composable
    override fun initModule() {
        initCalls.add(moduleName)
    }
}
