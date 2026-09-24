package com.worldline.devview.core

import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.collections.immutable.PersistentList

/**
 * Descriptor for a single icon button rendered in the DevView top app bar for the active
 * destination.
 *
 * Tapping the resulting icon button resolves to exactly one of three behaviours, in this order
 * of precedence:
 * 1. If [menuItems] is non-null, a dropdown menu listing those entries is shown.
 * 2. Otherwise, if [popup] is non-null, a confirmation dialog is shown; [action] runs when
 *    confirmed.
 * 3. Otherwise, [action] runs immediately.
 *
 * Prefer building instances via [DestinationMetadataBuilder.action] or
 * [DestinationMetadataBuilder.menu] rather than this constructor directly.
 *
 * @property icon The icon to display for this action button.
 * @property action The callback invoked when the icon is tapped directly (no [menuItems], no
 *   [popup]) or when a [popup] confirmation is confirmed. Ignored when [menuItems] is non-null.
 * @property popup Optional confirmation dialog shown before [action] runs. Ignored when
 *   [menuItems] is non-null.
 * @property menuItems Optional dropdown menu entries shown when the icon is tapped, instead of
 *   invoking [action] or [popup] directly. `null` (the default) means no dropdown.
 *
 * @see DestinationMetadataBuilder.action
 * @see DestinationMetadataBuilder.menu
 * @see ModuleDestinationActionMenuItem
 */
public data class ModuleDestinationAction(
    val icon: ImageVector,
    val action: () -> Unit = {},
    val popup: ModuleDestinationActionPopup? = null,
    val menuItems: PersistentList<ModuleDestinationActionMenuItem>? = null
)
