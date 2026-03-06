package com.worldline.devview.featureflip

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.worldline.devview.core.DestinationMetadata
import com.worldline.devview.core.Module
import com.worldline.devview.core.Section
import com.worldline.devview.core.withTitle
import com.worldline.devview.utils.DataStoreDelegate
import com.worldline.devview.utils.RequiresDataStore
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder

/** The filename used for the FeatureFlip DataStore preferences file. */
internal const val FEATURE_FLIP_DATASTORE_NAME = "feature_flip_datastore.preferences_pb"

/**
 * Navigation destinations for the FeatureFlip module.
 *
 * This sealed interface defines all possible navigation targets within the
 * FeatureFlip module. Each destination is serializable to support type-safe
 * navigation with the Navigation3 library.
 *
 * @see FeatureFlip
 */
public sealed interface FeatureFlipDestination : NavKey {
    /**
     * Main feature flip management screen destination.
     *
     * This destination displays the [FeatureFlipScreen] component, which shows
     * a searchable and filterable list of all registered feature flags with
     * controls to toggle their state.
     *
     * ## Navigation Example
     * ```kotlin
     * navController.navigate(FeatureFlipDestination.Main)
     * ```
     *
     * @see FeatureFlipScreen
     */
    @Serializable
    public data object Main : FeatureFlipDestination

    // Add more destinations as needed, for example:
    // @Serializable
    // public data class Detail(val featureId: String) : FeatureFlipDestination
}

/**
 * FeatureFlip module for the DevView developer tools suite.
 *
 * This module provides feature flag management capabilities, allowing developers
 * to toggle features on/off during development and testing. It supports both
 * local features (device-specific) and remote features (controlled by remote
 * configuration with local override capability).
 *
 * ## Features
 * - Local feature flags (simple on/off toggles)
 * - Remote feature flags (remote config with local overrides)
 * - Persistent state using DataStore
 * - Search and filter capabilities
 * - Real-time UI updates
 * - Tri-state controls for remote features (Remote/Off/On)
 *
 * ## Integration
 *
 * The FeatureFlip module integrates with the DevView core framework as part of
 * the FEATURES section. It provides navigation destinations and UI components
 * that can be accessed through the DevView menu.
 *
 * ### Module Configuration
 * ```kotlin
 * val devView = DevView(
 *     modules = listOf(
 *         FeatureFlip,
 *         // other modules...
 *     )
 * )
 * ```
 *
 * ### Defining Features
 * ```kotlin
 * val features = listOf(
 *     Feature.LocalFeature(
 *         name = "dark_mode",
 *         description = "Enable dark theme",
 *         isEnabled = false
 *     ),
 *     Feature.RemoteFeature(
 *         name = "new_checkout",
 *         description = "New checkout flow",
 *         defaultRemoteValue = true,
 *         state = FeatureState.REMOTE
 *     )
 * )
 * ```
 *
 * ### Using Features in Code
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     val featureHandler = LocalFeatureHandler.current
 *     val isDarkModeEnabled by featureHandler.isFeatureEnabled("dark_mode")
 *
 *     if (isDarkModeEnabled) {
 *         DarkThemeContent()
 *     } else {
 *         LightThemeContent()
 *     }
 * }
 * ```
 *
 * @see Module
 * @see Section.FEATURES
 * @see com.worldline.devview.featureflip.model.Feature
 * @see com.worldline.devview.featureflip.model.FeatureHandler
 * @see FeatureFlipScreen
 */
public object FeatureFlip : Module, RequiresDataStore {
    override val dataStoreName: String = FEATURE_FLIP_DATASTORE_NAME

    /**
     * The [DataStoreDelegate] instance for the FeatureFlip module.
     *
     * Initialised automatically by [com.worldline.devview.core.rememberModules]
     * before any call to `rememberFeatureHandler`. Accessed internally by
     * `rememberFeatureHandler` to obtain the DataStore instance without requiring
     * Compose context.
     */
    override val dataStoreDelegate: DataStoreDelegate = DataStoreDelegate()

    /**
     * The section this module belongs to in the DevView menu.
     *
     * FeatureFlip is categorized under the FEATURES section alongside other
     * feature management and development tools.
     */
    override val section: Section
        get() = Section.FEATURES

    /**
     * Maps each navigation destination in this module to its [DestinationMetadata].
     *
     * Currently includes only the main feature management screen.
     */
    override val destinations: PersistentMap<NavKey, DestinationMetadata> = persistentMapOf(
        FeatureFlipDestination.Main.withTitle(title = "Feature Flip")
    )

    /**
     * Registers serializers for navigation destinations.
     *
     * This is required for type-safe navigation with the Navigation3 library.
     */
    override val registerSerializers: PolymorphicModuleBuilder<NavKey>.() -> Unit
        get() = {
            subclass(
                subclass = FeatureFlipDestination.Main::class,
                serializer = FeatureFlipDestination.Main.serializer()
            )
        }

    /**
     * Registers the composable content for each navigation destination.
     *
     * This defines what UI is displayed when navigating to each destination
     * in the FeatureFlip module.
     *
     * @param onNavigateBack Callback to navigate back to the previous screen.
     * @param onNavigate Callback to navigate to a new destination.
     */
    override fun EntryProviderScope<NavKey>.registerContent(
        onNavigateBack: () -> Unit,
        onNavigate: (NavKey) -> Unit,
        bottomPadding: Dp
    ) {
        entry<FeatureFlipDestination.Main> {
            FeatureFlipScreen(
                modifier = Modifier
                    .fillMaxSize(),
                bottomPadding = bottomPadding
            )
        }
    }
}
