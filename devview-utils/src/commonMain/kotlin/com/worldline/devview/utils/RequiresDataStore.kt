package com.worldline.devview.utils

import androidx.compose.runtime.Composable

/**
 * Optional interface for `Module` implementations that require a [DataStoreDelegate].
 *
 * Implementing this interface signals to `rememberModules`
 * that this module needs a DataStore instance. The DataStore is initialised
 * automatically via [initDataStore] before `Module.initModule` is called, so
 * [dataStoreDelegate] is guaranteed to be ready when `initModule` runs.
 *
 * ## Minimal usage — DataStore only, no extra init
 *
 * For modules that only need a DataStore and no further Composable-context
 * initialisation, implementing this interface is sufficient — no `Module.initModule`
 * override is required:
 *
 * ```kotlin
 * object MyModule : Module, RequiresDataStore {
 *     override val dataStoreName = "my_module.preferences_pb"
 *     override val dataStoreDelegate = DataStoreDelegate()
 *     // DataStore is ready via dataStoreDelegate.get() once rememberModules has run
 * }
 * ```
 *
 * ## Usage with extra Composable-context initialisation
 *
 * For modules that need additional initialisation beyond DataStore (e.g. setting
 * up repositories), override `Module.initModule`. The DataStore is guaranteed
 * initialised before `Module.initModule` is called:
 *
 * ```kotlin
 * class MyModule(
 *     private val resourceLoader: suspend (String) -> ByteArray
 * ) : Module, RequiresDataStore {
 *     override val dataStoreName = "my_module.preferences_pb"
 *     override val dataStoreDelegate = DataStoreDelegate()
 *
 *     @Composable
 *     override fun initModule() {
 *         // dataStoreDelegate.get() is safe to call here
 *         MyModuleInitializer.initialize(
 *             dataStore = dataStoreDelegate.get(),
 *             resourceLoader = resourceLoader
 *         )
 *     }
 * }
 * ```
 *
 * ## Sharing a DataStore across multiple modules
 *
 * If a DataStore instance must be shared between two separate modules (e.g. a
 * Ktor plugin module and a UI module), declare the delegate as a top-level val
 * in the shared core module and point both modules at it:
 *
 * ```kotlin
 * // In the shared core module
 * public val myModuleDataStoreDelegate = DataStoreDelegate()
 *
 * // In the UI module — drives initialisation
 * class MyModule(...) : Module, RequiresDataStore {
 *     override val dataStoreName = "my_module.preferences_pb"
 *     override val dataStoreDelegate = myModuleDataStoreDelegate
 * }
 *
 * // In the Ktor plugin module — reads from the same delegate
 * val dataStore = myModuleDataStoreDelegate.get()
 * ```
 *
 * @see DataStoreDelegate
 */
public interface RequiresDataStore {

    /**
     * The filename used for this module's DataStore preferences file.
     *
     * Must be unique across all modules to avoid unintentional state sharing.
     * By convention, use the pattern `"<module_name>_datastore.preferences_pb"`.
     *
     * ## Example
     * ```kotlin
     * override val dataStoreName = "my_module_datastore.preferences_pb"
     * ```
     */
    public val dataStoreName: String

    /**
     * The [DataStoreDelegate] instance that holds this module's DataStore.
     *
     * For a module-owned DataStore, declare a new instance directly:
     * ```kotlin
     * override val dataStoreDelegate = DataStoreDelegate()
     * ```
     *
     * For a shared DataStore (used by multiple modules), point to a shared
     * top-level val declared in a common core module:
     * ```kotlin
     * override val dataStoreDelegate = myModuleDataStoreDelegate
     * ```
     */
    public val dataStoreDelegate: DataStoreDelegate

    /**
     * Initialises the DataStore instance for this module.
     *
     * Called automatically by `rememberModules` before `Module.initModule`
     * — do not call or override this function manually.
     *
     * The default implementation delegates to [DataStoreDelegate.init], which
     * ensures the DataStore is created exactly once per process.
     */
    @Suppress("ComposableNaming")
    @Composable
    public fun initDataStore() {
        dataStoreDelegate.init(dataStoreName = dataStoreName)
    }
}
