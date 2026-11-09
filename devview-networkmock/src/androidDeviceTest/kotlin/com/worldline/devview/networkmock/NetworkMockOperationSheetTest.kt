package com.worldline.devview.networkmock

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
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
import kotlin.test.assertTrue
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking

class NetworkMockOperationSheetTest {
    private val key = OperationKey(specId = "test", operationId = "getUser")
    private val response200 = MockResponse(200, "application/json", "default", "200 - default", "{}")
    private val response404 = response200.copy(statusCode = 404, displayName = "404 - default")

    @Test
    fun pickerShowsRadioSelectionAndStatusFamiliesWithBrowsingTabs() = runComposeUiTest {
        setPicker()
        onNodeWithTag("operation_sheet_network_item").assertIsSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        onNodeWithTag("mock_item_200_application/json_default")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        onNodeWithTag("operation_picker_list").performScrollToNode(androidx.compose.ui.test.hasText("Successful mocks"))
        onNodeWithText("Successful mocks").assertIsDisplayed()
        onNodeWithTag("operation_picker_list").performScrollToNode(androidx.compose.ui.test.hasText("Client Error mocks"))
        onNodeWithText("Client Error mocks").assertIsDisplayed()
        onNodeWithTag("operation_responses_tab").assertIsSelected()
        onNodeWithTag("operation_failures_tab").assertIsNotSelected()
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
    fun responseDetailsInspectsWithoutSelecting() = runComposeUiTest {
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
    fun detailsClosesBeforeNavigatingAndNavigatesOnce() = runComposeUiTest {
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
        onNodeWithTag("build_sequence").assertIsDisplayed().performClick()
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
        onNodeWithTag("operation_failures_tab").performClick()
        scrollTo("failure_item_TIMEOUT")
        onNodeWithTag("failure_item_TIMEOUT").performClick()
        selected shouldBe FailureKind.TIMEOUT
        onNodeWithText(
            "The spec applies a 25% random failure rate to mock responses, including sequences. " +
                "It does not affect actual network or explicit failures."
        ).assertIsDisplayed()
    }

    @Test
    fun activeSequenceOffersEditAndResetInThePicker() = runComposeUiTest {
        var reset = false
        setPicker(
            current = OperationMockState.Sequence(listOf(response200.asState(), response404.asState()), currentIndex = 1),
            onReset = { reset = true }
        )
        onNodeWithText("Sequence · next 2/2").assertIsDisplayed()
        onNodeWithText("Last step repeats").assertIsDisplayed()
        onNodeWithTag("mock_item_200_application/json_default").assertIsNotSelected()
        scrollTo("mock_item_404_application/json_default")
        onNodeWithTag("mock_item_404_application/json_default").assertIsNotSelected()
        onNodeWithTag("build_sequence").assertIsDisplayed()
        onNodeWithTag("reset_sequence_position").performClick()
        reset shouldBe true
    }

    @Test
    fun mediaTypesDisambiguateOtherwiseIdenticalVariants() = runComposeUiTest {
        setPicker(responses = listOf(response200, response200.copy(mediaType = "application/xml")))
        onNodeWithText("application/json").assertIsDisplayed()
        onNodeWithText("application/xml").assertIsDisplayed()
    }

    @Test
    fun tabsAndSearchDoNotSelectAndSequenceActionsStayVisibleWithManyResponses() = runComposeUiTest {
        var selections = 0
        var previews = 0
        val manyResponses = List(100) { response200.copy(exampleName = "variant_$it", displayName = "Variant $it") }
        setPicker(
            current = OperationMockState.Sequence(listOf(response200.asState(), response404.asState())),
            responses = manyResponses,
            onSelect = { selections++ }, onFailure = { selections++ }, onPreview = { previews++ }
        )
        onNodeWithTag("operation_failures_tab").performClick()
        onNodeWithTag("operation_responses_tab").performClick()
        onNodeWithTag("operation_response_search").performTextInput("variant_99")
        onNodeWithTag("mock_item_200_application/json_variant_99").assertIsDisplayed()
        onNodeWithTag("mock_item_200_application/json_variant_0").assertDoesNotExist()
        onNodeWithTag("reset_sequence_position").assertIsDisplayed()
        onNodeWithTag("build_sequence").assertIsDisplayed()
        onNodeWithTag("operation_sheet_network_item").assertIsDisplayed()
        selections shouldBe 0
        previews shouldBe 0
    }

    @Test
    fun searchHasRecoveryWithoutChangingConfiguredFailure() = runComposeUiTest {
        setPicker(current = OperationMockState.Failure(FailureKind.CONNECTION_REFUSED))
        onNodeWithText("Simulated failure").assertIsDisplayed()
        onNodeWithTag("operation_response_search").performTextInput("no_such_response")
        onNodeWithText("No matching responses").assertIsDisplayed()
        onNodeWithText("Clear search").performClick()
        onNodeWithTag("mock_item_200_application/json_default").assertIsDisplayed()
        onNodeWithTag("operation_failures_tab").performClick()
        onNodeWithTag("failure_item_CONNECTION_REFUSED").assertIsSelected()
        onNodeWithTag("failure_item_TIMEOUT").assertIsNotSelected()
    }

    @Test
    fun noResponsesStillOffersActualNetworkAndFailures() = runComposeUiTest {
        setPicker(responses = emptyList())
        onNodeWithText("No responses in this specification").assertIsDisplayed()
        onNodeWithTag("operation_sheet_network_item").assertIsDisplayed()
        onNodeWithTag("operation_failures_tab").performClick()
        onNodeWithTag("failure_item_TIMEOUT").assertIsDisplayed()
    }

    @Test
    fun compactViewportKeepsControlsAndChoicesReachable() = runComposeUiTest {
        assertCompactPickerReachable(height = 460.dp)
    }

    @Test
    fun measuredControlsTriggerScrollingAboveTheOldBreakpoint() = runComposeUiTest {
        assertCompactPickerReachable(height = 500.dp)
    }

    @Test
    fun constrainedSheetKeepsItsHeaderAndResponseChoicesReachable() = runComposeUiTest {
        setSheet(
            current = OperationMockState.Sequence(listOf(response200.asState(), response404.asState()), currentIndex = 1),
            modifier = Modifier.height(580.dp)
        )
        onNodeWithTag("operation_sheet_close_button").assertIsDisplayed()
        onNodeWithTag("operation_picker_list").performScrollTo()
        scrollTo("mock_item_404_application/json_default")
        onNodeWithTag("mock_item_404_application/json_default").assertIsDisplayed()
        onNodeWithTag("build_sequence").performScrollTo().assertIsDisplayed()
        onNodeWithTag("reset_sequence_position").assertIsDisplayed()
    }

    @Test
    fun focusedSearchShowsMatchesWhenTheViewportShrinksWithoutManualScrolling() = runComposeUiTest {
        var height by mutableStateOf(650.dp)
        var selections = 0
        var previews = 0
        val sequence = OperationMockState.Sequence(listOf(response200.asState(), response404.asState()))
        setContent {
            MaterialTheme {
                Box(modifier = Modifier.height(height).width(400.dp)) {
                    OperationPickerPage(
                        content = content(sequence, listOf(response200, response404), null),
                        onSelectResponse = { selections++ },
                        onPreview = { previews++ }
                    )
                }
            }
        }
        onNodeWithTag("operation_response_search").performClick().performTextInput("404")
        runOnIdle { height = 340.dp }
        onNodeWithTag("operation_response_search").assertIsDisplayed()
        onNodeWithTag("mock_item_404_application/json_default").assertIsDisplayed()
        onNodeWithTag("mock_item_404_application/json_default_preview_toggle").assertIsDisplayed()
        selections shouldBe 0
        previews shouldBe 0
    }

    @Test
    fun focusedSearchIncludesTheWrappedFailureRateNoteBeforeItsMatches() = runComposeUiTest {
        var height by mutableStateOf(700.dp)
        var selections = 0
        var previews = 0
        val sequence = OperationMockState.Sequence(listOf(response200.asState(), response404.asState()))
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                MaterialTheme {
                    Box(modifier = Modifier.height(height).width(400.dp)) {
                        OperationPickerPage(
                            content = content(sequence, listOf(response200, response404), 0.25),
                            onSelectResponse = { selections++ },
                            onPreview = { previews++ }
                        )
                    }
                }
            }
        }
        onNodeWithTag("operation_response_search").performScrollTo().performClick().performTextInput("404")
        runOnIdle { height = 420.dp }
        onNodeWithTag("mock_item_404_application/json_default").assertIsDisplayed()
        onNodeWithTag("mock_item_404_application/json_default_preview_toggle").assertIsDisplayed()
        selections shouldBe 0
        previews shouldBe 0
    }

