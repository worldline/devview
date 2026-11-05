package com.worldline.devview.crashhistory

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.worldline.devview.core.DestinationMetadata
import com.worldline.devview.core.Module
import com.worldline.devview.core.ModuleDestinationActionPopup
import com.worldline.devview.core.Section
import com.worldline.devview.core.withTitle
import com.worldline.devview.crashhistory.core.CrashRecorder
import kotlin.reflect.KClass
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder

/**
 * Navigation destinations for the Crash History module.
 *
 * @see CrashHistory
 */
public sealed interface CrashHistoryDestination : NavKey {
    /** The list of recorded crashes. */
    @Serializable
    public data object Main : CrashHistoryDestination

    /**
     * The details of one recorded crash.
     *
     * @property id The [com.worldline.devview.crashhistory.core.CrashRecord.id] to show.
     */
    @Serializable
    public data class Detail(val id: String) : CrashHistoryDestination
}

/**
 * Crash History module for the DevView developer tools suite.
 *
 * Shows a bounded, persistent list of the app's crashes so testers and developers can read them
 * on the device and pass them on (copy, share or export as JSON). Nothing is uploaded: data only
 * leaves the device when the user chooses to share it.
 *
 * It complements, and does not replace, a crash reporting SDK such as Crashlytics: the previously
 * installed crash handler is always chained. See [CrashRecorder] for exactly what is captured and
 * for the platform limits.
 *
 * ## Integration
 * ```kotlin
 * val modules = rememberModules {
 *     module(CrashHistory())
 * }
 * ```
 * Registering the module starts recording. Records are stored raw (not redacted) in app-private,
 * non-backed-up storage; sharing them is the user's decision.
 *
 * @property maxRecords Positive maximum number of records kept; the oldest are deleted first.
 * Zero is unsupported. The latest initialized module configuration wins.
 * @throws IllegalArgumentException if [maxRecords] is not positive.
 *
 * @see Module
 * @see Section.LOGGING
 * @see CrashRecorder
 */
public class CrashHistory(public val maxRecords: Int = CrashRecorder.DEFAULT_MAX_RECORDS) : Module {
    init {
        require(value = maxRecords > 0) { "maxRecords must be positive" }
    }

    override val section: Section
        get() = Section.LOGGING

    override val icon: ImageVector
        get() = Icons.Rounded.BugReport

    override val destinations: PersistentMap<KClass<out NavKey>, DestinationMetadata> = persistentMapOf(
        CrashHistoryDestination.Main.withTitle(title = "Crash History") {
            action(
                icon = Icons.Rounded.Delete,
                popup = ModuleDestinationActionPopup(
                    title = "Clear Crash History",
                    subtitle = "Delete all recorded crashes from this device",
                    confirmButton = "Clear",
                    dismissButton = "Cancel"
                )
            ) {
                CrashRecorder.clear()
            }
        },
        CrashHistoryDestination.Detail::class.withTitle(title = "Crash Details")
    )

    override val entryDestination: NavKey = CrashHistoryDestination.Main

    override val registerSerializers: PolymorphicModuleBuilder<NavKey>.() -> Unit
        get() = {
            subclass(
                subclass = CrashHistoryDestination.Main::class,
                serializer = CrashHistoryDestination.Main.serializer()
            )
            subclass(
                subclass = CrashHistoryDestination.Detail::class,
                serializer = CrashHistoryDestination.Detail.serializer()
            )
        }

    override fun EntryProviderScope<NavKey>.registerContent(
        onNavigateBack: () -> Unit,
        onNavigate: (NavKey) -> Unit,
        bottomPadding: Dp
    ) {
        entry<CrashHistoryDestination.Main> {
            val records by CrashRecorder.records.collectAsState()
            CrashListScreen(
                modifier = Modifier,
                records = records,
                onOpen = { id -> onNavigate(CrashHistoryDestination.Detail(id = id)) },
                bottomPadding = bottomPadding
            )
        }
        entry<CrashHistoryDestination.Detail> { key ->
            val records by CrashRecorder.records.collectAsState()
            CrashDetailScreen(
                record = records.firstOrNull { record -> record.id == key.id },
                bottomPadding = bottomPadding
            )
        }
    }

    @Composable
    override fun initModule() {
        CrashRecorder.install(maxRecords = maxRecords)
    }
}
