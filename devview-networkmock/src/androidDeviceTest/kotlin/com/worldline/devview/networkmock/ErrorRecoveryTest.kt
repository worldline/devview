package com.worldline.devview.networkmock

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import com.worldline.devview.networkmock.core.NetworkMockResourceLoader
import com.worldline.devview.networkmock.core.model.OperationKey
import com.worldline.devview.networkmock.core.repository.MockConfigRepository
import com.worldline.devview.networkmock.core.repository.MockStateRepository
import com.worldline.devview.networkmock.viewmodel.NetworkMockMutationCoordinator
import com.worldline.devview.networkmock.viewmodel.OperationDetailViewModel
import com.worldline.devview.networkmock.viewmodel.OperationSheetState
import com.worldline.devview.test.FakePreferencesDataStore
import kotlin.test.Test
import kotlin.test.assertTrue

class ErrorRecoveryTest {
    private val longMessage = "The specification could not be read from its configured resource. ".repeat(40)

    @Test
    fun long_detail_and_sequence_errors_keep_reload_visible_in_a_short_viewport() = runComposeUiTest {
        var reads = 0
        val config = MockConfigRepository(
            specPaths = listOf("missing.json"),
            resourceLoader = NetworkMockResourceLoader { reads++; error(longMessage) }
        )
        val showSequence = mutableStateOf(value = false)
        setContent {
            val scope = rememberCoroutineScope()
            val state = remember { MockStateRepository(FakePreferencesDataStore()) }
            val mutations = remember { NetworkMockMutationCoordinator(state, scope) }
            val key = OperationKey("test", "getUser")
            val vm = remember { OperationDetailViewModel(key, config, state, SavedStateHandle(), mutations) }
            MaterialTheme {
                Box(modifier = Modifier.size(width = 280.dp, height = 240.dp)) {
                    if (showSequence.value) {
                        SequenceBuilderScreen(viewModel = vm, onBack = {})
                    } else {
                        ResponseDetailScreen(NetworkMockDestination.ResponseDetail(key, 200, "application/json", "default"), vm, onBack = {})
                    }
                }
            }
        }
        onNodeWithText("Reload specification").assertIsDisplayed().performClick()
        waitUntil { reads >= 2 }
        runOnIdle { showSequence.value = true }
        onNodeWithText("Reload specification").assertIsDisplayed().performClick()
        waitUntil { reads >= 3 }
    }

    @Test
    fun long_sheet_errors_keep_retry_and_close_visible_without_scrolling_the_buttons() = runComposeUiTest {
        var reloads = 0
        var closed = false
        setContent {
            val scope = rememberCoroutineScope()
            val mutations = remember { NetworkMockMutationCoordinator(MockStateRepository(FakePreferencesDataStore()), scope) }
            val dispatcher = rememberNavigationEventDispatcherOwner()
            CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides dispatcher) {
                MaterialTheme {
                    NetworkMockOperationSheet(
                        sheetState = OperationSheetState.Error(longMessage),
                        mutations = mutations,
                        selection = null,
                        onSelectionComplete = {},
                        onTransitionChange = {},
                        canCompleteSelection = { true },
                        onDismissRequest = { closed = true },
                        onSelectResponse = {},
                        onSelectFailure = {},
                        onOpenResponse = {},
                        onBuildSequence = {},
                        onResetSequencePosition = {},
                        onReload = { reloads++ },
                        modifier = Modifier.height(320.dp)
                    )
                }
            }
        }
        onNodeWithText("Retry loading").assertIsDisplayed().performClick()
        assertTrue(reloads == 1)
        onNodeWithText("Close").assertIsDisplayed().performClick()
        waitUntil { closed }
    }
}
