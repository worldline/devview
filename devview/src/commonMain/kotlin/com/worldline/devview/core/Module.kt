package com.worldline.devview.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlin.reflect.KClass
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.serialization.modules.PolymorphicModuleBuilder

/**
 * Base interface for all DevView modules.
 *
 * A module represents a self-contained feature or tool within the DevView developer
 * tools suite. Each module provides:
 * - Metadata (name, icon, section)
 * - Navigation destinations (screens)
 * - UI content registration
 * - Serialization configuration for type-safe navigation
 *
 * ## Architecture
 *
 * Modules are **metadata containers** that don't participate in navigation directly.
 * Instead, they:
 * 1. Define navigable [destinations] (NavKey objects)
 * 2. Register serializers for those destinations via [registerSerializers]
 * 3. Provide composable content via [registerContent]
 *
 * The DevView framework uses this information to:
 * - Display modules in the home screen grouped by [section]
 * - Enable type-safe navigation to module screens
 * - Render module content when navigated to
 *
 * ## Creating a Module
 *
 * ### 1. Define Navigation Destinations
 * ```kotlin
 * sealed interface MyModuleDestination : NavKey {
 *     @Serializable
 *     data object Main : MyModuleDestination
 *
 *     @Serializable
 *     data class Detail(val id: String) : MyModuleDestination
 * }
 * ```
 *
 * ### 2. Implement the Module Interface
 * ```kotlin
 * object MyModule : Module {
 *     override val section: Section = Section.FEATURES
 *
 *     override val destinations = persistentMapOf(
 *         MyModuleDestination.Main.withTitle("Overview"),
 *         MyModuleDestination.Detail.asDestination()
 *     )
 *
 *     override val registerSerializers: PolymorphicModuleBuilder<NavKey>.() -> Unit = {
 *         subclass(MyModuleDestination.Main::class, MyModuleDestination.Main.serializer())
 *         // Register other destinations...
 *     }
 *
 *     override fun EntryProviderScope<NavKey>.registerContent(
 *         onNavigateBack: () -> Unit,
 *         onNavigate: (NavKey) -> Unit,
 *         bottomPadding: Dp,
 *     ) {
 *         entry<MyModuleDestination.Main> {
 *             MainScreen(
 *                 onNavigateBack = onNavigateBack,
 *                 onItemClick = { id -> onNavigate(MyModuleDestination.Detail(id)) }
 *             )
 *         }
 *
 *         entry<MyModuleDestination.Detail> {
 *             DetailScreen(onNavigateBack = onNavigateBack)
 *         }
 *     }
 * }
 * ```
 *
 * ### 3. Register with DevView
 * ```kotlin
 * @Composable
 * fun App() {
 *     val modules = rememberModules {
 *         module(MyModule)
 *         module(FeatureFlip)
 *         module(Analytics)
 *     }
 *
 *     DevView(
 *         devViewIsOpen = isDevViewOpen,
 *         closeDevView = { isDevViewOpen = false },
 *         modules = modules
 *     )
 * }
 * ```
 *
 * ## Customization
 *
 * Modules can customize their appearance by overriding:
 * - [moduleName]: Display name in the module list
 * - [icon]: Icon displayed in the module card
 * - [containerColor]: Background color of the icon container
 * - [contentColor]: Color of the icon itself
 * - [subtitle]: Optional descriptive text below the module name
 *
 * ## See Also
 * - [Section]: For grouping related modules
 * - [com.worldline.devview.DevView]: Main entry point
 * - [rememberModules]: Helper for building module lists
 * - Navigation3 library for navigation concepts
 *
 * @see Section
 * @see com.worldline.devview.DevView
 * @see rememberModules
 */
@Suppress("ComplexInterface")
public interface Module {
    /**
     * The name displayed in the module list on the DevView home screen.
     *
     * By default, this uses the class's simple name (e.g., "FeatureFlip", "Analytics").
     * Override this to provide a custom display name.
     *
     * ## Examples
     * - Default: "FeatureFlip" (from class name)
     * - Custom: "Feature Flags" or "Feature Toggle Manager"
     *
     * ```kotlin
     * override val moduleName: String = "Feature Flags"
     * ```
     *
     * @return The display name of the module.
     */
    public val moduleName: String
        get() = this::class.simpleName ?: "UnknownModule"

