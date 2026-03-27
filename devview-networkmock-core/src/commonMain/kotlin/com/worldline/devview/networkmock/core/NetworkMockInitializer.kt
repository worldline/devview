package com.worldline.devview.networkmock.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.worldline.devview.networkmock.core.repository.MockConfigRepository
import com.worldline.devview.networkmock.core.repository.MockStateRepository

/**
 * Singleton initializer for the network mock module's repositories.
 *
 * This object is responsible for constructing and holding the single instances
 * of [MockStateRepository] and [MockConfigRepository] for the lifetime of the
 * process. It is initialised once from `devview-networkmock`'s `NetworkMock.initModule()`
 * and read by both `devview-networkmock` (UI) and `devview-networkmock-ktor` (Ktor plugin).
 *
 * ## Initialisation Order
 * The following order is guaranteed by `rememberModules`:
 * 1. [com.worldline.devview.utils.RequiresDataStore.initDataStore] — initialises
 *    [NetworkMockDataStoreDelegate] so [DataStore] is ready
 * 2. `NetworkMock.initModule()` — calls [initialize], passing the DataStore from
 *    the delegate explicitly
 *
 * ## Why an object
 * `devview-networkmock-ktor` has no Compose context and no access to the
 * `NetworkMock` class (which lives in `devview-networkmock`). A process-level
 * singleton in `core` is the only location both modules can read repositories
 * from without creating a circular dependency.
 *
 * @see NetworkMockDataStoreDelegate
 * @see MockStateRepository
 * @see MockConfigRepository
 */
public object NetworkMockInitializer {
    private var stateRepository: MockStateRepository? = null
    private var configRepository: MockConfigRepository? = null

    /**
     * Initialises the repositories for the network mock module.
     *
     * Called once from `NetworkMock.initModule()` in `devview-networkmock`.
     * Subsequent calls on recomposition are no-ops — repositories are only
     * ever constructed once.
     *
     * The [dataStore] instance is passed explicitly rather than read from
     * [NetworkMockDataStoreDelegate] directly, keeping this initializer
     * independent and testable in isolation.
     *
     * @param dataStore The initialised [DataStore] instance from [NetworkMockDataStoreDelegate]
     * @param configPath Path to the `mocks.json` configuration file relative to
     * composeResources (e.g. `"files/networkmocks/mocks.json"`)
     * @param resourceLoader Function to load resource bytes from a path, provided
     * by the integrator's resource system (e.g. `Res.readBytes` from Compose Resources)
     */
    @Suppress("ComposableNaming")
    @Composable
    public fun initialize(
        dataStore: DataStore<Preferences>,
        configPath: String,
        resourceLoader: suspend (String) -> ByteArray
    ) {
        if (stateRepository != null) return
        stateRepository = remember {
            MockStateRepository(
                dataStore = dataStore
            )
        }
        configRepository = remember {
            MockConfigRepository(
                configPath = configPath,
                resourceLoader = resourceLoader
            )
        }
    }

    /**
     * Returns the [com.worldline.devview.networkmock.core.repository.MockStateRepository] instance.
     *
     * Used by both the UI layer (`devview-networkmock`) and the Ktor plugin
     * (`devview-networkmock-ktor`) to read and update mock state.
     *
     * @throws IllegalStateException if called before [initialize], which indicates
     * that `NetworkMock` was not registered via `rememberModules { }`.
     */
    public fun requireStateRepository(): MockStateRepository = stateRepository
        ?: error(
            message = "NetworkMockInitializer not initialised. " +
                "Ensure NetworkMock is registered via rememberModules { }."
        )

    /**
     * Returns the [com.worldline.devview.networkmock.core.repository.MockConfigRepository] instance.
     *
     * Used by both the UI layer (`devview-networkmock`) and the Ktor plugin
     * (`devview-networkmock-ktor`) to load mock configuration and response files.
     *
     * @throws IllegalStateException if called before [initialize], which indicates
     * that `NetworkMock` was not registered via `rememberModules { }`.
     */
    public fun requireConfigRepository(): MockConfigRepository = configRepository
        ?: error(
            message = "NetworkMockInitializer not initialised. " +
                "Ensure NetworkMock is registered via rememberModules { }."
        )
}
