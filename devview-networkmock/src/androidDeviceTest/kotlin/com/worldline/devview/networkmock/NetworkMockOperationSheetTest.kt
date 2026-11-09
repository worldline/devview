package com.worldline.devview.networkmock

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import com.worldline.devview.networkmock.core.model.FailureKind
import com.worldline.devview.networkmock.core.model.HttpMethod
import com.worldline.devview.networkmock.core.model.MockResponse
import com.worldline.devview.networkmock.viewmodel.asState
import com.worldline.devview.networkmock.core.model.Operation
import com.worldline.devview.networkmock.core.model.OperationDescriptor
import com.worldline.devview.networkmock.core.model.OperationKey
import com.worldline.devview.networkmock.core.model.OperationMockState
import com.worldline.devview.networkmock.core.repository.MockStateRepository
import com.worldline.devview.networkmock.model.OperationUiModel
import com.worldline.devview.networkmock.viewmodel.MutationOrigin
import com.worldline.devview.networkmock.viewmodel.MutationTicket
import com.worldline.devview.networkmock.viewmodel.NetworkMockMutation
import com.worldline.devview.networkmock.viewmodel.NetworkMockMutationCoordinator
import com.worldline.devview.networkmock.viewmodel.OperationSheetState
import com.worldline.devview.test.FakePreferencesDataStore
import io.kotest.matchers.shouldBe
import java.io.IOException
import kotlin.test.Test
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CompletableDeferred

class NetworkMockOperationSheetTest {
    private val key = OperationKey(specId = "test", operationId = "getUser")
    private val response200 = MockResponse(200, "application/json", "default", "200 - default", "{}")
    private val response404 = response200.copy(statusCode = 404, displayName = "404 - default")

    @Test
    fun pickerShowsRadioSelectionAndStatusFamiliesWithoutTabs() = runComposeUiTest {
        setPicker()
        onNodeWithTag("operation_sheet_network_item").assertIsSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        onNodeWithTag("mock_item_200_application/json_default")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        onNodeWithTag("operation_picker_list").performScrollToNode(androidx.compose.ui.test.hasText("Successful mocks"))
        onNodeWithText("Successful mocks").assertIsDisplayed()
        onNodeWithTag("operation_picker_list").performScrollToNode(androidx.compose.ui.test.hasText("Client Error mocks"))
        onNodeWithText("Client Error mocks").assertIsDisplayed()
        onNodeWithTag("operation_sheet_tab_responses").assertDoesNotExist()
    }

    @Test
    fun networkAndMockRowsInvokeExactSelection() = runComposeUiTest {
        var selected: MockResponse? = response200
        setPicker(current = response200.asState(), onSelect = { selected = it })
        onNodeWithTag("operation_sheet_network_item").performClick()
        selected shouldBe null
        scrollTo("mock_item_404_application/json_default")
        onNodeWithTag("mock_item_404_application/json_default").performClick()
        selected shouldBe response404
    }

    @Test
    fun responseChevronInspectsWithoutSelecting() = runComposeUiTest {
        var preview: MockResponse? = null
        var selected: MockResponse? = null
        setPicker(onSelect = { selected = it }, onPreview = { preview = it })
        onNodeWithTag("mock_item_200_application/json_default_preview_toggle").performClick()
        preview shouldBe response200
        selected shouldBe null
    }

    @Test
    fun sheetShowsHeaderAndCurrentStateAndCloseWorks() = runComposeUiTest {
        var closed = false
        setSheet(current = response200.asState(), onClose = { closed = true })
        onNodeWithText("Get User").assertIsDisplayed()
        onNodeWithTag("operation_sheet_current_state").assertIsDisplayed()
        onNodeWithTag("operation_sheet_close_button").performClick()
        waitUntil(timeoutMillis = 5_000) { closed }
    }