    /**
     * The section this module belongs to for organizational grouping.
     *
     * Modules are grouped by section on the DevView home screen, making it
     * easier to find related functionality. Choose the section that best
     * describes your module's purpose.
     *
     * ## Available Sections
     * - [Section.SETTINGS]: Configuration and app information
     * - [Section.FEATURES]: Feature flags and development tools
     * - [Section.LOGGING]: Debugging, analytics, and logging
     * - [Section.CUSTOM]: Application-specific modules
     *
     * ```kotlin
     * override val section: Section = Section.FEATURES
     * ```
     *
     * @see Section
     */
    public val section: Section

    /**
     * Icon displayed for this module in the module list.
     *
     * By default, uses the icon associated with the module's [section].
     * Override this to provide a custom icon specific to your module.
     *
     * ## Default Behavior
     * Returns [Section.icon] based on the module's section.
     *
     * ## Custom Icon Example
     * ```kotlin
     * override val icon: ImageVector = Icons.Rounded.BugReport
     * ```
     *
     * @see Section.icon
     */
    public val icon: ImageVector
        get() = section.icon

    /**
     * Background color of the icon container in the module list.
     *
     * This creates the circular colored background behind the icon.
     * Override to match your module's branding or to differentiate it visually.
     *
     * ## Default
     * A blue color (0xFF326EE6)
     *
     * ## Custom Color Example
     * ```kotlin
     * override val containerColor: Color = Color(0xFFFF5722) // Orange
     * ```
     */
    public val containerColor: Color
        get() = Color(color = 0xFF326EE6)

    /**
     * Color of the icon itself within the container.
     *
     * This should contrast well with [containerColor] for visibility.
     * Override to customize the icon color.
     *
     * ## Default
     * A light gray color (0xFFE6E6E6)
     *
     * ## Custom Color Example
     * ```kotlin
     * override val contentColor: Color = Color.White
     * ```
     */
    public val contentColor: Color
        get() = Color(color = 0xFFE6E6E6)

    /**
     * Optional subtitle displayed below the module name in the module list.
     *
     * Use this to provide additional context about what the module does.
     * Returns null by default (no subtitle).
     *
     * ## Example
     * ```kotlin
     * override val subtitle: String = "Manage feature flags"
     * ```
     *
     * @return The subtitle text, or null if no subtitle should be displayed.
     */
    public val subtitle: String?
        get() = null

    /**
     * Maps each navigable destination in this module to its [DestinationMetadata].
     *
     * The **keys** of this map are the [NavKey] objects representing every screen
     * that belongs to this module. DevView uses the key set to:
     * - Determine which module is currently active (by matching the backstack top).
     * - Navigate to the first key when the user opens this module from the home screen.
     *
     * The **values** are [DestinationMetadata] instances that describe, per destination:
     * - An optional [title][DestinationMetadata.title] for the top app bar
     *   (`null` falls back to [moduleName]).
     * - An ordered list of [actions][DestinationMetadata.actions] rendered as icon
     *   buttons in the top app bar while that destination is active.
     *
     * ## Declaration
     *
     * Prefer the [NavKey] extension functions for a concise declaration:
     *
     * ```kotlin
     * // One screen, static title, no actions
     * override val destinations = persistentMapOf(
     *     MyDestination.Main.withTitle("My Screen")
     * )
     *
     * // One screen, static title, one action with confirmation popup
     * override val destinations = persistentMapOf(
     *     MyDestination.Main.withTitle("Logs") {
     *         action(
     *             icon = Icons.Rounded.Delete,
     *             popup = ModuleDestinationActionPopup(
     *                 title = "Clear Logs",
     *                 confirmButton = "Clear",
     *                 dismissButton = "Cancel"
     *             )
     *         ) {
     *             logger.clear()
     *         }
     *     }
     * )
     *
     * // Multiple screens with mixed metadata
     * override val destinations = persistentMapOf(
     *     MyDestination.Main.withTitle("Overview"),
     *     MyDestination.Detail.asDestination()
     * )
     * ```
     *
     * You can also use the explicit form when you need full control:
     *
     * ```kotlin
     * override val destinations = persistentMapOf(
     *     MyDestination.Main to DestinationMetadata(
     *         title = "My Screen",
     *         actions = persistentListOf(
     *             ModuleDestinationAction(
     *                 icon = Icons.Rounded.Delete,
     *                 action = { logger.clear() }
     *             )
     *         )
     *     )
     * )
     * ```
     *
     * ## Best Practices
     * - Always include at least one destination (typically a `Main` screen).
     * - Use [asDestination] for screens that need no title override and no actions.
     * - For `data class` destinations use the [KClass] extension overloads
     *   (e.g. `MyDestination.Detail::class.withTitle("Detail")`), since no
     *   representative instance is available at module-construction time.
     *
     * @see entryDestination
     * @see DestinationMetadata
     * @see NavKey
     */
    public val destinations: PersistentMap<KClass<out NavKey>, DestinationMetadata>

