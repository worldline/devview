package com.worldline.devview.core

import androidx.navigation3.runtime.NavKey
import kotlin.reflect.KClass

/**
 * Registers this [NavKey] as a destination with no title override and no actions.
 *
 * Use this when the destination should simply fall back to the module's
 * [Module.moduleName] as the top app bar title, and no contextual actions
 * are required.
 *
 * This overload is a convenience for `data object` destinations — it delegates to
 * the [KClass] overload using `this::class`.
 *
 * ## Example
 * ```kotlin
 * override val destinations = persistentMapOf(
 *     MyDestination.Main.asDestination()
 * )
 * ```
 *
 * @receiver The [NavKey] instance representing the destination to register.
 * @return A [Pair] of this destination's [KClass] and an empty [DestinationMetadata],
 *   suitable for use directly inside [kotlinx.collections.immutable.persistentMapOf].
 *
 * @see KClass.asDestination
 * @see withTitle
 * @see withActions
 * @see DestinationMetadata
 */
public fun NavKey.asDestination(): Pair<KClass<out NavKey>, DestinationMetadata> =
    this::class.asDestination()

/**
 * Registers this [KClass] as a destination with no title override and no actions.
 *
 * Prefer this overload for `data class` destinations, where no representative
 * instance is available at module-construction time. The class itself is used as
 * the map key so that metadata lookup works correctly for any instance of the
 * destination, regardless of its runtime parameters.
 *
 * ## Example
 * ```kotlin
 * override val destinations = persistentMapOf(
 *     MyDestination.Main.asDestination(),           // data object — instance overload
 *     MyDestination.Detail::class.asDestination()   // data class — KClass overload
 * )
 * ```
 *
 * @receiver The [KClass] of the [NavKey] subtype representing the destination to register.
 * @return A [Pair] of this [KClass] and an empty [DestinationMetadata], suitable
 *   for use directly inside [kotlinx.collections.immutable.persistentMapOf].
 *
 * @see NavKey.asDestination
 * @see withTitle
 * @see withActions
 * @see DestinationMetadata
 */
public fun KClass<out NavKey>.asDestination(): Pair<KClass<out NavKey>, DestinationMetadata> =
    this to DestinationMetadata()

/**
 * Registers this [NavKey] as a destination with a static title and no actions.
 *
 * Use this when you want a specific title shown in the top app bar for this
 * destination, but no contextual action buttons are needed.
 *
 * This overload is a convenience for `data object` destinations — it delegates to
 * the [KClass] overload using `this::class`.
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
 * @receiver The [NavKey] instance representing the destination to register.
 * @param title The title to display in the top app bar when this destination is active.
 * @return A [Pair] of this destination's [KClass] and a [DestinationMetadata] with the
 *   given [title] and no actions, suitable for use directly inside
 *   [kotlinx.collections.immutable.persistentMapOf].
 *
 * @see KClass.withTitle
 * @see asDestination
 * @see withActions
 * @see DestinationMetadata
 */
public fun NavKey.withTitle(title: String): Pair<KClass<out NavKey>, DestinationMetadata> =
    this::class.withTitle(title = title)

/**
 * Registers this [KClass] as a destination with a static title and no actions.
 *
 * Prefer this overload for `data class` destinations, where no representative
 * instance is available at module-construction time. The class itself is used as
 * the map key so that the title is resolved correctly for any instance of the
 * destination, regardless of its runtime parameters.
 *
 * ## Example
 * ```kotlin
 * override val destinations = persistentMapOf(
 *     MyDestination.Main.withTitle("Overview"),             // data object — instance overload
 *     MyDestination.Detail::class.withTitle("Detail")      // data class — KClass overload
 * )
 * ```
 *
 * To also add actions, use the overload that accepts a [DestinationMetadataBuilder] block:
 * ```kotlin
 * MyDestination.Detail::class.withTitle("Detail") {
 *     action(icon = Icons.Rounded.Refresh) { viewModel.refresh() }
 * }
 * ```
 *
 * @receiver The [KClass] of the [NavKey] subtype representing the destination to register.
 * @param title The title to display in the top app bar when this destination is active.
 * @return A [Pair] of this [KClass] and a [DestinationMetadata] with the given [title]
 *   and no actions, suitable for use directly inside
 *   [kotlinx.collections.immutable.persistentMapOf].
 *
 * @see NavKey.withTitle
 * @see asDestination
 * @see withActions
 * @see DestinationMetadata
 */
public fun KClass<out NavKey>.withTitle(
    title: String
): Pair<KClass<out NavKey>, DestinationMetadata> = this to DestinationMetadata(title = title)

/**
 * Registers this [NavKey] as a destination with a static title and contextual actions
 * defined via a [DestinationMetadataBuilder] DSL block.
 *
 * Use this when you want both a specific top app bar title and one or more contextual
 * action buttons for this destination.
 *
 * This overload is a convenience for `data object` destinations — it delegates to
 * the [KClass] overload using `this::class`.
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
 * @receiver The [NavKey] instance representing the destination to register.
 * @param title The title to display in the top app bar when this destination is active.
 * @param block A [DestinationMetadataBuilder] DSL block in which you register actions
 *   via [DestinationMetadataBuilder.action].
 * @return A [Pair] of this destination's [KClass] and a [DestinationMetadata] with the
 *   given [title] and the actions registered in [block], suitable for use directly inside
 *   [kotlinx.collections.immutable.persistentMapOf].
 *
 * @see KClass.withTitle
 * @see asDestination
 * @see withActions
 * @see DestinationMetadataBuilder
 * @see DestinationMetadata
 */