    @Test
    fun chevronClosesBeforeNavigatingAndNavigatesOnce() = runComposeUiTest {
        val events = mutableListOf<String>()
        setSheet(onClose = { events += "closed" }, onPreview = { events += "detail" })
        onNodeWithTag("mock_item_200_application/json_default_preview_toggle").performClick()
        waitUntil(timeoutMillis = 5_000) { events.size == 2 }
        events shouldBe listOf("closed", "detail")
    }

    @Test
    fun hidingSheetBlocksNavigationUntilTheTransitionFinishes() = runComposeUiTest {
        var transitioning = false
        val events = mutableListOf<String>()
        setSheet(
            onClose = { events += "closed" },
            onPreview = { events += "detail" },
            onTransition = { transitioning = it }
        )
        mainClock.autoAdvance = false
        onNodeWithTag("mock_item_200_application/json_default_preview_toggle").performClick()
        runOnIdle { transitioning shouldBe true }
        events shouldBe emptyList()
        mainClock.advanceTimeBy(1_000)
        mainClock.autoAdvance = true
        waitUntil(timeoutMillis = 5_000) { events.size == 2 }
        transitioning shouldBe false
        events shouldBe listOf("closed", "detail")
    }

    @Test
    fun buildSequenceClosesBeforeNavigating() = runComposeUiTest {
        val events = mutableListOf<String>()
        setSheet(onClose = { events += "closed" }, onBuild = { events += "sequence" })
        scrollTo("build_sequence")
        onNodeWithTag("build_sequence").performClick()
        waitUntil(timeoutMillis = 5_000) { events.size == 2 }
        events shouldBe listOf("closed", "sequence")
    }

    @Test
    fun selectionClosesOnlyAfterSuccessfulPersistence() = runComposeUiTest {
        val store = ControlledDataStore(block = CompletableDeferred())
        var closed = false
        setSheet(store = store, onClose = { closed = true })
        onNodeWithTag("mock_item_200_application/json_default").performClick()
        onNodeWithTag("operation_sheet_close_button").assertIsNotEnabled()
        closed shouldBe false
        store.block!!.complete(Unit)
        waitUntil(timeoutMillis = 5_000) { closed }
    }

    @Test
    fun failedWriteKeepsSheetAndRetryCompletesSameSelection() = runComposeUiTest {
        val store = ControlledDataStore(fail = true)
        var closed = false
        setSheet(store = store, onClose = { closed = true })
        onNodeWithTag("mock_item_200_application/json_default").performClick()
        onNodeWithText("Mock setting was not saved").assertIsDisplayed()
        onNodeWithTag("operation_sheet_close_button").assertIsNotEnabled()
        closed shouldBe false
        store.fail = false
        onNodeWithTag("mutation_retry").performClick()
        waitUntil(timeoutMillis = 5_000) { closed }
    }

    @Test
    fun cancellingFailedWriteKeepsSheetAndUnlocksChoices() = runComposeUiTest {
        var closed = false
        setSheet(store = ControlledDataStore(fail = true), onClose = { closed = true })
        onNodeWithTag("mock_item_200_application/json_default").performClick()
        onNodeWithTag("mutation_cancel").performClick()
        onNodeWithTag("operation_sheet_network_item").assertIsSelected()
        onNodeWithTag("operation_sheet_close_button").performClick()
        waitUntil(timeoutMillis = 5_000) { closed }
    }

    @Test
    fun failuresAndConfiguredRateShareThePicker() = runComposeUiTest {
        var selected: FailureKind? = null
        setPicker(failureRate = 0.25, onFailure = { selected = it })
        scrollTo("failure_item_TIMEOUT")
        onNodeWithTag("failure_item_TIMEOUT").performClick()
        selected shouldBe FailureKind.TIMEOUT
        onNodeWithText("The spec also applies a 25% random failure rate to mock responses.").assertIsDisplayed()
    }

