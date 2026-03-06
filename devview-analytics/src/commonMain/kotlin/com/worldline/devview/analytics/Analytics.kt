package com.worldline.devview.analytics

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.worldline.devview.analytics.model.AnalyticsLogCategory.Action.Click
import com.worldline.devview.analytics.model.AnalyticsLogCategory.Performance.Error
import com.worldline.devview.analytics.model.AnalyticsLogType
import com.worldline.devview.core.DestinationMetadata
import com.worldline.devview.core.Module
import com.worldline.devview.core.ModuleDestinationActionPopup
import com.worldline.devview.core.Section
import com.worldline.devview.core.withTitle
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder

/**
 * Navigation destinations for the Analytics module.
 *
 * This sealed interface defines all possible navigation targets within the
 * Analytics module. Each destination is serializable to support type-safe
 * navigation with the Navigation3 library.
 *
 * @see Analytics
 */
public sealed interface AnalyticsDestination : NavKey {
    /**
     * Main analytics dashboard screen destination.
     *
     * This destination displays the [AnalyticsScreen] component, which shows
     * a tabular view of all logged analytics events with filtering and
     * search capabilities.
     *
     * ## Navigation Example
     * ```kotlin
     * navController.navigate(AnalyticsDestination.Main)
     * ```
     *
     * @see AnalyticsScreen
     */
    @Serializable
    public data object Main : AnalyticsDestination

    // Add more destinations as needed, for example:
    // @Serializable
    // public data class Detail(val analyticsId: String) : AnalyticsDestination
}

/**
 * Analytics module for the DevView developer tools suite.
 *
 * This module provides real-time analytics event monitoring and visualization,
 * allowing developers to track and debug analytics integration during development
 * and testing.
 *
 * ## Features
 * - Real-time analytics event logging
 * - Tabular display of events with timestamps
 * - Multiple event type support (SCREEN, EVENT, CUSTOM)
 * - Thread-safe in-memory storage
 * - Reactive UI updates
 *
 * ## Integration
 *
 * The Analytics module integrates with the DevView core framework as part of
 * the LOGGING section. It provides navigation destinations and UI components
 * that can be accessed through the DevView menu.
 *
 * ### Module Configuration
 * ```kotlin
 * val devView = DevView(
 *     modules = listOf(
 *         Analytics,
 *         // other modules...
 *     )
 * )
 * ```
 *
 * ### Logging Events
 * ```kotlin
 * AnalyticsLogger.log(
 *     AnalyticsLog(
 *         tag = "user_action",
 *         screenClass = "HomeScreen",
 *         timestamp = System.currentTimeMillis(),
 *         type = AnalyticsLogType.EVENT
 *     )
 * )
 * ```
 *
 * @see Module
 * @see Section.LOGGING
 * @see AnalyticsLogger
 * @see AnalyticsScreen
 */
public class Analytics(
    public val highlightedLogType1: AnalyticsLogType? = Click,
    public val highlightedLogType2: AnalyticsLogType? = Error,
    public val highlightedLogType3: AnalyticsLogType? = null
) : Module {
    /**
     * The section this module belongs to in the DevView menu.
     *
     * Analytics is categorized under the LOGGING section alongside other
     * debugging and monitoring tools.
     */
    override val section: Section
        get() = Section.LOGGING

    /**
     * Maps each navigation destination in this module to its [DestinationMetadata].
     *
     * The main analytics screen carries a "Clear" action that removes all logged
     * events from the in-memory store, with a confirmation popup to prevent
     * accidental data loss.
     */
    override val destinations: PersistentMap<NavKey, DestinationMetadata> = persistentMapOf(
        AnalyticsDestination.Main.withTitle(title = "Analytics") {
            action(
                icon = Icons.Rounded.Delete,
                popup = ModuleDestinationActionPopup(
                    title = "Clear Logs",
                    subtitle = "Remove all logged analytics events from the dashboard",
                    confirmButton = "Clear",
                    dismissButton = "Cancel"
                )
            ) {
                AnalyticsLogger.clear()
            }
        }
    )

    /**
     * Registers serializers for navigation destinations.
     *
     * This is required for type-safe navigation with the Navigation3 library.
     */
    override val registerSerializers: PolymorphicModuleBuilder<NavKey>.() -> Unit
        get() = {
            subclass(
                subclass = AnalyticsDestination.Main::class,
                serializer = AnalyticsDestination.Main.serializer()
            )
        }

    /**
     * Registers the composable content for each navigation destination.
     *
     * This defines what UI is displayed when navigating to each destination
     * in the Analytics module.
     *
     * @param onNavigateBack Callback to navigate back to the previous screen.
     * @param onNavigate Callback to navigate to a new destination.
     */
    override fun EntryProviderScope<NavKey>.registerContent(
        onNavigateBack: () -> Unit,
        onNavigate: (NavKey) -> Unit,
        bottomPadding: Dp
    ) {
        entry<AnalyticsDestination.Main> {
            AnalyticsScreen(
                modifier = Modifier
                    .fillMaxSize(),
                highlightedAnalyticsLogTypes = listOfNotNull(
                    highlightedLogType1,
                    highlightedLogType2,
                    highlightedLogType3
                ).toPersistentList(),
                bottomPadding = bottomPadding
            )
        }
    }
}
