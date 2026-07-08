package com.worldline.devview.core

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.reflect.KClass
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlin.test.Test

class ModuleRegistryTest {

    @Test
    fun `module adds a single module and preserves insertion order`() {
        val registry = ModuleRegistry()

        val built = registry
            .module(ModuleA)
            .module(ModuleB)
            .build()

        built.shouldContainExactly(ModuleA, ModuleB)
    }

    @Test
    fun `modules adds all provided modules in order`() {
        val registry = ModuleRegistry()

        val built = registry
            .modules(ModuleA, ModuleB, ModuleC)
            .build()

        built.shouldContainExactly(ModuleA, ModuleB, ModuleC)
    }

    @Test
    fun `module and modules can be chained together`() {
        val registry = ModuleRegistry()

        val built = registry
            .module(ModuleA)
            .modules(ModuleB, ModuleC)
            .module(ModuleD)
            .build()

        built.shouldContainExactly(ModuleA, ModuleB, ModuleC, ModuleD)
    }

    @Test
    fun `buildModules DSL produces the same ordered immutable list`() {
        val built = buildModules {
            module(ModuleA)
            modules(ModuleB, ModuleC)
            module(ModuleD)
        }

        built.shouldContainExactly(ModuleA, ModuleB, ModuleC, ModuleD)
    }

    @Test
    fun `builder methods return the same registry instance for fluent chaining`() {
        val registry = ModuleRegistry()

        val fromModule = registry.module(ModuleA)
        val fromModules = registry.modules(ModuleB)

        fromModule shouldBe registry
        fromModules shouldBe registry
    }
}

private data object DummyDestination : NavKey

private abstract class TestModule(
    name: String
) : Module {
    override val moduleName: String = name
    override val section: Section = Section.CUSTOM
    override val destinations: PersistentMap<KClass<out NavKey>, DestinationMetadata> =
        persistentMapOf(DummyDestination::class to DestinationMetadata())
    override val registerSerializers: PolymorphicModuleBuilder<NavKey>.() -> Unit = {}

    override fun EntryProviderScope<NavKey>.registerContent(
        onNavigateBack: () -> Unit,
        onNavigate: (NavKey) -> Unit,
        bottomPadding: androidx.compose.ui.unit.Dp
    ) = Unit
}

private data object ModuleA : TestModule(name = "A") {
    override val entryDestination: NavKey
        get() = DummyDestination
}

private data object ModuleB : TestModule(name = "B") {
    override val entryDestination: NavKey
        get() = DummyDestination
}

private data object ModuleC : TestModule(name = "C") {
    override val entryDestination: NavKey
        get() = DummyDestination
}

private data object ModuleD : TestModule(name = "D") {
    override val entryDestination: NavKey
        get() = DummyDestination
}