    @Test
    fun activeSequenceOffersEditAndResetInThePicker() = runComposeUiTest {
        var reset = false
        setPicker(
            current = OperationMockState.Sequence(listOf(response200.asState(), response404.asState()), currentIndex = 1),
            onReset = { reset = true }
        )
        scrollTo("reset_sequence_position")
        onNodeWithText("Edit sequence").assertIsDisplayed()
        onNodeWithTag("reset_sequence_position").performClick()
        reset shouldBe true
    }

    @Test
    fun mediaTypesDisambiguateOtherwiseIdenticalVariants() = runComposeUiTest {
        setPicker(responses = listOf(response200, response200.copy(mediaType = "application/xml")))
        onNodeWithText("application/json").assertIsDisplayed()
        onNodeWithText("application/xml").assertIsDisplayed()
    }

    private fun content(
        current: OperationMockState,
        responses: List<MockResponse>,
        failureRate: Double?
    ) = OperationSheetState.Content(
        operationUiModel = OperationUiModel(
            descriptor = OperationDescriptor(key, Operation("getUser", "Get User", "/api/users/{userId}", HttpMethod.Get, failureRate = failureRate)),
            currentState = current
        ),
        responses = responses.toPersistentList()
    )

    private fun ComposeUiTest.scrollTo(tag: String) {
        onNodeWithTag("operation_picker_list").performScrollToNode(hasTestTag(tag))
    }

    private fun ComposeUiTest.setPicker(
        current: OperationMockState = OperationMockState.Network,
        responses: List<MockResponse> = listOf(response200, response404),
        failureRate: Double? = null,
        onSelect: (MockResponse?) -> Unit = {},
        onPreview: (MockResponse) -> Unit = {},
        onFailure: (FailureKind) -> Unit = {},
        onReset: () -> Unit = {}
    ) {
        setContent {
            MaterialTheme {
                OperationPickerPage(
                    content = content(current, responses, failureRate), onSelectResponse = onSelect,
                    onPreview = onPreview, onSelectFailure = onFailure, onResetSequencePosition = onReset
                )
            }
        }
    }

    private fun ComposeUiTest.setSheet(
        current: OperationMockState = OperationMockState.Network,
        store: DataStore<Preferences> = FakePreferencesDataStore(),
        onClose: () -> Unit = {},
        onPreview: (MockResponse) -> Unit = {},
        onBuild: () -> Unit = {},
        onTransition: (Boolean) -> Unit = {}
    ) {
        setContent {
            val navigationOwner = rememberNavigationEventDispatcherOwner(parent = null)
            val scope = rememberCoroutineScope()
            val mutations = remember { NetworkMockMutationCoordinator(MockStateRepository(store), scope) }
            var selection by remember { mutableStateOf<MutationTicket?>(null) }
            CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides navigationOwner) {
                MaterialTheme {
                    NetworkMockOperationSheet(
                        sheetState = content(current, listOf(response200, response404), null), mutations = mutations,
                        selection = selection, onSelectionComplete = { onClose() }, onTransitionChange = onTransition,
                        canCompleteSelection = { it == selection?.id && mutations.interactionsEnabled },
                        onDismissRequest = onClose,
                        onSelectResponse = { response ->
                            selection = mutations.submit(
                                NetworkMockMutation.Select(key, response?.asState() ?: OperationMockState.Network), MutationOrigin(1, 1)
                            )
                        },
                        onSelectFailure = {}, onOpenResponse = onPreview, onBuildSequence = onBuild,
                        onResetSequencePosition = {}, onReload = {}
                    )
                }
            }
        }
    }

    private class ControlledDataStore(
        var fail: Boolean = false,
        val block: CompletableDeferred<Unit>? = null
    ) : DataStore<Preferences> {
        private val delegate = FakePreferencesDataStore()
        override val data = delegate.data
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            block?.await()
            if (fail) throw IOException("disk error")
            return delegate.updateData(transform)
        }
    }
}
