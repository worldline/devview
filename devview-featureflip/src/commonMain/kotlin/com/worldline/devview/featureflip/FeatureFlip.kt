package com.worldline.devview.featureflip

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.worldline.devview.core.Module
import com.worldline.devview.core.Section
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder

/**
 * Navigation destinations for the FeatureFlip module.
 * All screens within FeatureFlip are defined here.
 */
public sealed interface FeatureFlipDestination : NavKey {
    /**
     * Main feature flip list screen
     */
    @Serializable
    public data object Main : FeatureFlipDestination

    // Add more destinations as needed, for example:
    // @Serializable
    // public data class Detail(val featureId: String) : FeatureFlipDestination
}

/**
 * FeatureFlip module - manages feature flag toggles.
 * This is a regular object, not serializable.
 */
public object FeatureFlip : Module {
    override val section: Section
        get() = Section.FEATURES

    override val destinations: ImmutableList<NavKey> = persistentListOf(
        FeatureFlipDestination.Main
    )

    override val registerSerializers: PolymorphicModuleBuilder<NavKey>.() -> Unit
        get() = {
            subclass(
                subclass = FeatureFlipDestination.Main::class,
                serializer = FeatureFlipDestination.Main.serializer()
            )
        }

    override fun EntryProviderScope<NavKey>.registerContent(
        onNavigateBack: () -> Unit,
        onNavigate: (NavKey) -> Unit
    ) {
        entry<FeatureFlipDestination.Main> {
            Scaffold { paddingValues ->
                FeatureFlipScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues = paddingValues)
                )
            }
        }
    }
}
