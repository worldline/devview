package com.worldline.devview.core

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.collections.immutable.ImmutableList
import kotlinx.serialization.modules.PolymorphicModuleBuilder

/**
 * Base interface for all DevView modules.
 * Modules are metadata containers and don't participate in navigation directly.
 * Navigation is handled through the module's destinations.
 */
public interface Module {
    /**
     * The name displayed in the module list.
     * Defaults to the class simple name.
     */
    public val moduleName: String
        get() = this::class.simpleName ?: "UnknownModule"

    /**
     * The section this module belongs to (for grouping).
     */
    public val section: Section

    /**
     * Icon displayed for this module.
     * Defaults to the section icon.
     */
    public val icon: ImageVector
        get() = section.icon

    /**
     * Background color of the icon container.
     */
    public val containerColor: Color
        get() = Color(color = 0xFF326EE6)

    /**
     * Color of the icon itself.
     */
    public val contentColor: Color
        get() = Color(color = 0xFFE6E6E6)

    /**
     * Optional subtitle displayed below the module name.
     */
    public val subtitle: String?
        get() = null

    /**
     * List of all navigable destinations within this module.
     * These are the NavKey objects that represent screens in this module.
     */
    public val destinations: ImmutableList<NavKey>

    /**
     * Register all destination serializers for navigation.
     * Required for kotlinx.serialization polymorphism.
     *
     * Example:
     * ```
     * override val registerSerializers: PolymorphicModuleBuilder<NavKey>.() -> Unit = {
     *     subclass(MyDestination.Main::class, MyDestination.Main.serializer())
     *     subclass(MyDestination.Detail::class, MyDestination.Detail.serializer())
     * }
     * ```
     */
    public val registerSerializers: PolymorphicModuleBuilder<NavKey>.() -> Unit

    /**
     * Register this module's composable content with the navigation entry provider.
     *
     * @param onNavigateBack Callback to navigate back (close current screen)
     * @param onNavigate Callback to navigate forward to a destination
     *
     * Example:
     * ```
     * override fun EntryProviderScope<NavKey>.registerContent(
     *     onNavigateBack: () -> Unit,
     *     onNavigate: (NavKey) -> Unit
     * ) {
     *     entry<MyDestination.Main> {
     *         MainScreen(
     *             onNavigateBack = onNavigateBack,
     *             onItemClick = { id -> onNavigate(MyDestination.Detail(id)) }
     *         )
     *     }
     *     entry<MyDestination.Detail> {
     *         DetailScreen(
     *             onNavigateBack = onNavigateBack
     *         )
     *     }
     * }
     * ```
     */
    public fun EntryProviderScope<NavKey>.registerContent(
        onNavigateBack: () -> Unit,
        onNavigate: (NavKey) -> Unit
    )
}

/**
 * Helper function for creating preview modules.
 * Internal use only for previews and testing.
 */
internal fun previewModule(
    name: String = "PreviewModule",
    section: Section = Section.CUSTOM
): Module = object : Module {
    override val section: Section = section
    override val moduleName: String = name
    override val destinations: ImmutableList<NavKey> = kotlinx.collections.immutable
        .persistentListOf()
    override val registerSerializers: PolymorphicModuleBuilder<NavKey>.() -> Unit = {}

    override fun EntryProviderScope<NavKey>.registerContent(
        onNavigateBack: () -> Unit,
        onNavigate: (NavKey) -> Unit
    ) {}
}