    /**
     * The [NavKey] instance pushed onto the backstack when the user opens this module
     * from the DevView home screen.
     *
     * This is the concrete destination instance that is navigated to on module entry.
     * It must correspond to one of the keys registered in [destinations] (i.e.
     * `entryDestination::class` must be a key in the map).
     *
     * For modules whose root screen is a `data object`, this is simply that object:
     * ```kotlin
     * override val entryDestination: NavKey = MyDestination.Main
     * ```
     *
     * For modules whose root screen is a `data class` (rare — the entry screen typically
     * does not require parameters), construct the appropriate instance here:
     * ```kotlin
     * override val entryDestination: NavKey = MyDestination.Root(defaultParam)
     * ```
     *
     * @see destinations
     */
    public val entryDestination: NavKey

    /**
     * Registers all destination serializers for type-safe navigation.
     *
     * This property provides a lambda that registers kotlinx.serialization
     * serializers for all destination types in this module. This is required
     * for the Navigation3 library to properly serialize and deserialize
     * navigation state.
     *
     * ## Implementation Pattern
     * ```kotlin
     * override val registerSerializers: PolymorphicModuleBuilder<NavKey>.() -> Unit = {
     *     subclass(MyDestination.Main::class, MyDestination.Main.serializer())
     *     subclass(MyDestination.Detail::class, MyDestination.Detail.serializer())
     *     // Register all destination types here
     * }
     * ```
     *
     * ## Why This is Needed
     * Navigation3 uses kotlinx.serialization to save and restore navigation state
     * across process death. Each destination type must be registered as a subclass
     * of the NavKey polymorphic base class.
     *
     * @see destinations
     * @see registerContent
     */
    public val registerSerializers: PolymorphicModuleBuilder<NavKey>.() -> Unit

    /**
     * Registers this module's composable content with the navigation entry provider.
     *
     * This function defines what UI is displayed when navigating to each destination
     * in this module. It's called once during DevView initialization to set up the
     * navigation graph.
     *
     * ## Implementation Pattern
     * ```kotlin
     * override fun EntryProviderScope<NavKey>.registerContent(
     *     onNavigateBack: () -> Unit,
     *     onNavigate: (NavKey) -> Unit,
     *     bottomPadding: Dp,
     * ) {
     *     // Register each destination
     *     entry<MyDestination.Main> {
     *         MainScreen(
     *             onNavigateBack = onNavigateBack,
     *             onItemClick = { id ->
     *                 onNavigate(MyDestination.Detail(id))
     *             }
     *         )
     *     }
     *
     *     entry<MyDestination.Detail> { destination ->
     *         DetailScreen(
     *             id = destination.id,
     *             onNavigateBack = onNavigateBack
     *         )
     *     }
     * }
     * ```
     *
     * ## Navigation Callbacks
     * - **onNavigateBack**: Call this to navigate back (pop the backstack)
     * - **onNavigate**: Call this with a NavKey to navigate forward to a new destination
     *
     * ## Bottom Padding
     * [bottomPadding] carries the inset from the DevView [Scaffold][androidx.compose.material3.Scaffold]'s
     * bottom padding (e.g. navigation bar insets). Pass it down to any scrollable content or
     * lazy lists inside your screen so that the last item is not obscured by system UI.
     *
     * ```kotlin
     * entry<MyDestination.Main> {
     *     MyScreen(
     *         modifier = Modifier.fillMaxSize(),
     *         bottomPadding = bottomPadding
     *     )
     * }
     * ```
     *
     * ## Best Practices
     * - Register an entry for every destination in [destinations]
     * - Always provide a way to navigate back (use onNavigateBack)
     * - Use type-safe destination objects instead of string routes
     * - Wrap content in Scaffold if needed for consistent padding
     *
     * @param onNavigateBack Callback to navigate back to the previous screen.
     *   Typically pops the current screen from the backstack.
     * @param onNavigate Callback to navigate forward to a new destination.
     *   Pass a [NavKey] instance to specify the target screen.
     * @param bottomPadding The bottom inset padding provided by the DevView Scaffold.
     *   Apply this to your screen's scrollable content to avoid content being hidden
     *   behind system navigation bars.
     *
     * @see destinations
     * @see registerSerializers
     */
    public fun EntryProviderScope<NavKey>.registerContent(
        onNavigateBack: () -> Unit,
        onNavigate: (NavKey) -> Unit,
        bottomPadding: Dp
    )

