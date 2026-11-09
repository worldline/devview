package com.worldline.devview.consolelogger

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import com.worldline.devview.core.withActions
import com.worldline.devview.utils.DataStoreDelegate
import com.worldline.devview.utils.RequiresDataStore
import kotlin.reflect.KClass
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import okio.IOException

/**
 * Navigation destinations for the Console module.
 *
 * @see Console
 */
public sealed interface ConsoleDestination : NavKey {
    /** Console log destination, rendered by the registered module. */
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
 * - Noise exclusions applied before buffering, with device-persisted rules and enabled states
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
 * @param defaultExcludedPatterns Host defaults matched against tags or messages. Append to
 *        [DEFAULT_EXCLUDED_PATTERNS], replace the list, or pass an empty list. Blank patterns
 *        are ignored and duplicates are merged without regard to case. Device edits are
 *        merged with current defaults after app updates; Reset to defaults clears those edits.
 *
 * @see Module
 * @see Section.LOGGING
 * @see ConsoleLogger
 */
public class Console(
    public val maxEntries: Int = ConsoleLogger.DEFAULT_MAX_ENTRIES,
    public val captureNativeConsole: Boolean = true,
    defaultExcludedPatterns: List<String> = DEFAULT_EXCLUDED_PATTERNS
) : Module,
    RequiresDataStore {
    /** Normalized defaults for incoming noise admission; device edits apply to future entries. */
    public val defaultExcludedPatterns: List<String> = normalizeConsolePatterns(
        patterns = defaultExcludedPatterns
    )

    /** Preferences file containing device-specific noise filter edits, not console logs. */
    override val dataStoreName: String = "console_datastore.preferences_pb"

    /** Shared persistence initialized by `rememberModules` before the module starts. */
    override val dataStoreDelegate: DataStoreDelegate
        get() = sharedDataStoreDelegate

    /** Name displayed on the home screen and default destination toolbar. */
    override val moduleName: String = "Console"

    /** Summary displayed on the home screen. */
    override val subtitle: String = "App console logs"

    override val section: Section
        get() = Section.LOGGING

    override val icon: ImageVector
        get() = Icons.Rounded.Terminal

    override val destinations: PersistentMap<KClass<out NavKey>, DestinationMetadata> = persistentMapOf(
        ConsoleDestination.Main.withActions {
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
                bottomPadding = bottomPadding,
                defaults = defaultExcludedPatterns,
                dataStore = dataStoreDelegate.get()
            )
        }
    }

    public companion object {
        /** Minimal built-in exclusions. Hosts can append to or replace this list. */
        public val DEFAULT_EXCLUDED_PATTERNS: List<String> = listOf(
            "setRequestedFrameRate",
            "setFrameRateCategory"
        )

        private val sharedDataStoreDelegate = DataStoreDelegate()
    }

    @Composable
    override fun initModule() {
        val dataStore = dataStoreDelegate.get()
        val patterns = remember(key1 = dataStore, key2 = defaultExcludedPatterns) {
            ConsoleNoiseRepository(
                dataStore = dataStore,
                defaults = defaultExcludedPatterns
            ).overrides
                .map { it.enabledPatterns(defaults = defaultExcludedPatterns) }
                .retryWhen { cause, _ ->
                    if (cause is IOException) {
                        delay(timeMillis = 1_000)
                        true
                    } else {
                        false
                    }
                }
        }
        LaunchedEffect(key1 = maxEntries, key2 = captureNativeConsole, key3 = patterns) {
            ConsoleLogger.collectInto(maxEntries = maxEntries, excludedPatterns = patterns) {
                if (captureNativeConsole) {
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
