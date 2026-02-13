package com.worldline.devview.networkmock

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.worldline.devview.core.Module
import com.worldline.devview.core.Section
import com.worldline.devview.networkmock.repository.MockStateRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import org.jetbrains.compose.resources.ExperimentalResourceApi

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

    // Add more destinations as needed, for example:
    // @Serializable
    // public data class Detail(val mockId: String) : NetworkMockDestination
}

/**
 * NetworkMock module - manages network request/response mocking for development and testing.
 * This is a regular object, not serializable.
 *
 * @property resourceLoader Function to load resource bytes, must be provided by integrator
 * @property stateRepository MockStateRepository instance for state management
 */
public class NetworkMock(
    private val resourceLoader: suspend (String) -> ByteArray,
    private val stateRepository: MockStateRepository
) : Module {
    override val section: Section
        get() = Section.LOGGING

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

    @OptIn(ExperimentalResourceApi::class)
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
                    resourceLoader = resourceLoader,
                    stateRepository = stateRepository
                )
            }
        }
    }
}
