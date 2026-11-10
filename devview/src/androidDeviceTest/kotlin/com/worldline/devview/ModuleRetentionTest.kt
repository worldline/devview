package com.worldline.devview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runEmptyComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.test.core.app.ActivityScenario
import com.worldline.devview.core.DestinationMetadata
import com.worldline.devview.core.Module
import com.worldline.devview.core.Section
import com.worldline.devview.core.rememberModules
import kotlin.reflect.KClass
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import org.junit.Test

class ModuleRetentionTest {

    @Test
    fun configurationChange_retains_registered_session_and_entry_draft() = withActivity { scenario ->
        onNodeWithTag(testTag = "module_item_Retention module").performClick()
        onNodeWithText(text = "Add draft step").performClick()
        lateinit var originalActivity: ModuleRetentionTestActivity
        lateinit var originalSession: RetentionSession
        lateinit var originalEntry: RetentionEntry
        scenario.onActivity {
            originalActivity = it
            originalSession = it.session
            originalEntry = it.entry
        }

        scenario.recreate()
        onNodeWithText(text = "Draft step").assertIsDisplayed()
        scenario.onActivity {
            assertNotSame(originalActivity, it)
            assertSame(originalSession, it.session)
            assertSame(originalEntry, it.entry)
            assertFalse(originalSession.cleared)
            assertFalse(originalEntry.cleared)
        }
    }

    @Test
    fun hiding_overlay_retains_entry_and_draft() = withActivity { scenario ->
        onNodeWithTag(testTag = "module_item_Retention module").performClick()
        onNodeWithText(text = "Add draft step").performClick()
        lateinit var original: RetentionEntry
        scenario.onActivity {
            original = it.entry
            it.open = false
        }
        onNodeWithText(text = "Draft step").assertDoesNotExist()
        scenario.onActivity { it.open = true }
        onNodeWithText(text = "Draft step").assertIsDisplayed()
        scenario.onActivity {
            assertSame(original, it.entry)
            assertFalse(original.cleared)
        }
    }

    @Test
    fun filtering_active_module_returns_home_and_discards_only_entry() = withActivity { scenario ->
        onNodeWithTag(testTag = "module_item_Retention module").performClick()
        onNodeWithText(text = "Add draft step").performClick()
        lateinit var originalSession: RetentionSession
        lateinit var originalEntry: RetentionEntry
        scenario.onActivity {
            originalSession = it.session
            originalEntry = it.entry
            it.displayed = false
        }
        onNodeWithContentDescription(label = "Back").assertDoesNotExist()
        waitUntil(timeoutMillis = 5_000) { originalEntry.cleared }
        scenario.onActivity {
            assertSame(originalSession, it.session)
            assertFalse(originalSession.cleared)
            it.displayed = true
        }
        onNodeWithTag(testTag = "module_item_Retention module").performClick()
        onNodeWithText(text = "Add draft step").assertIsDisplayed()
        onNodeWithText(text = "Draft step").assertDoesNotExist()
        scenario.onActivity { assertNotSame(originalEntry, it.entry) }
    }

    @Test
    fun removing_registry_ends_session_and_registration_starts_fresh() = withActivity { scenario ->
        lateinit var original: RetentionSession
        scenario.onActivity {
            original = it.session
            it.registered = false
        }
        waitUntil(timeoutMillis = 5_000) { original.cleared }
        scenario.onActivity { it.registered = true }
        onNodeWithTag(testTag = "module_item_Retention module").assertIsDisplayed()
        scenario.onActivity {
            assertNotSame(original, it.session)
            assertFalse(it.session.cleared)
        }
    }

    @Test
    fun navigation_guard_is_checked_when_back_is_pressed() = withActivity { scenario ->
        onNodeWithTag(testTag = "module_item_Retention module").performClick()
        scenario.onActivity { it.session.interactionsEnabled = false }
        onNodeWithContentDescription(label = "Back").performClick()
        onNodeWithText(text = "Add draft step").assertIsDisplayed()
        scenario.onActivity { it.session.interactionsEnabled = true }
        onNodeWithContentDescription(label = "Back").performClick()
        onNodeWithTag(testTag = "module_item_Retention module").assertIsDisplayed()
    }

