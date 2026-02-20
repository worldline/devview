package com.worldline.devview.utils

import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

/**
 * A reusable holder that owns a single [DataStore] instance for a DevView module.
 *
 * Each module that requires persistent storage should declare one instance of this
 * class. The DataStore is initialised lazily on the first call to [init], which is
 * triggered automatically by [com.worldline.devview.core.rememberModules] for any
 * module implementing [RequiresDataStore]. The `null` guard inside [init] ensures
 * the DataStore is created exactly once per process regardless of recompositions.
 *
 * ## Usage
 *
 * Declare a single instance on the module object or as a top-level val if the
 * DataStore must be shared across multiple modules (e.g. a Ktor plugin and a UI
 * panel that need the same DataStore):
 *
 * ### Module-owned delegate
 * ```kotlin
 * object MyModule : Module, RequiresDataStore {
 *     override val dataStoreName = "my_module.preferences_pb"
 *     override val dataStoreDelegate = DataStoreDelegate()
 * }
 * ```
 *
 * ### Shared top-level delegate
 * ```kotlin
 * // In the shared core module
 * public val myModuleDataStoreDelegate = DataStoreDelegate()
 *
 * // In the UI module
 * object MyModule : Module, RequiresDataStore {
 *     override val dataStoreName = "my_module.preferences_pb"
 *     override val dataStoreDelegate = myModuleDataStoreDelegate
 * }
 *
 * // In the Ktor plugin module
 * val dataStore = myModuleDataStoreDelegate.get()
 * ```
 *
 * @see RequiresDataStore
 * @see com.worldline.devview.core.rememberModules
 */
public class DataStoreDelegate {
    private var instance: DataStore<Preferences>? = null

    /**
     * Initialises the [DataStore] instance for the given [dataStoreName].
     *
     * This function is `@Composable` so that [rememberDataStore] can access
     * platform-specific context (e.g. `LocalContext` on Android) without requiring
     * the integrator to pass it explicitly.
     *
     * The `null` guard ensures the DataStore is created exactly once — subsequent
     * calls on recomposition are no-ops.
     *
     * This function is called automatically by [com.worldline.devview.core.rememberModules]
     * for any module implementing [RequiresDataStore]. It should not be called manually.
     *
     * @param dataStoreName The filename used for the DataStore preferences file.
     */
    @Suppress("ComposableNaming")
    @Composable
    public fun init(dataStoreName: String) {
        if (instance != null) return
        instance = rememberDataStore(dataStoreName = dataStoreName)
    }

    /**
     * Returns the initialised [DataStore] instance.
     *
     * @throws IllegalStateException if [init] has not been called yet, which
     * indicates the module was not registered via [com.worldline.devview.core.rememberModules].
     */
    public fun get(): DataStore<Preferences> = instance
        ?: error(
            "DataStore not initialised. " +
                "Ensure the module is registered via rememberModules { }."
        )
}


