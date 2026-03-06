package com.worldline.devview.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.worldline.devview.utils.RequiresDataStore
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Builder for configuring DevView modules using a clean DSL.
 *
 * This class provides a fluent API for composing a list of modules to be
 * displayed in DevView. It supports adding modules individually or in bulk,
 * and builds an immutable list suitable for consumption by the DevView framework.
 *
 * ## Usage
 *
 * Typically used via the [buildModules] or [rememberModules] DSL functions:
 * ```kotlin
 * val modules = buildModules {
 *     module(AppInfo)
 *     module(FeatureFlip)
 *     module(Analytics)
 * }
 * ```
 *
 * Or in a Composable:
 * ```kotlin
 * @Composable
 * fun App() {
 *     val modules = rememberModules {
 *         module(AppInfo)
 *         module(FeatureFlip)
 *         modules(Analytics, Console, NetworkMonitor)
 *     }
 *
 *     DevView(
 *         devViewIsOpen = isOpen,
 *         closeDevView = { isOpen = false },
 *         modules = modules
 *     )
 * }
 * ```
 *
 * ## Thread Safety
 * This builder is not thread-safe. It should be used within a single thread,
 * typically during initialization or within a Composable function.
 *
 * @see buildModules
 * @see rememberModules
 * @see Module
 */
public class ModuleRegistry {
    private val modules = mutableListOf<Module>()

    /**
     * Adds a single module to the DevView configuration.
     *
     * Modules are added in the order they are registered. DevView will group
     * and display them by their [Section] on the home screen.
     *
     * ## Example
     * ```kotlin
     * buildModules {
     *     module(FeatureFlip)
     *     module(Analytics)
     * }
     * ```
     *
     * @param module The module to add to the configuration.
     * @return This [ModuleRegistry] instance for method chaining.
     *
     * @see Module
     * @see modules
     */
    public fun module(module: Module): ModuleRegistry = apply {
        modules.add(element = module)
    }

    /**
     * Adds multiple modules at once to the DevView configuration.
     *
     * This is a convenience method for adding several modules in a single call.
     * Modules are added in the order provided.
     *
     * ## Example
     * ```kotlin
     * buildModules {
     *     modules(FeatureFlip, Analytics, Console)
     * }
     * ```
     *
     * @param modules The modules to add to the configuration.
     * @return This [ModuleRegistry] instance for method chaining.
     *
     * @see Module
     * @see module
     */
    public fun modules(vararg modules: Module): ModuleRegistry = apply {
        this.modules.addAll(elements = modules)
    }

    /**
     * Builds the final immutable list of modules.
     *
     * This method is called internally by [buildModules] and [rememberModules]
     * to create the final module list for DevView.
     *
     * ## Usage
     * ```kotlin
     * val registry = ModuleRegistry()
     *     .module(FeatureFlip)
     *     .module(Analytics)
     * val moduleList = registry.build()
     * ```
     *
     * @return An [ImmutableList] containing all registered modules.
     */
    public fun build(): ImmutableList<Module> = modules.toImmutableList()
}

/**
 * DSL function for creating a module configuration.
 *
 * This function provides a clean, declarative way to build a list of DevView
 * modules. The returned list is immutable and can be safely passed to the DevView
 * composable.
 *
 * ## Basic Usage
 * ```kotlin
 * val modules = buildModules {
 *     module(AppInfo)
 *     module(FeatureFlip)
 *     module(Analytics)
 * }
 * ```
 *
 * ## Adding Multiple Modules
 * ```kotlin
 * val modules = buildModules {
 *     module(AppInfo)
 *     modules(FeatureFlip, Analytics, Console)
 * }
 * ```
 *
 * ## Conditional Modules
 * ```kotlin
 * val modules = buildModules {
 *     module(AppInfo)
 *
 *     if (BuildConfig.DEBUG) {
 *         module(DebugTools)
 *     }
 *
 *     if (hasFeatureFlagsEnabled) {
 *         module(FeatureFlip)
 *     }
 * }
 * ```
 *
 * ## See Also
 * - [rememberModules]: Composable version that remembers the module list
 * - DevView composable: Main entry point that consumes the module list
 *
 * @param block Lambda with receiver for configuring the module registry.
 * @return An [ImmutableList] of configured modules.
 *
 * @see ModuleRegistry
 * @see rememberModules
 */
public fun buildModules(block: ModuleRegistry.() -> Unit): ImmutableList<Module> =
    ModuleRegistry().apply(block = block).build()

/**
 * Composable version of [buildModules] that remembers the module list.
 *
 * This function creates and remembers a module configuration across recompositions.
 * Use this when building the module list within a Composable function to avoid
 * recreating the list on every recomposition.
 *
 * After building the module list, this function calls [Module.initModule] on each
 * registered module. This gives every module a chance to perform Composable-context
 * initialisation (e.g. repository setup). For modules that implement
 * [RequiresDataStore][com.worldline.devview.utils.RequiresDataStore], DataStore initialisation is also
 * triggered automatically before [Module.initModule] is called.
 *
 * ## Basic Usage
 * ```kotlin
 * @Composable
 * fun App() {
 *     val modules = rememberModules {
 *         module(AppInfo)
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
 * ## With Dynamic Configuration
 * ```kotlin
 * @Composable
 * fun App() {
 *     val isDebugBuild = BuildConfig.DEBUG
 *
 *     val modules = rememberModules {
 *         module(AppInfo)
 *         module(FeatureFlip)
 *
 *         if (isDebugBuild) {
 *             modules(DebugConsole, NetworkMonitor)
 *         }
 *     }
 *
 *     // Use modules...
 * }
 * ```
 *
 * ## Recomputation
 * The module list is created once and remembered. If you need to recompute
 * it based on changing dependencies, use `remember(key1, key2, ...) { }` instead:
 * ```kotlin
 * val modules = remember(userRole, featureFlags) {
 *     buildModules {
 *         // Configuration based on dependencies
 *     }
 * }
 * ```
 *
 * @param block Lambda with receiver for configuring the module registry.
 * @return A remembered [ImmutableList] of configured modules.
 *
 * @see buildModules
 * @see ModuleRegistry
 */
@Composable
public fun rememberModules(block: ModuleRegistry.() -> Unit): ImmutableList<Module> {
    val modules = buildModules(block = block)

    modules.forEach { module ->
        if (module is RequiresDataStore) module.initDataStore()
        module.initModule()
    }

    return remember {
        modules
    }
}
