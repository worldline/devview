package com.worldline.devview

import androidx.compose.ui.unit.Dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.worldline.devview.core.DestinationMetadata
import com.worldline.devview.core.Module
import com.worldline.devview.core.Section
import kotlin.reflect.KClass
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.serialization.modules.PolymorphicModuleBuilder

data class TestModule(
    private val name: String,
    override val subtitle: String? = null,
    override val section: Section = Section.CUSTOM,
    override val entryDestination: NavKey = object : NavKey {},
    private val destinationsToUse: List<KClass<out NavKey>> = emptyList(),
    private val entries: EntryProviderScope<NavKey>.() -> Unit = {},
) : Module {
    override val moduleName: String = name
    override val destinations: PersistentMap<KClass<out NavKey>, DestinationMetadata> =
        destinationsToUse.associateWith {
            DestinationMetadata()
        }.toPersistentMap()
    override val registerSerializers: PolymorphicModuleBuilder<NavKey>.() -> Unit = {}

    override fun EntryProviderScope<NavKey>.registerContent(
        onNavigateBack: () -> Unit,
        onNavigate: (NavKey) -> Unit,
        bottomPadding: Dp
    ) {
        entries()
    }
}