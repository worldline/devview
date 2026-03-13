package com.worldline.devview.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.worldline.devview.core.DestinationMetadata
import com.worldline.devview.core.Module
import com.worldline.devview.core.Section
import com.worldline.devview.core.asDestination
import com.worldline.devview.core.withTitle
import kotlin.reflect.KClass
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder

/**
 * Test module navigation destinations.
 */
public sealed interface TestModuleNavigation : NavKey {
    @Serializable
    public data object Main : TestModuleNavigation

    @Serializable
    public data object Detail : TestModuleNavigation
}

/**
 * Sample test module for DevView demonstration.
 *
 * This module showcases basic navigation between two screens.
 */
public object TestModule : Module {
    override val section: Section
        get() = Section.CUSTOM

    override val destinations: PersistentMap<KClass<out NavKey>, DestinationMetadata> = persistentMapOf(
        TestModuleNavigation.Main.withTitle(title = "Test Module"),
        TestModuleNavigation.Detail.asDestination()
    )

    override val entryDestination: NavKey = TestModuleNavigation.Main

    override val registerSerializers: PolymorphicModuleBuilder<NavKey>.() -> Unit
        get() = {
            subclass(
                subclass = TestModuleNavigation.Main::class,
                serializer = TestModuleNavigation.Main.serializer()
            )
            subclass(
                subclass = TestModuleNavigation.Detail::class,
                serializer = TestModuleNavigation.Detail.serializer()
            )
        }

    override fun EntryProviderScope<NavKey>.registerContent(
        onNavigateBack: () -> Unit,
        onNavigate: (NavKey) -> Unit,
        bottomPadding: Dp
    ) {
        entry<TestModuleNavigation.Main> {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    modifier = Modifier.align(alignment = Alignment.Center),
                    text = "Main screen"
                )
                Button(
                    onClick = {
                        onNavigate(TestModuleNavigation.Detail)
                    }
                ) {
                    Text(text = "Go to Detail")
                }
            }
        }

        entry<TestModuleNavigation.Detail> {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    modifier = Modifier.align(alignment = Alignment.Center),
                    text = "Detail screen"
                )
                Button(
                    onClick = {
                        onNavigateBack()
                    }
                ) {
                    Text(text = "Go Back")
                }
            }
        }
    }
}