    /**
     * Called once when the module is first registered in a Composable context
     * via [rememberModules].
     *
     * Override this function to perform any Composable-context initialisation
     * this module requires — for example, setting up repositories or initialising
     * module-specific singletons that need a Composable context.
     *
     * For modules that implement [com.worldline.devview.utils.RequiresDataStore],
     * DataStore initialisation is handled automatically by [rememberModules] before
     * this function is called, so
     * [dataStoreDelegate][com.worldline.devview.utils.RequiresDataStore.dataStoreDelegate]
     * is guaranteed to be ready when [initModule] runs.
     *
     * ## Example
     * ```kotlin
     * class MyModule(private val resourceLoader: suspend (String) -> ByteArray) : Module {
     *     @Composable
     *     override fun initModule() {
     *         MyModuleInitializer.initialize(resourceLoader = resourceLoader)
     *     }
     * }
     * ```
     *
     * Default implementation is a no-op.
     *
     * @see rememberModules
     * @see com.worldline.devview.utils.RequiresDataStore
     */
    @Suppress("ComposableNaming")
    @Composable
    public fun initModule() {
    }
}

/**
 * Helper function for creating preview modules for UI previews and testing.
 *
 * This internal function creates a minimal [Module] implementation suitable for
 * Compose previews and testing scenarios. The created module has no actual
 * destinations or content registration.
 *
 * ## Usage in Previews
 * ```kotlin
 * @Preview
 * @Composable
 * fun ModuleListPreview() {
 *     HomeScreen(
 *         modules = listOf(
 *             previewModule(name = "FeatureFlip", section = Section.FEATURES),
 *             previewModule(name = "Analytics", section = Section.LOGGING),
 *             previewModule(name = "Settings", section = Section.SETTINGS)
 *         ),
 *         openModule = {}
 *     )
 * }
 * ```
 *
 * ## Note
 * This is an internal function and should not be used in production code.
 * For production, implement the [Module] interface properly.
 *
 * @param name The display name for the preview module.
 * @param section The section to assign the preview module to.
 * @return A minimal [Module] implementation for preview/testing purposes.
 *
 * @see Module
 * @see Section
 */
internal fun previewModule(
    name: String = "PreviewModule",
    section: Section = Section.CUSTOM
): Module = object : Module {
    override val section: Section = section
    override val moduleName: String = name
    override val destinations: PersistentMap<KClass<out NavKey>, DestinationMetadata> = persistentMapOf()
    override val entryDestination: NavKey get() = error(
        message = "previewModule has no destinations"
    )
    override val registerSerializers: PolymorphicModuleBuilder<NavKey>.() -> Unit = {}

    override fun EntryProviderScope<NavKey>.registerContent(
        onNavigateBack: () -> Unit,
        onNavigate: (NavKey) -> Unit,
        bottomPadding: Dp
    ) {
    }
}
