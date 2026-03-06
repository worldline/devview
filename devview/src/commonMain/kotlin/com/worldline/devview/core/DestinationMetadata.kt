package com.worldline.devview.core

import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

/**
 * Metadata associated with a single navigation destination within a [Module].
 *
 * Each entry in [Module.destinations] maps a [NavKey][androidx.navigation3.runtime.NavKey] to a
 * [DestinationMetadata] instance. This metadata controls:
 * - The title displayed in the DevView top app bar while this destination is active.
 * - The contextual action buttons rendered in the top app bar for this destination.
 *
 * ## Title Resolution
 *
 * The top app bar title is resolved in the following order:
 * 1. [title] from this metadata, if non-null.
 * 2. [Module.moduleName] as a fallback when [title] is null.
 *
 * ## Actions
 *
 * [actions] is an ordered list of [ModuleDestinationAction] items rendered as icon buttons
 * in the top app bar while this destination is active. An empty list (the default) means
 * no actions are shown.
 *
 * ## Creating instances
 *
 * Prefer the [NavKey][androidx.navigation3.runtime.NavKey] extension functions for concise declarations:
 *
 * ```kotlin
 * override val destinations = persistentMapOf(
 *     MyDestination.Main.withTitle("My Screen"),           // title, no actions
 *     MyDestination.Detail.asDestination()                 // no title, no actions
 * )
 *
 * override val destinations = persistentMapOf(
 *     MyDestination.Main.withTitle("Logs") {
 *         action(icon = Icons.Rounded.Delete) { logger.clear() }
 *     }
 * )
 * ```
 *
 * The explicit form is also supported when full control is needed:
 *
 * ```kotlin
 * MyDestination.Main to DestinationMetadata(
 *     title = "My Screen",
 *     actions = persistentListOf(
 *         ModuleDestinationAction(icon = Icons.Rounded.Refresh, action = { logger.clear() })
 *     )
 * )
 * ```
 *
 * @property title The title to display in the top app bar when this destination is active.
 *   Pass `null` (the default) to fall back to [Module.moduleName].
 * @property actions The contextual actions to display as icon buttons in the top app bar
 *   when this destination is active. Defaults to an empty list (no actions).
 *
 * @see Module.destinations
 * @see ModuleDestinationAction
 * @see ModuleDestinationActionPopup
 */
public data class DestinationMetadata(
    val title: String? = null,
    val actions: PersistentList<ModuleDestinationAction> = persistentListOf()
)

/**
 * DSL builder for constructing the [actions] list of a [DestinationMetadata].
 *
 * Used as the receiver of the `block` lambda in
 * [withTitle][com.worldline.devview.core.withTitle] and
 * [withActions][com.worldline.devview.core.withActions]. Not intended to be instantiated
 * directly — always go through those extension functions.
 *
 * @see com.worldline.devview.core.withTitle
 * @see com.worldline.devview.core.withActions
 * @see ModuleDestinationAction
 */
public class DestinationMetadataBuilder internal constructor() {
    private val actions = mutableListOf<ModuleDestinationAction>()

    /**
     * Adds a contextual action to this destination's top app bar.
     *
     * Each action is rendered as an [IconButton][androidx.compose.material3.IconButton]. If [popup] is
     * provided, a confirmation dialog is shown before [action] is invoked.
     *
     * ## Action scope
     *
     * The [action] lambda is captured once at module construction time. It is therefore
     * best suited for calls that are **lifecycle-independent**:
     *
     * - **Module-owned singletons** — the simplest and most common case:
     * ```kotlin
     * action(icon = Icons.Rounded.Delete) { AnalyticsLogger.clear() }
     * ```
     *
     * - **Cross-boundary commands (e.g. triggering a ViewModel)** — use a [SharedFlow][kotlinx.coroutines.flow.SharedFlow]
     * on the module as an event bus, then observe it at the call site where the ViewModel
     * is available:
     * ```kotlin
     * // In the module
     * val onClearRequested = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
     * // ...
     * action(icon = Icons.Rounded.Delete) { onClearRequested.tryEmit(Unit) }
     *
     * // In the host Composable, where the ViewModel is in scope
     * LaunchedEffect(myModule) {
     *     myModule.onClearRequested.collect { viewModel.clearData() }
     * }
     * ```
     * This keeps the module decoupled from the host app's DI graph while still
     * allowing ViewModel functions to be triggered safely.
     *
     * @param icon The icon to display for this action button.
     * @param popup Optional confirmation dialog shown before [action] is invoked.
     *   Pass `null` (the default) to invoke [action] immediately on tap.
     * @param action The callback invoked when the action is confirmed (or tapped
     *   directly if no [popup] is provided).
     *
     * @see ModuleDestinationAction
     * @see ModuleDestinationActionPopup
     */
    public fun action(
        icon: ImageVector,
        popup: ModuleDestinationActionPopup? = null,
        action: () -> Unit
    ) {
        actions.add(element = ModuleDestinationAction(icon = icon, action = action, popup = popup))
    }

    /**
     * Builds and returns the immutable list of registered [ModuleDestinationAction] items.
     *
     * Called internally by the [NavKey][androidx.navigation3.runtime.NavKey] extension functions
     * after the builder block
     * has been applied. Not intended for direct use.
     *
     * @return A [PersistentList] of all actions added via [action].
     */
    internal fun build(): PersistentList<ModuleDestinationAction> = actions.toPersistentList()
}