    @Test
    fun sequenceSummaryUsesThreeCompactRowsAtNormalWidth() = runComposeUiTest {
        setPicker(
            current = OperationMockState.Sequence(listOf(response200.asState(), response404.asState()), currentIndex = 1),
            width = 400.dp
        )
        val summary = onNodeWithTag("operation_sheet_current_state").getUnclippedBoundsInRoot()
        assertTrue(summary.bottom - summary.top <= 120.dp, "Sequence summary must not crowd out the response catalogue: $summary")
        val heading = onNodeWithText("Sequence · next 2/2").getUnclippedBoundsInRoot()
        val edit = onNodeWithTag("build_sequence").getUnclippedBoundsInRoot()
        val restart = onNodeWithTag("reset_sequence_position").getUnclippedBoundsInRoot()
        assertTrue((heading.top + heading.bottom) / 2 in edit.top..edit.bottom, "Heading and actions must share a row")
        assertTrue(edit.bottom - edit.top >= 48.dp && restart.bottom - restart.top >= 48.dp, "Keep sequence action touch targets")
        onNodeWithTag("mock_item_200_application/json_default").assertIsDisplayed()
    }

    @Test
    fun narrowSequenceSummaryWrapsItsActionsAndLongResponseName() = runComposeUiTest {
        val xml = response404.copy(
            mediaType = "application/xml",
            exampleName = "A detailed unavailable user response with a long descriptive name"
        )
        setPicker(
            current = OperationMockState.Sequence(listOf(response200.asState(), xml.asState()), currentIndex = 1),
            responses = listOf(response200, xml),
            width = 280.dp
        )
        val summary = onNodeWithTag("operation_sheet_current_state").getUnclippedBoundsInRoot()
        val name = onNode(
            androidx.compose.ui.test.hasText(xml.asState().displayName) and
                androidx.compose.ui.test.hasAnyAncestor(hasTestTag("operation_sheet_current_state")),
            useUnmergedTree = true
        ).getUnclippedBoundsInRoot()
        assertTrue(name.left >= summary.left && name.right <= summary.right, "Response name must stay in its summary")
        assertTrue(name.bottom - name.top > 20.dp, "Long response names must wrap rather than truncate")
        val heading = onNodeWithText("Sequence · next 2/2").getUnclippedBoundsInRoot()
        val edit = onNodeWithTag("build_sequence").getUnclippedBoundsInRoot()
        assertTrue(edit.top >= heading.bottom, "Sequence actions must wrap when they cannot share the heading row")
        onNodeWithTag("build_sequence").assertIsDisplayed()
        onNodeWithTag("reset_sequence_position").assertIsDisplayed()
        onNodeWithTag("operation_sheet_current_state").assert(
            androidx.compose.ui.test.hasAnyDescendant(androidx.compose.ui.test.hasText("application/xml"))
        )
    }

