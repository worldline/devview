package com.worldline.devview.consolelogger

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import co.touchlab.kermit.Logger
import com.worldline.devview.core.DestinationMetadata
import com.worldline.devview.core.Module
import com.worldline.devview.core.ModuleDestinationActionPopup
import com.worldline.devview.core.Section
import com.worldline.devview.core.withTitle
import kotlin.reflect.KClass
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder

/**
 * Navigation destinations for the Console module.
 *
 * @see Console
 */
public sealed interface ConsoleDestination : NavKey {
    /**
     * Main console log screen destination.
     *
     * @see ConsoleScreen
     */
    @Serializable
    public data object Main : ConsoleDestination
}

/**
 * Console Logger module for the DevView developer tools suite.
 *
 * Captures the platform's native console output — `logcat` on Android, a `stdout`/`stderr`
 * redirect on iOS — and displays it in a filterable, auto-following list, similar to
 * viewing Logcat in Android Studio.
 *
 * ## Features
 * - Native console capture that works on an untethered device (see the module docs for the
 *   exact capture matrix per platform)
 * - Bounded, drop-oldest history (default 1000 entries) so a logcat firehose can't OOM the
 *   host app
 * - Per-[com.worldline.devview.consolelogger.model.LogLevel] filter chips and a text filter
 * - Optional [DevViewLogWriter] for routing Kermit-based logging into the same view
 *
 * ## Integration
 * ```kotlin
 * val modules = rememberModules {
 *     module(Console())
 * }
 * ```
 *
 * @property maxEntries Maximum number of console log entries retained in memory. Oldest
 *           entries are evicted first.
 * @property captureNativeConsole Whether to automatically tail the platform's native console
 *           output. Set to `false` to only receive entries logged manually via
 *           [ConsoleLogger.log] or [DevViewLogWriter].
 *
 * @see Module
 * @see Section.LOGGING
 * @see ConsoleLogger
 * @see ConsoleScreen
 */
public class Console(
    public val maxEntries: Int = ConsoleLogger.DEFAULT_MAX_ENTRIES,
    public val captureNativeConsole: Boolean = true
) : Module {
    override val section: Section
        get() = Section.LOGGING

    override val icon: ImageVector
        get() = Icons.Rounded.Terminal

    override val destinations: PersistentMap<KClass<out NavKey>, DestinationMetadata> = persistentMapOf(
        ConsoleDestination.Main.withTitle(title = "Console") {
            action(
                label = "Clear logs",
                icon = Icons.Rounded.Delete,
                popup = ModuleDestinationActionPopup(
                    title = "Clear Logs",
                    subtitle = "Remove all captured console log lines",
                    confirmButton = "Clear",
                    dismissButton = "Cancel"
                )
            ) {
                ConsoleLogger.clear()
            }
        }
    )

    override val entryDestination: NavKey = ConsoleDestination.Main

    override val registerSerializers: PolymorphicModuleBuilder<NavKey>.() -> Unit
        get() = {
            subclass(
                subclass = ConsoleDestination.Main::class,
                serializer = ConsoleDestination.Main.serializer()
            )
        }

    override fun EntryProviderScope<NavKey>.registerContent(
        onNavigateBack: () -> Unit,
        onNavigate: (NavKey) -> Unit,
        bottomPadding: Dp
    ) {
        entry<ConsoleDestination.Main> {
            ConsoleScreen(
                modifier = Modifier.fillMaxSize(),
                bottomPadding = bottomPadding
            )
        }
    }

    @Composable
    override fun initModule() {
        ConsoleLogger.configure(maxEntries = maxEntries)

        LaunchedEffect(key1 = Unit) {
            launch { ConsoleLogger.collectInto() }
            if (captureNativeConsole) {
                launch {
                    // Isolate native capture failures from the log-drain job above: a
                    // crash here (e.g. an OEM ROM blocking logcat) must not take down the
                    // whole console. Cancellation itself must still propagate normally.
                    @Suppress("TooGenericExceptionCaught")
                    try {
                        tailNativeConsole()
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (throwable: Throwable) {
                        Logger.w(throwable = throwable, tag = "DevViewConsole") {
                            "Native console capture stopped unexpectedly"
                        }
                    }
                }
            }
        }
    }
}
