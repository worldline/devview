package com.worldline.devview.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Builder for configuring DevView modules.
 * Provides a clean DSL for users to compose their module list.
 */
public class ModuleRegistry {
    private val modules = mutableListOf<Module>()

    /**
     * Add a module to the DevView.
     */
    public fun module(module: Module): ModuleRegistry = apply {
        modules.add(element = module)
    }

    /**
     * Add multiple modules at once.
     */
    public fun modules(vararg modules: Module): ModuleRegistry = apply {
        this.modules.addAll(elements = modules)
    }

    /**
     * Build the final immutable list of modules.
     */
    public fun build(): ImmutableList<Module> = modules.toImmutableList()
}

/**
 * DSL function for creating a module configuration.
 *
 * Example:
 * ```
 * val modules = buildModules {
 *     module(AppInfo)
 *     module(FeatureFlip)
 *     module(MyCustomModule)
 * }
 * ```
 */
public fun buildModules(block: ModuleRegistry.() -> Unit): ImmutableList<Module> =
    ModuleRegistry().apply(block = block).build()

/**
 * Composable version that remembers the module list.
 *
 * Example:
 * ```
 * val modules = rememberModules {
 *     module(AppInfo)
 *     module(FeatureFlip)
 * }
 * ```
 */
@Composable
public fun rememberModules(block: ModuleRegistry.() -> Unit): ImmutableList<Module> = remember {
    buildModules(block = block)
}