    @Test
    fun emptySequenceDoesNotAdvertiseANonexistentNextStep() = runComposeUiTest {
        setPicker(current = OperationMockState.Sequence(emptyList()))
        onNodeWithText("Sequence · unavailable").assertIsDisplayed()
        onNodeWithText("No available next response. Edit this sequence.").assertIsDisplayed()
        onNodeWithTag("build_sequence").assertIsDisplayed()
    }

    @Test
    fun outOfRangeSequenceDoesNotAdvertiseANonexistentNextStep() = runComposeUiTest {
        setPicker(current = OperationMockState.Sequence(listOf(response200.asState()), currentIndex = 2))
        onNodeWithText("Sequence · unavailable").assertIsDisplayed()
        onNodeWithText("No available next response. Edit this sequence.").assertIsDisplayed()
    }

    @Test
    fun narrowConfiguredFailureKeepsItsNameReadable() = runComposeUiTest {
        setPicker(current = OperationMockState.Failure(FailureKind.CONNECTION_REFUSED), width = 240.dp)
        val summary = onNodeWithTag("operation_sheet_current_state").getUnclippedBoundsInRoot()
        val name = onNode(
            androidx.compose.ui.test.hasText(OperationMockState.Failure(FailureKind.CONNECTION_REFUSED).displayName) and
                androidx.compose.ui.test.hasAnyAncestor(hasTestTag("operation_sheet_current_state")),
            useUnmergedTree = true
        ).getUnclippedBoundsInRoot()
        assertTrue(name.left >= summary.left && name.right <= summary.right)
        onNodeWithText("Failure").assertIsDisplayed()
    }

    @Test
    fun configuredSequenceDisambiguatesItsNextMediaType() = runComposeUiTest {
        val xml = response200.copy(mediaType = "application/xml")
        setPicker(
            current = OperationMockState.Sequence(listOf(response200.asState(), xml.asState()), currentIndex = 1),
            responses = listOf(response200, xml)
        )
        onNodeWithTag("operation_sheet_current_state").assert(
            androidx.compose.ui.test.hasAnyDescendant(androidx.compose.ui.test.hasText("application/xml"))
        )
    }

    @Test
    fun configuredSingleResponseDisambiguatesItsMediaType() = runComposeUiTest {
        val xml = response200.copy(mediaType = "application/xml")
        setPicker(current = xml.asState(), responses = listOf(response200, xml))
        onNodeWithTag("operation_sheet_current_state").assert(
            androidx.compose.ui.test.hasAnyDescendant(androidx.compose.ui.test.hasText("application/xml"))
        )
    }

