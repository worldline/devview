package com.worldline.devview.timecapsule

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.worldline.devview.core.DestinationMetadata
import com.worldline.devview.core.Module
import com.worldline.devview.core.ModuleDestinationActionPopup
import com.worldline.devview.core.Section
import com.worldline.devview.core.withActions
import kotlin.reflect.KClass
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder

/** Navigation destinations owned by [TimeCapsule]. */
public sealed interface TimeCapsuleDestination : NavKey {
    /** Timeline screen for the currently recording screen, if any. */
    @Serializable
    public data object Main : TimeCapsuleDestination
}

/**
 * DevView module that shows the state history of whichever screen is currently recording
 * via [TimeCapsuleEffect], and lets a developer restore any earlier state into it.
 *
 * Only one screen records at a time — whichever is topmost in the host app's own navigation
 * — and its history resets whenever that screen leaves composition.
 *
 * ## Integration
 * ```kotlin
 * val modules = rememberModules {
 *     module(module = TimeCapsule)
 * }
 * ```
 *
 * @see TimeCapsuleEffect
 * @see TimeCapsuleOwner
 */
public object TimeCapsule : Module {
    /** Default number of retained entries when none is specified to [TimeCapsuleEffect]. */
    public const val DEFAULT_MAX_ENTRIES: Int = 50

    private val activeCapsules = mutableStateListOf<ScreenCapsule<*>>()

    /**
     * The capsule of the screen currently recording, or `null` if no screen is composed
     * with [TimeCapsuleEffect].
     *
     * When multiple screens are briefly composed at once during a host-app navigation
     * transition, this is the most recently registered one.
     */
    internal val current: ScreenCapsule<*>? get() = activeCapsules.lastOrNull()

    internal fun register(capsule: ScreenCapsule<*>) {
        activeCapsules += capsule
    }

    internal fun unregister(capsule: ScreenCapsule<*>) {
        activeCapsules -= capsule
    }

    /** Name displayed on the home screen and default destination toolbar. */
    override val moduleName: String = "Time Capsule"

    /** Summary displayed on the home screen. */
    override val subtitle: String = "Screen state history"

    /** Icon identifying this module on the home screen. */
    override val icon: ImageVector = Icons.Rounded.History

    override val section: Section = Section.LOGGING

    override val destinations: PersistentMap<KClass<out NavKey>, DestinationMetadata> = persistentMapOf(
        TimeCapsuleDestination.Main.withActions {
            action(
                label = "Clear history",
                icon = Icons.Rounded.Delete,
                popup = ModuleDestinationActionPopup(
                    title = "Clear Timeline",
                    subtitle = "Remove all captured states from memory.",
                    confirmButton = "Clear",
                    dismissButton = "Cancel"
                )
            ) {
                current?.clear()
            }
        }
    )

    override val entryDestination: NavKey = TimeCapsuleDestination.Main

    override val registerSerializers: PolymorphicModuleBuilder<NavKey>.() -> Unit = {
        subclass(
            subclass = TimeCapsuleDestination.Main::class,
            serializer = TimeCapsuleDestination.Main.serializer()
        )
    }

    override fun EntryProviderScope<NavKey>.registerContent(
        onNavigateBack: () -> Unit,
        onNavigate: (NavKey) -> Unit,
        bottomPadding: Dp
    ) {
        entry<TimeCapsuleDestination.Main> {
            TimeCapsuleScreen(
                modifier = Modifier.fillMaxSize(),
                bottomPadding = bottomPadding
            )
        }
    }
}