    @Test
    fun outgoing_entry_cannot_push_a_destination_after_back() = withActivity { scenario ->
        onNodeWithTag(testTag = "module_item_Retention module").performClick()
        onNodeWithText(text = "Prepare detail navigation").performClick()
        scenario.onActivity { assertTrue(it.session.pendingNavigation != null) }
        onNodeWithContentDescription(label = "Back").performClick()
        scenario.onActivity { it.session.pendingNavigation?.invoke() }
        onNodeWithTag(testTag = "module_item_Retention module").assertIsDisplayed()
        onNodeWithText(text = "Detail destination").assertDoesNotExist()
    }

    private fun withActivity(block: ComposeUiTest.(ActivityScenario<ModuleRetentionTestActivity>) -> Unit) {
        var scenario: ActivityScenario<ModuleRetentionTestActivity>? = null
        try {
            runEmptyComposeUiTest {
                val launched = ActivityScenario.launch(ModuleRetentionTestActivity::class.java)
                scenario = launched
                waitForIdle()
                block(launched)
            }
        } finally {
            scenario?.close()
        }
    }
}

class ModuleRetentionTestActivity : ComponentActivity() {
    var open: Boolean by mutableStateOf(value = true)
    var displayed: Boolean by mutableStateOf(value = true)
    var registered: Boolean by mutableStateOf(value = true)
    internal lateinit var session: RetentionSession
    internal lateinit var entry: RetentionEntry

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val modules = if (registered) {
                rememberModules {
                    module(RetentionModule(onSession = { session = it }, onEntry = { entry = it }))
                }
            } else {
                persistentListOf()
            }
            DevView(
                devViewIsOpen = open,
                closeDevView = { open = false },
                modules = if (displayed) modules else persistentListOf()
            )
        }
    }
}

internal class RetentionSession : ViewModel() {
    var interactionsEnabled: Boolean by mutableStateOf(value = true)
    var pendingNavigation: (() -> Unit)? = null
    var cleared: Boolean = false
        private set

    override fun onCleared() {
        cleared = true
    }
}

internal class RetentionEntry : ViewModel() {
    var hasDraft: Boolean by mutableStateOf(value = false)
    var cleared: Boolean = false
        private set

    override fun onCleared() {
        cleared = true
    }
}

@Serializable
private data object RetentionDestination : NavKey

@Serializable
private data object RetentionDetailDestination : NavKey

private class RetentionModule(
    private val onSession: (RetentionSession) -> Unit,
    private val onEntry: (RetentionEntry) -> Unit
) : Module {
    private lateinit var session: RetentionSession
    override val moduleName: String = "Retention module"
    override val section: Section = Section.CUSTOM
    override val destinations: PersistentMap<KClass<out NavKey>, DestinationMetadata> =
        persistentMapOf(
            RetentionDestination::class to DestinationMetadata(),
            RetentionDetailDestination::class to DestinationMetadata()
        )
    override val entryDestination: NavKey = RetentionDestination
    override val registerSerializers: PolymorphicModuleBuilder<NavKey>.() -> Unit = {
        subclass(RetentionDestination::class, RetentionDestination.serializer())
        subclass(RetentionDetailDestination::class, RetentionDetailDestination.serializer())
    }

    @Composable
    override fun initModule() {
        val retained = viewModel { RetentionSession() }
        SideEffect {
            session = retained
            onSession(retained)
        }
    }

    override fun EntryProviderScope<NavKey>.registerContent(
        onNavigateBack: () -> Unit,
        onNavigate: (NavKey) -> Unit,
        bottomPadding: Dp
    ) {
        entry<RetentionDestination>(
            metadata = mapOf(
                "com.worldline.devview.internal.interactionsEnabled" to { session.interactionsEnabled }
            )
        ) {
            val draft = viewModel { RetentionEntry() }
            SideEffect { onEntry(draft) }
            androidx.compose.foundation.layout.Column {
                Button(onClick = { session.pendingNavigation = { onNavigate(RetentionDetailDestination) } }) {
                    Text(text = "Prepare detail navigation")
                }
                if (draft.hasDraft) {
                    Text(text = "Draft step")
                } else {
                    Button(onClick = { draft.hasDraft = true }) {
                        Text(text = "Add draft step")
                    }
                }
            }
        }
        entry<RetentionDetailDestination> { Text(text = "Detail destination") }
    }
}