    @Test
    fun restartClosesOnlyAfterPersistingTheFirstStep() = runComposeUiTest {
        val sequence = OperationMockState.Sequence(listOf(response200.asState(), response404.asState()), currentIndex = 1)
        val store = ControlledDataStore()
        val repository = MockStateRepository(store)
        runBlocking { repository.setOperationMockState(key, sequence) }
        store.block = CompletableDeferred()
        var closed = false
        setSheet(current = sequence, store = store, onClose = { closed = true })
        onNodeWithTag("reset_sequence_position").performClick()
        onNodeWithTag("operation_sheet_close_button").assertIsNotEnabled()
        closed shouldBe false
        runBlocking { repository.getState().getOperationState(key) } shouldBe sequence
        store.block!!.complete(Unit)
        waitUntil(timeoutMillis = 5_000) { closed }
        runBlocking { repository.getState().getOperationState(key) } shouldBe sequence.copy(currentIndex = 0)
    }

    @Test
    fun failedRestartKeepsItsPositionUntilRetrySucceeds() = runComposeUiTest {
        val sequence = OperationMockState.Sequence(listOf(response200.asState(), response404.asState()), currentIndex = 1)
        val store = ControlledDataStore()
        val repository = MockStateRepository(store)
        runBlocking { repository.setOperationMockState(key, sequence) }
        store.fail = true
        var closed = false
        setSheet(current = sequence, store = store, onClose = { closed = true })
        onNodeWithTag("reset_sequence_position").performClick()
        onNodeWithText("Mock setting was not saved").assertIsDisplayed()
        closed shouldBe false
        runBlocking { repository.getState().getOperationState(key) } shouldBe sequence
        store.fail = false
        onNodeWithTag("mutation_retry").performClick()
        waitUntil(timeoutMillis = 5_000) { closed }
        runBlocking { repository.getState().getOperationState(key) } shouldBe sequence.copy(currentIndex = 0)
    }

    private fun ComposeUiTest.assertCompactPickerReachable(height: Dp) {
        setPicker(
            current = OperationMockState.Sequence(listOf(response200.asState(), response404.asState()), currentIndex = 1),
            failureRate = 0.25,
            height = height,
            fontScale = 1.5f
        )
        onNodeWithTag("operation_response_search").performScrollTo().assertIsDisplayed().performTextInput("404")
        onNodeWithTag("operation_picker_list").performScrollTo().assertIsDisplayed()
        scrollTo("mock_item_404_application/json_default")
        onNodeWithTag("mock_item_404_application/json_default").assertIsDisplayed()
        onNodeWithTag("build_sequence").performScrollTo().assertIsDisplayed()
        onNodeWithTag("reset_sequence_position").assertIsDisplayed()
        onNodeWithTag("operation_failures_tab").performScrollTo().performClick()
        onNodeWithTag("operation_picker_list").performScrollTo()
        scrollTo("failure_item_CONNECTION_REFUSED")
        onNodeWithTag("failure_item_CONNECTION_REFUSED").assertIsDisplayed()
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
        onReset: () -> Unit = {},
        height: Dp = Dp.Unspecified,
        width: Dp = Dp.Unspecified,
        fontScale: Float = 1f
    ) {
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                MaterialTheme {
                    Box(
                        modifier = (if (height == Dp.Unspecified) Modifier else Modifier.height(height))
                            .then(if (width == Dp.Unspecified) Modifier else Modifier.width(width))
                    ) {
                        OperationPickerPage(
                            content = content(current, responses, failureRate), onSelectResponse = onSelect,
                            onPreview = onPreview, onSelectFailure = onFailure, onResetSequencePosition = onReset
                        )
                    }
                }
            }
        }
    }

    private fun ComposeUiTest.setSheet(
        current: OperationMockState = OperationMockState.Network,
        store: DataStore<Preferences> = FakePreferencesDataStore(),
        onClose: () -> Unit = {},
        onPreview: (MockResponse) -> Unit = {},
        onBuild: () -> Unit = {},
        onTransition: (Boolean) -> Unit = {},
        modifier: Modifier = Modifier
    ) {
        setContent {
            val navigationOwner = rememberNavigationEventDispatcherOwner(parent = null)
            val scope = rememberCoroutineScope()
            val mutations = remember { NetworkMockMutationCoordinator(MockStateRepository(store), scope) }
            var selection by remember { mutableStateOf<MutationTicket?>(null) }
            CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides navigationOwner) {
                MaterialTheme {
                    NetworkMockOperationSheet(
                        modifier = modifier,
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
                        onResetSequencePosition = {
                            selection = mutations.submit(NetworkMockMutation.ResetSequence(key), MutationOrigin(1, 1))
                        }, onReload = {}
                    )
                }
            }
        }
    }

    private class ControlledDataStore(
        var fail: Boolean = false,
        var block: CompletableDeferred<Unit>? = null
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
