package com.worldline.devview.sample.onetimeflags

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.worldline.devview.core.DestinationMetadata
import com.worldline.devview.core.Module
import com.worldline.devview.core.ModuleDestinationActionPopup
import com.worldline.devview.core.Section
import com.worldline.devview.core.withTitle
import kotlin.reflect.KClass
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import okio.IOException

/** Navigation key for the app's one-time flags. */
@Serializable
public data object OneTimeFlagsScreen : NavKey

/** A developer window onto the same preferences the app reads. */
public class OneTimeFlags(private val dataStore: DataStore<Preferences>) : Module {
    private lateinit var scope: CoroutineScope
    private var resetError by mutableStateOf<String?>(value = null)

    override val moduleName: String = "One-time flags"
    override val subtitle: String = "Reset onboarding, prompts and tips"
    override val section: Section = Section.CUSTOM
    override val entryDestination: NavKey = OneTimeFlagsScreen
    override val destinations: PersistentMap<KClass<out NavKey>, DestinationMetadata> = persistentMapOf(
        OneTimeFlagsScreen.withTitle(title = "One-time flags") {
            action(
                label = "Reset all",
                icon = Icons.Default.Refresh,
                popup = ModuleDestinationActionPopup(
                    title = "Reset all one-time flags?",
                    subtitle = "Other app preferences will not be changed.",
                    confirmButton = "Reset all",
                    dismissButton = "Cancel"
                )
            ) {
                scope.launch {
                    try {
                        resetAll()
                        resetError = null
                    } catch (_: IOException) {
                        resetError = "Could not reset flags. Try again."
                    }
                }
            }
        }
    )

    override val registerSerializers: PolymorphicModuleBuilder<NavKey>.() -> Unit = {
        subclass(subclass = OneTimeFlagsScreen::class, serializer = OneTimeFlagsScreen.serializer())
    }

    @Composable
    override fun initModule() {
        val compositionScope = rememberCoroutineScope()
        SideEffect { scope = compositionScope }
    }

    /** Removes only declared one-time keys in one transaction; failures propagate to the caller. */
    public suspend fun resetAll() {
        dataStore.edit { preferences ->
            OneTimeFlag.entries.forEach { flag -> preferences.remove(key = flag.key) }
        }
    }

    override fun EntryProviderScope<NavKey>.registerContent(
        onNavigateBack: () -> Unit,
        onNavigate: (NavKey) -> Unit,
        bottomPadding: Dp
    ) {
        entry<OneTimeFlagsScreen> {
            OneTimeFlagsContent(
                dataStore = dataStore,
                bottomPadding = bottomPadding,
                resetError = resetError,
                onResetError = { error -> resetError = error }
            )
        }
    }
}
