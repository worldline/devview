package com.worldline.devview.networkmock

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.worldline.devview.core.DestinationMetadata
import com.worldline.devview.core.Module
import com.worldline.devview.core.ModuleDestinationActionPopup
import com.worldline.devview.core.Section
import com.worldline.devview.core.withActions
import com.worldline.devview.networkmock.core.NETWORK_MOCK_DATASTORE_NAME
import com.worldline.devview.networkmock.core.NetworkMockDataStoreDelegate
import com.worldline.devview.networkmock.core.NetworkMockInitializer
import com.worldline.devview.networkmock.core.NetworkMockResourceLoader
import com.worldline.devview.networkmock.core.model.OperationKey
import com.worldline.devview.networkmock.viewmodel.NetworkMockSessionViewModel
import com.worldline.devview.networkmock.viewmodel.NetworkMockViewModel
import com.worldline.devview.networkmock.viewmodel.OperationDetailViewModel
import com.worldline.devview.utils.DataStoreDelegate
import com.worldline.devview.utils.RequiresDataStore
import kotlin.reflect.KClass
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import org.jetbrains.compose.resources.MissingResourceException

/**
 * Navigation destinations for the NetworkMock module.
 *
 * The operation list owns the quick-choice sheet. Inspecting a response or editing a sequence
 * closes that sheet and opens a full-screen destination. Back and successful saves return to the list.
 */
public sealed interface NetworkMockDestination : NavKey {
    /**
     * The operation list and its quick-choice sheet.
     */
    @Serializable
    public data object Main : NetworkMockDestination

    /**
     * Inspects one response variant, with comparison, copying and activation controls.
     *
     * @property operation The operation containing the response.
     * @property statusCode The response's HTTP status code.
     * @property mediaType The response's declared media type.
     * @property exampleName The response's example identifier.
     */
    @Serializable
    public data class ResponseDetail(
        val operation: OperationKey,
        val statusCode: Int,
        val mediaType: String,
        val exampleName: String
    ) : NetworkMockDestination

    /**
     * Edits an ordered response sequence, retaining its draft while the overlay is hidden.
     *
     * @property operation The operation whose sequence is being edited.
     */
    @Serializable
    public data class SequenceBuilder(val operation: OperationKey) : NetworkMockDestination
}

/**
 * NetworkMock module for network request/response mocking during development and testing.
 *
 * Implements [RequiresDataStore] so that [com.worldline.devview.core.rememberModules]
 * automatically initialises the shared [NetworkMockDataStoreDelegate] before
 * [initModule] is called.
 *
 * The [dataStoreDelegate] points to [NetworkMockDataStoreDelegate], the
 * process-level singleton declared in `devview-networkmock-core`. This ensures
 * that both this UI module and `devview-networkmock-ktor` share exactly one
 * DataStore instance without depending on each other.
 *
 * [initModule] passes the initialised DataStore explicitly to
 * [NetworkMockInitializer.initialize], which constructs
 * [MockStateRepository][com.worldline.devview.networkmock.core.repository.MockStateRepository] and
 * [MockConfigRepository][com.worldline.devview.networkmock.core.repository.MockConfigRepository]
 * once for the lifetime of the process.
 *
 * A separate module-owned session serializes accepted UI writes and retains pending recovery
 * through overlay hiding and host configuration changes. Deliberate registration removal ends
 * that session. Filtering this module out of DevView returns Home and discards its entry drafts,
 * but does not end the session while its registration remains composed.
 *
 * ## Usage
 * ```kotlin
 * val modules = rememberModules {
 *     module(NetworkMock(
 *         resourceLoader = NetworkMockResourceLoader { path -> Res.readBytes(path) },
 *         specPaths = listOf("files/networkmocks/specs/my-backend.json")
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
 * @property resourceLoader [NetworkMockResourceLoader] provided by the integrator's resource system
 * @property specPaths Paths to the OpenAPI spec files relative to composeResources, one per
 * API group (e.g. `listOf("files/networkmocks/specs/my-backend.json")`).
 */
