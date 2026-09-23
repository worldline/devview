package com.worldline.devview.core

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

/**
 * A single selectable entry within a [ModuleDestinationAction]'s dropdown menu.
 *
 * Rendered as a [DropdownMenuItem][androidx.compose.material3.DropdownMenuItem]. Tapping it
 * invokes [onClick] and then dismisses the menu.
 *
 * @property label The text displayed for this menu entry.
 * @property onClick The callback invoked when this entry is tapped.
 *
 * @see ModuleDestinationAction.menuItems
 * @see DestinationMetadataBuilder.menu
 */
public data class ModuleDestinationActionMenuItem(val label: String, val onClick: () -> Unit)

/**
 * DSL builder for constructing the ordered list of [ModuleDestinationActionMenuItem] entries
 * of a [DestinationMetadataBuilder.menu] action.
 *
 * Not intended to be instantiated directly — always go through [DestinationMetadataBuilder.menu].
 *
 * @see DestinationMetadataBuilder.menu
 * @see ModuleDestinationActionMenuItem
 */
public class ModuleDestinationActionMenuBuilder internal constructor() {
    private val items = mutableListOf<ModuleDestinationActionMenuItem>()

    /**
     * Adds a selectable entry to this dropdown menu.
     *
     * @param label The text displayed for this menu entry.
     * @param onClick The callback invoked when this entry is tapped. Captured at construction
     *   time — see [DestinationMetadataBuilder.action]'s "Action scope" section for guidance on
     *   triggering lifecycle-bound code (e.g. a ViewModel) from here.
     */
    public fun item(label: String, onClick: () -> Unit) {
        items.add(element = ModuleDestinationActionMenuItem(label = label, onClick = onClick))
    }

    /**
     * Builds and returns the immutable list of registered [ModuleDestinationActionMenuItem]
     * entries.
     *
     * Called internally by [DestinationMetadataBuilder.menu] after the builder block has been
     * applied. Not intended for direct use.
     */
    internal fun build(): PersistentList<ModuleDestinationActionMenuItem> = items.toPersistentList()
}
