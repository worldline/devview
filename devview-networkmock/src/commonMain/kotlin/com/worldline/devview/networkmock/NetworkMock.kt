package com.worldline.devview.networkmock

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.worldline.devview.core.Module
import com.worldline.devview.core.Section
import com.worldline.devview.networkmock.repository.MockConfigRepository
import com.worldline.devview.networkmock.repository.MockStateRepository
import com.worldline.devview.utils.DataStoreDelegate
import com.worldline.devview.utils.RequiresDataStore
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder

/**
 * Navigation destinations for the NetworkMock module.
 * All screens within NetworkMock are defined here.
 */
public sealed interface NetworkMockDestination : NavKey {
    /**
     * Main network mock list screen
     */
    @Serializable
    public data object Main : NetworkMockDestination
}

/**
 * NetworkMock module — manages network request/response mocking for development and testing.
 *
 * Implements [RequiresDataStore] so that [com.worldline.devview.core.rememberModules]
 * automatically initialises the shared [NetworkMockDataStoreDelegate] before
 * [initModule] is called.
 *
 * The [dataStoreDelegate] points to [NetworkMockDataStoreDelegate] — the
 * process-level singleton declared in `devview-networkmock-core`. This ensures
 * that both this UI module and `devview-networkmock-ktor` share exactly one
 * DataStore instance without depending on each other.
 *
 * [initModule] passes the initialised DataStore explicitly to
 * [NetworkMockInitializer.initialize], which constructs [MockStateRepository]
 * and [MockConfigRepository] once for the lifetime of the process.
 *
 * ## Usage
 * ```kotlin
 * val modules = rememberModules {
 *     module(NetworkMock(
 *         resourceLoader = { path -> Res.readBytes(path) }
 *     ))
 * }
 * ```
 *
 * The Ktor plugin requires no configuration when `NetworkMock` is registered:
 * ```kotlin
 * val client = HttpClient(OkHttp) {
 *     install(NetworkMockPlugin)
 * }
 * ```
 *
 * @property resourceLoader Function to load resource bytes from a path, provided
 * by the integrator's resource system (e.g. `Res.readBytes` from Compose Resources)
 * @property configPath Path to the `mocks.json` configuration file relative to
 * composeResources. Defaults to `"files/networkmocks/mocks.json"`.
 */
public class NetworkMock(
    private val resourceLoader: suspend (String) -> ByteArray,
    private val configPath: String = "files/networkmocks/mocks.json"
) : Module, RequiresDataStore {

    override val dataStoreName: String = NETWORK_MOCK_DATASTORE_NAME

    /**
     * Points to [NetworkMockDataStoreDelegate] — the process-level singleton in
     * `devview-networkmock-core`. Both this module and `devview-networkmock-ktor`
     * reference the same delegate, guaranteeing a single DataStore instance.
     */
    override val dataStoreDelegate: DataStoreDelegate = NetworkMockDataStoreDelegate

    /**
     * Initialises the network mock repositories once the DataStore is ready.
     *
     * Called automatically by [com.worldline.devview.core.rememberModules] after
     * [com.worldline.devview.utils.RequiresDataStore.initDataStore] has run.
     * The DataStore is retrieved from [dataStoreDelegate] and passed explicitly
     * to [NetworkMockInitializer.initialize] so the initializer remains
     * independent and testable.
     */
    @Composable
    override fun initModule() {
        NetworkMockInitializer.initialize(
            dataStore = dataStoreDelegate.get(),
            configPath = configPath,
            resourceLoader = resourceLoader
        )
    }

    override val section: Section
        get() = Section.NETWORK

    override val destinations: ImmutableList<NavKey> = persistentListOf(
        NetworkMockDestination.Main
    )

    override val registerSerializers: PolymorphicModuleBuilder<NavKey>.() -> Unit
        get() = {
            subclass(
                subclass = NetworkMockDestination.Main::class,
                serializer = NetworkMockDestination.Main.serializer()
            )
        }

    override fun EntryProviderScope<NavKey>.registerContent(
        onNavigateBack: () -> Unit,
        onNavigate: (NavKey) -> Unit
    ) {
        entry<NetworkMockDestination.Main> {
            Scaffold { paddingValues ->
                NetworkMockScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues = paddingValues),
                    configRepository = NetworkMockInitializer.requireConfigRepository(),
                    stateRepository = NetworkMockInitializer.requireStateRepository()
                )
            }
        }
    }
}
