package com.worldline.devview.core

import androidx.navigation3.runtime.NavKey

/**
 * Registers this [NavKey] as a destination with no title override and no actions.
 *
 * Use this when the destination should simply fall back to the module's
 * [Module.moduleName] as the top app bar title, and no contextual actions
 * are required.
 *
 * ## Example
 * ```kotlin
 * override val destinations = persistentMapOf(
 *     MyDestination.Main.asDestination()
 * )
 * ```
 *
 * @receiver The [NavKey] representing the destination to register.
 * @return A [Pair] of this [NavKey] and an empty [DestinationMetadata], suitable
 *   for use directly inside [kotlinx.collections.immutable.persistentMapOf].
 *
 * @see withTitle
 * @see withActions
 * @see DestinationMetadata
 */
public fun NavKey.asDestination(): Pair<NavKey, DestinationMetadata> = this to DestinationMetadata()

/**
 * Registers this [NavKey] as a destination with a static title and no actions.
 *
 * Use this when you want a specific title shown in the top app bar for this
 * destination, but no contextual action buttons are needed.
 *
 * ## Example
 * ```kotlin
 * override val destinations = persistentMapOf(
 *     MyDestination.Main.withTitle("My Screen")
 * )
 * ```
 *
 * To also add actions, use the overload that accepts a [DestinationMetadataBuilder] block:
 * ```kotlin
 * MyDestination.Main.withTitle("My Screen") {
 *     action(icon = Icons.Rounded.Refresh) { viewModel.refresh() }
 * }
 * ```
 *
 * @receiver The [NavKey] representing the destination to register.
 * @param title The title to display in the top app bar when this destination is active.
 * @return A [Pair] of this [NavKey] and a [DestinationMetadata] with the given [title]
 *   and no actions, suitable for use directly inside
 *   [kotlinx.collections.immutable.persistentMapOf].
 *
 * @see asDestination
 * @see withActions
 * @see DestinationMetadata
 */
public fun NavKey.withTitle(title: String): Pair<NavKey, DestinationMetadata> =
    this to DestinationMetadata(title = title)

/**
 * Registers this [NavKey] as a destination with a static title and contextual actions
 * defined via a [DestinationMetadataBuilder] DSL block.
 *
 * Use this when you want both a specific top app bar title and one or more contextual
 * action buttons for this destination.
 *
 * ## Example
 * ```kotlin
 * override val destinations = persistentMapOf(
 *     MyDestination.Main.withTitle("Analytics") {
 *         action(
 *             icon = Icons.Rounded.Delete,
 *             popup = ModuleDestinationActionPopup(
 *                 title = "Clear Logs",
 *                 subtitle = "This will remove all logged events.",
 *                 confirmButton = "Clear",
 *                 dismissButton = "Cancel"
 *             )
 *         ) {
 *             logger.clear()
 *         }
 *     }
 * )
 * ```
 *
 * To register a title without actions, use [withTitle] without a block.
 *
 * @receiver The [NavKey] representing the destination to register.
 * @param title The title to display in the top app bar when this destination is active.
 * @param block A [DestinationMetadataBuilder] DSL block in which you register actions
 *   via [DestinationMetadataBuilder.action].
 * @return A [Pair] of this [NavKey] and a [DestinationMetadata] with the given [title]
 *   and the actions registered in [block], suitable for use directly inside
 *   [kotlinx.collections.immutable.persistentMapOf].
 *
 * @see asDestination
 * @see withActions
 * @see DestinationMetadataBuilder
 * @see DestinationMetadata
 */
public fun NavKey.withTitle(
    title: String,
    block: DestinationMetadataBuilder.() -> Unit
): Pair<NavKey, DestinationMetadata> = this to DestinationMetadata(
    title = title,
    actions = DestinationMetadataBuilder().apply(block = block).build()
)

/**
 * Registers this [NavKey] as a destination with no title override and contextual
 * actions defined via a [DestinationMetadataBuilder] DSL block.
 *
 * Use this when the top app bar title should fall back to the module's
 * [Module.moduleName], but you still want one or more contextual action buttons
 * for this destination.
 *
 * ## Example
 * ```kotlin
 * override val destinations = persistentMapOf(
 *     MyDestination.Main.withActions {
 *         action(icon = Icons.Rounded.Delete) { viewModel.clear() }
 *     }
 * )
 * ```
 *
 * To also set an explicit title, use [withTitle] with a block instead.
 *
 * @receiver The [NavKey] representing the destination to register.
 * @param block A [DestinationMetadataBuilder] DSL block in which you register actions
 *   via [DestinationMetadataBuilder.action].
 * @return A [Pair] of this [NavKey] and a [DestinationMetadata] with no title override
 *   and the actions registered in [block], suitable for use directly inside
 *   [kotlinx.collections.immutable.persistentMapOf].
 *
 * @see asDestination
 * @see withTitle
 * @see DestinationMetadataBuilder
 * @see DestinationMetadata
 */
public fun NavKey.withActions(
    block: DestinationMetadataBuilder.() -> Unit
): Pair<NavKey, DestinationMetadata> = this to DestinationMetadata(
    actions = DestinationMetadataBuilder().apply(block = block).build()
)