public fun NavKey.withTitle(
    title: String,
    block: DestinationMetadataBuilder.() -> Unit
): Pair<KClass<out NavKey>, DestinationMetadata> = this::class.withTitle(
    title = title,
    block = block
)

/**
 * Registers this [KClass] as a destination with a static title and contextual actions
 * defined via a [DestinationMetadataBuilder] DSL block.
 *
 * Prefer this overload for `data class` destinations, where no representative
 * instance is available at module-construction time. The class itself is used as
 * the map key so that the title and actions are resolved correctly for any instance
 * of the destination, regardless of its runtime parameters.
 *
 * ## Example
 * ```kotlin
 * override val destinations = persistentMapOf(
 *     MyDestination.Main.withTitle("Overview") {          // data object — instance overload
 *         action(icon = Icons.Rounded.Refresh) { viewModel.refresh() }
 *     },
 *     MyDestination.Detail::class.withTitle("Detail") {  // data class — KClass overload
 *         action(icon = Icons.Rounded.Share) { viewModel.share() }
 *     }
 * )
 * ```
 *
 * To register a title without actions, use [withTitle] without a block.
 *
 * @receiver The [KClass] of the [NavKey] subtype representing the destination to register.
 * @param title The title to display in the top app bar when this destination is active.
 * @param block A [DestinationMetadataBuilder] DSL block in which you register actions
 *   via [DestinationMetadataBuilder.action].
 * @return A [Pair] of this [KClass] and a [DestinationMetadata] with the given [title]
 *   and the actions registered in [block], suitable for use directly inside
 *   [kotlinx.collections.immutable.persistentMapOf].
 *
 * @see NavKey.withTitle
 * @see asDestination
 * @see withActions
 * @see DestinationMetadataBuilder
 * @see DestinationMetadata
 */
public fun KClass<out NavKey>.withTitle(
    title: String,
    block: DestinationMetadataBuilder.() -> Unit
): Pair<KClass<out NavKey>, DestinationMetadata> = this to DestinationMetadata(
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
 * This overload is a convenience for `data object` destinations — it delegates to
 * the [KClass] overload using `this::class`.
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
 * @receiver The [NavKey] instance representing the destination to register.
 * @param block A [DestinationMetadataBuilder] DSL block in which you register actions
 *   via [DestinationMetadataBuilder.action].
 * @return A [Pair] of this destination's [KClass] and a [DestinationMetadata] with no
 *   title override and the actions registered in [block], suitable for use directly inside
 *   [kotlinx.collections.immutable.persistentMapOf].
 *
 * @see KClass.withActions
 * @see asDestination
 * @see withTitle
 * @see DestinationMetadataBuilder
 * @see DestinationMetadata
 */
public fun NavKey.withActions(
    block: DestinationMetadataBuilder.() -> Unit
): Pair<KClass<out NavKey>, DestinationMetadata> = this::class.withActions(block = block)

/**
 * Registers this [KClass] as a destination with no title override and contextual
 * actions defined via a [DestinationMetadataBuilder] DSL block.
 *
 * Prefer this overload for `data class` destinations, where no representative
 * instance is available at module-construction time. The class itself is used as
 * the map key so that actions are resolved correctly for any instance of the
 * destination, regardless of its runtime parameters.
 *
 * ## Example
 * ```kotlin
 * override val destinations = persistentMapOf(
 *     MyDestination.Main.withActions {                    // data object — instance overload
 *         action(icon = Icons.Rounded.Delete) { viewModel.clear() }
 *     },
 *     MyDestination.Detail::class.withActions {          // data class — KClass overload
 *         action(icon = Icons.Rounded.Share) { viewModel.share() }
 *     }
 * )
 * ```
 *
 * To also set an explicit title, use [withTitle] with a block instead.
 *
 * @receiver The [KClass] of the [NavKey] subtype representing the destination to register.
 * @param block A [DestinationMetadataBuilder] DSL block in which you register actions
 *   via [DestinationMetadataBuilder.action].
 * @return A [Pair] of this [KClass] and a [DestinationMetadata] with no title override
 *   and the actions registered in [block], suitable for use directly inside
 *   [kotlinx.collections.immutable.persistentMapOf].
 *
 * @see NavKey.withActions
 * @see asDestination
 * @see withTitle
 * @see DestinationMetadataBuilder
 * @see DestinationMetadata
 */
public fun KClass<out NavKey>.withActions(
    block: DestinationMetadataBuilder.() -> Unit
): Pair<KClass<out NavKey>, DestinationMetadata> = this to DestinationMetadata(
    actions = DestinationMetadataBuilder().apply(block = block).build()
)