public class NetworkMock(
    private val resourceLoader: NetworkMockResourceLoader,
    private val specPaths: List<String>
) : Module,
    RequiresDataStore {
    override val dataStoreName: String = NETWORK_MOCK_DATASTORE_NAME

    /**
     * Points to [NetworkMockDataStoreDelegate], the process-level singleton in
     * `devview-networkmock-core`. Both this module and `devview-networkmock-ktor`
     * reference the same delegate, guaranteeing a single DataStore instance.
     */
    override val dataStoreDelegate: DataStoreDelegate = NetworkMockDataStoreDelegate

    /**
     * Initialises the network mock repositories once the DataStore is ready.
     *
     * Called automatically by [com.worldline.devview.core.rememberModules] after
     * [initDataStore] has run.
     * The DataStore is retrieved from [dataStoreDelegate] and passed explicitly
     * to [NetworkMockInitializer.initialize] so the initializer remains
     * independent and testable.
     */
    @Composable
    override fun initModule() {
        NetworkMockInitializer.initialize(
            dataStore = dataStoreDelegate.get(),
            specPaths = specPaths,
            resourceLoader = { path ->
                try {
                    resourceLoader.load(path = path)
                } catch (e: MissingResourceException) {
                    throw IllegalStateException(e.message, e)
                }
            }
        )
        val retainedSession = viewModel {
            NetworkMockSessionViewModel(
                stateRepository = NetworkMockInitializer.requireStateRepository()
            )
        }
        session = retainedSession
        DisposableEffect(key1 = retainedSession) {
            onDispose {
                if (session === retainedSession) session = null
            }
        }
    }

    /** Name displayed on the home screen and default destination toolbar. */
    override val moduleName: String = "Network Mock"

    /** Summary displayed on the home screen. */
    override val subtitle: String = "Mock API responses"

    override val section: Section
        get() = Section.NETWORK

    override val destinations: PersistentMap<KClass<out NavKey>, DestinationMetadata> =
        persistentMapOf(
            NetworkMockDestination.Main.withActions {
                action(label = "Reload specs", icon = Icons.Rounded.Refresh) {
                    session?.requestReload()
                }
                action(
                    label = "Reset all to network",
                    icon = Icons.Rounded.RestartAlt,
                    popup = ModuleDestinationActionPopup(
                        title = "Reset all operations to network?",
                        subtitle = "Every mock, failure and sequence is cleared.",
                        confirmButton = "Reset",
                        dismissButton = "Cancel"
                    )
                ) {
                    session?.resetAll()
                }
            },
            NetworkMockDestination.ResponseDetail::class to DestinationMetadata(title = "Response"),
            NetworkMockDestination.SequenceBuilder::class to DestinationMetadata(
                title = "Build sequence"
            )
        )

    override val entryDestination: NavKey = NetworkMockDestination.Main

    override val registerSerializers: PolymorphicModuleBuilder<NavKey>.() -> Unit
        get() = {
            subclass(
                subclass = NetworkMockDestination.Main::class,
                serializer = NetworkMockDestination.Main.serializer()
            )
            subclass(
                subclass = NetworkMockDestination.ResponseDetail::class,
                serializer = NetworkMockDestination.ResponseDetail.serializer()
            )
            subclass(
                subclass = NetworkMockDestination.SequenceBuilder::class,
                serializer = NetworkMockDestination.SequenceBuilder.serializer()
            )
        }

    private var session: NetworkMockSessionViewModel? = null

    // Register each owned destination explicitly so its lifecycle and metadata remain visible.
    @Suppress("LongMethod")
    override fun EntryProviderScope<NavKey>.registerContent(
        onNavigateBack: () -> Unit,
        onNavigate: (NavKey) -> Unit,
        bottomPadding: Dp
    ) {
        entry<NetworkMockDestination.Main>(
            metadata = mapOf(
                pairs = arrayOf(
                    "com.worldline.devview.internal.interactionsEnabled" to
                        { session?.interactionsEnabled == true }
                )
            )
        ) {
            val retainedSession = checkNotNull(value = session) {
                "Register NetworkMock with rememberModules before displaying DevView"
            }
            NetworkMockScreen(
                modifier = Modifier
                    .fillMaxSize(),
                viewModel = viewModel {
                    NetworkMockViewModel(
                        configRepository = NetworkMockInitializer.requireConfigRepository(),
                        stateRepository = NetworkMockInitializer.requireStateRepository(),
                        savedStateHandle = createSavedStateHandle(),
                        sharedMutations = retainedSession.mutations
                    )
                },
                bottomPadding = bottomPadding,
                session = retainedSession,
                onOpenResponse = { operation, response ->
                    onNavigate(
                        NetworkMockDestination.ResponseDetail(
                            operation = operation,
                            statusCode = response.statusCode,
                            mediaType = response.mediaType,
                            exampleName = response.exampleName
                        )
                    )
                },
                onBuildSequence = { operation ->
                    onNavigate(
                        NetworkMockDestination.SequenceBuilder(operation = operation)
                    )
                }
            )
        }
        entry<NetworkMockDestination.ResponseDetail>(
            metadata = mapOf(
                pairs = arrayOf(
                    "com.worldline.devview.internal.interactionsEnabled" to
                        { session?.interactionsEnabled == true }
                )
            )
        ) { destination ->
            val retainedSession = checkNotNull(value = session) {
                "Register NetworkMock with rememberModules before displaying DevView"
            }
            ResponseDetailScreen(
                destination = destination,
                viewModel = viewModel {
                    OperationDetailViewModel(
                        operation = destination.operation,
                        configRepository = NetworkMockInitializer.requireConfigRepository(),
                        stateRepository = NetworkMockInitializer.requireStateRepository(),
                        savedStateHandle = createSavedStateHandle(),
                        mutations = retainedSession.mutations
                    )
                },
                onBack = onNavigateBack,
                bottomPadding = bottomPadding
            )
        }
        entry<NetworkMockDestination.SequenceBuilder>(
            metadata = mapOf(
                pairs = arrayOf(
                    "com.worldline.devview.internal.interactionsEnabled" to
                        { session?.interactionsEnabled == true }
                )
            )
        ) { destination ->
            val retainedSession = checkNotNull(value = session) {
                "Register NetworkMock with rememberModules before displaying DevView"
            }
            SequenceBuilderScreen(
                viewModel = viewModel {
                    OperationDetailViewModel(
                        operation = destination.operation,
                        configRepository = NetworkMockInitializer.requireConfigRepository(),
                        stateRepository = NetworkMockInitializer.requireStateRepository(),
                        savedStateHandle = createSavedStateHandle(),
                        mutations = retainedSession.mutations
                    )
                },
                onBack = onNavigateBack,
                bottomPadding = bottomPadding
            )
        }
    }
}
