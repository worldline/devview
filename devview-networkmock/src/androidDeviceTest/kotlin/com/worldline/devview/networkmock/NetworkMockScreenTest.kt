package com.worldline.devview.networkmock

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.worldline.devview.networkmock.core.model.OperationKey
import com.worldline.devview.networkmock.fixtures.MockScreenTestData
import com.worldline.devview.networkmock.viewmodel.NetworkMockUiState
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class NetworkMockScreenTest {

    @Test
    fun showsLoadingStateUi() = runComposeUiTest {
        setScreen(uiState = NetworkMockUiState.Loading)

        onNodeWithText(text = "Loading mock configuration...")
            .assertIsDisplayed()
    }

    @Test
    fun showsErrorStateUi_withMessage() = runComposeUiTest {
        setScreen(uiState = NetworkMockUiState.Error(message = "boom"))

        onNodeWithText(text = "Error Loading Configuration")
            .assertIsDisplayed()

        onNodeWithText(text = "boom")
            .assertIsDisplayed()
    }


    @Test
    fun showsEmptyStateUi() = runComposeUiTest {
        setScreen(uiState = NetworkMockUiState.Empty)

        onNodeWithText(text = "No Mocks Configured", substring = true)
            .assertIsDisplayed()
    }


    @Test
    fun rendersSpecTabs_forContentState() = runComposeUiTest {
        setScreen(uiState = MockScreenTestData.contentState())

        onNodeWithTag(testTag = "spec_tab_example").assertIsDisplayed()
        onNodeWithTag(testTag = "spec_tab_catalog").assertIsDisplayed()
    }


    @Test
    fun initialSelectedTab_isFirstSpec() = runComposeUiTest {
        setScreen(uiState = MockScreenTestData.contentState())

        onNodeWithTag(testTag = "spec_tab_example").assertIsSelected()

    }

    @Test
    fun tabSwitching_changesVisibleEndpoints() = runComposeUiTest {
        setScreen(uiState = MockScreenTestData.contentState())

        onNodeWithTag(testTag = "endpoint_card_example_getUser").assertIsDisplayed()

        onNodeWithTag(testTag = "spec_tab_catalog").performClick()
        waitForIdle()

        collapseFilters()
        onNodeWithTag(testTag = "endpoint_card_catalog_getUser").assertIsDisplayed()
    }


    @Test
    fun globalToggle_isVisibleInContentState() = runComposeUiTest {
        setScreen(uiState = MockScreenTestData.contentState(globalMockingEnabled = false))

        onNodeWithTag(testTag = "global_mock_toggle_switch").assertIsDisplayed()
    }


    @Test
    fun globalToggle_checkedStateIsOn() = runComposeUiTest {
        setScreen(uiState = MockScreenTestData.contentState(globalMockingEnabled = true))
        onNodeWithTag(testTag = "global_mock_toggle_switch").assertIsOn()

    }

    @Test
    fun globalToggle_checkedStateIsOff() = runComposeUiTest {
        setScreen(uiState = MockScreenTestData.contentState(globalMockingEnabled = false))
        onNodeWithTag(testTag = "global_mock_toggle_switch").assertIsOff()
    }


    @Test
    fun globalToggle_showsMockedCountAcrossAllSpecs() = runComposeUiTest {
        // 2 specs x 3 operations = 6 total; createUser is Mock in each spec = 2 mocked.
        // Global, not per-tab: the count must not reset when only the "example" tab is visible.
        setScreen(uiState = MockScreenTestData.contentState())

        onNodeWithText(text = "2 of 6 mocked").assertIsDisplayed()
    }

    @Test
    fun globalToggle_stateChangeInvokesCallback() = runComposeUiTest {
        var callbackValue: Boolean? = null

        setScreen(
            uiState = MockScreenTestData.contentState(globalMockingEnabled = false),
            onGlobalToggle = { enabled -> callbackValue = enabled }
        )

        onNodeWithTag(testTag = "global_mock_toggle_switch").performClick()


        callbackValue shouldBe true
    }

    @Test
    fun endpointSelection_invokesOnSelectOperationCallback() = runComposeUiTest {
        var selected: OperationKey? = null

        setScreen(
            uiState = MockScreenTestData.contentState(),
            onSelectOperation = { operationKey -> selected = operationKey }
        )

        onNodeWithTag(testTag = "endpoint_card_example_getUser").performClick()

        selected shouldBe OperationKey(specId = "example", operationId = "getUser")
    }

    @Test
    fun multipleEndpointCardsAllRender() = runComposeUiTest {
        setScreen(uiState = MockScreenTestData.contentState())

        onNodeWithTag(testTag = "endpoint_card_example_getUser").assertIsDisplayed()
        onNodeWithTag(testTag = "endpoint_card_example_createUser").assertIsDisplayed()
    }

    @Test
    fun searchQuery_narrowsVisibleOperations() = runComposeUiTest {
        setScreen(uiState = MockScreenTestData.contentState())

        onNodeWithText(text = "Search operations").performTextInput(text = "health")
        waitForIdle()

        collapseFilters()
        onNodeWithTag(testTag = "endpoint_card_example_health").assertIsDisplayed()
        onAllNodesWithTag(testTag = "endpoint_card_example_getUser").assertCountEquals(expectedSize = 0)
    }

    @Test
    fun clearSearchButton_restoresFullList() = runComposeUiTest {
        setScreen(uiState = MockScreenTestData.contentState())

        onNodeWithText(text = "Search operations").performTextInput(text = "health")
        waitForIdle()
        onNodeWithText(text = "Search operations").performTextClearance()
        waitForIdle()

        collapseFilters()
        onNodeWithTag(testTag = "endpoint_card_example_getUser").assertIsDisplayed()
        onNodeWithTag(testTag = "endpoint_card_example_health").assertIsDisplayed()
    }

    @Test
    fun versionFilterChip_narrowsToThatVersion() = runComposeUiTest {
        setScreen(uiState = MockScreenTestData.contentState())
        expandFilters()

        clickFilter(rowTag = "version_filter_row_example", chipTag = "version_filter_example_v1")
        waitForIdle()

        collapseFilters()
        onNodeWithTag(testTag = "endpoint_card_example_getUser").assertIsDisplayed()
        onAllNodesWithTag(testTag = "endpoint_card_example_createUser").assertCountEquals(expectedSize = 0)
    }

    @Test
    fun versionFilterAllChip_restoresFullList() = runComposeUiTest {
        setScreen(uiState = MockScreenTestData.contentState())
        expandFilters()

        clickFilter(rowTag = "version_filter_row_example", chipTag = "version_filter_example_v1")
        waitForIdle()
        clickFilter(rowTag = "version_filter_row_example", chipTag = "version_filter_all_example")
        waitForIdle()

        collapseFilters()
        onNodeWithTag(testTag = "endpoint_card_example_getUser").assertIsDisplayed()
        onNodeWithTag(testTag = "endpoint_card_example_createUser").assertIsDisplayed()
    }

    @Test
    fun versionFilterRow_isAbsent_forSpecWithNoVersions() = runComposeUiTest {
        setScreen(uiState = MockScreenTestData.contentState())
        expandFilters()

        onNodeWithTag(testTag = "spec_tab_catalog").performClick()
        waitForIdle()

        onAllNodesWithTag(testTag = "version_filter_row_catalog").assertCountEquals(expectedSize = 0)
    }

    @Test
    fun methodFilterChip_narrowsToThatMethod() = runComposeUiTest {
        setScreen(uiState = MockScreenTestData.contentState())
        expandFilters()

        clickFilter(rowTag = "method_filter_row_example", chipTag = "method_filter_example_POST")
        waitForIdle()

        collapseFilters()
        onNodeWithTag(testTag = "endpoint_card_example_createUser").assertIsDisplayed()
        onAllNodesWithTag(testTag = "endpoint_card_example_getUser").assertCountEquals(expectedSize = 0)
        onAllNodesWithTag(testTag = "endpoint_card_example_health").assertCountEquals(expectedSize = 0)
    }

    @Test
    fun methodFilterChip_deselecting_restoresFullList() = runComposeUiTest {
        setScreen(uiState = MockScreenTestData.contentState())
        expandFilters()

        clickFilter(rowTag = "method_filter_row_example", chipTag = "method_filter_example_POST")
        waitForIdle()
        clickFilter(rowTag = "method_filter_row_example", chipTag = "method_filter_example_POST", selected = false)
        waitForIdle()

        collapseFilters()
        onNodeWithTag(testTag = "endpoint_card_example_getUser").assertIsDisplayed()
        onNodeWithTag(testTag = "endpoint_card_example_createUser").assertIsDisplayed()
        onNodeWithTag(testTag = "endpoint_card_example_health").assertIsDisplayed()
    }

    @Test
    fun methodFilterChips_unionMultipleSelections() = runComposeUiTest {
        setScreen(uiState = MockScreenTestData.contentState())
        expandFilters()

        clickFilter(rowTag = "method_filter_row_example", chipTag = "method_filter_example_GET")
        collapseFilters()
        onNodeWithTag(testTag = "endpoint_card_example_getUser").assertIsDisplayed()
        onNodeWithTag(testTag = "endpoint_card_example_health").assertIsDisplayed()
        onAllNodesWithTag(testTag = "endpoint_card_example_createUser").assertCountEquals(expectedSize = 0)
        expandFilters()
        clickFilter(rowTag = "method_filter_row_example", chipTag = "method_filter_example_POST")
        waitForIdle()

        collapseFilters()
        onNodeWithTag(testTag = "endpoint_card_example_getUser").assertIsDisplayed()
        onNodeWithTag(testTag = "endpoint_card_example_createUser").assertIsDisplayed()
        onNodeWithTag(testTag = "endpoint_card_example_health").assertIsDisplayed()
    }

    @Test
    fun methodAndVersionFilters_intersect() = runComposeUiTest {
        setScreen(uiState = MockScreenTestData.contentState())
        expandFilters()

        clickFilter(rowTag = "version_filter_row_example", chipTag = "version_filter_example_v1")
        waitForIdle()
        clickFilter(rowTag = "method_filter_row_example", chipTag = "method_filter_example_GET")
        waitForIdle()

        collapseFilters()
        onNodeWithTag(testTag = "endpoint_card_example_getUser").assertIsDisplayed()
        onAllNodesWithTag(testTag = "endpoint_card_example_createUser").assertCountEquals(expectedSize = 0)
        onAllNodesWithTag(testTag = "endpoint_card_example_health").assertCountEquals(expectedSize = 0)
    }

    @Test
    fun filterRows_areCollapsed_byDefault() = runComposeUiTest {
        setScreen(uiState = MockScreenTestData.contentState())

        onAllNodesWithTag(testTag = "state_filter_row").assertCountEquals(expectedSize = 0)
    }

    @Test
    fun expandFilterButton_revealsFilterRows() = runComposeUiTest {
        setScreen(uiState = MockScreenTestData.contentState())
        expandFilters()

        onNodeWithTag(testTag = "state_filter_row").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun stateFilterChip_mocked_narrowsToMockedOperations() = runComposeUiTest {
        // createUser is the only Mock-state operation in MockScreenTestData; getUser/health are Network.
        setScreen(uiState = MockScreenTestData.contentState())
        expandFilters()

        onNodeWithTag(testTag = "state_filter_chip_MOCKED").performScrollTo().performClick()
        waitForIdle()

        collapseFilters()
        onNodeWithTag(testTag = "endpoint_card_example_createUser").assertIsDisplayed()
        onAllNodesWithTag(testTag = "endpoint_card_example_getUser").assertCountEquals(expectedSize = 0)
        onAllNodesWithTag(testTag = "endpoint_card_example_health").assertCountEquals(expectedSize = 0)
    }

    @Test
    fun tagFilterRow_isAbsent_forSpecWithNoTags() = runComposeUiTest {
        setScreen(uiState = MockScreenTestData.contentState())
        expandFilters()

        onNodeWithTag(testTag = "spec_tab_catalog").performClick()
        waitForIdle()

        onAllNodesWithTag(testTag = "tag_filter_row_catalog").assertCountEquals(expectedSize = 0)
    }

    @Test
    fun tagFilterChip_narrowsToThatTag() = runComposeUiTest {
        // Only createUser carries the "Admin" tag in MockScreenTestData.
        setScreen(uiState = MockScreenTestData.contentState())
        expandFilters()

        clickFilter(rowTag = "tag_filter_row_example", chipTag = "tag_filter_example_Admin")
        waitForIdle()

        collapseFilters()
        onNodeWithTag(testTag = "endpoint_card_example_createUser").assertIsDisplayed()
        onAllNodesWithTag(testTag = "endpoint_card_example_getUser").assertCountEquals(expectedSize = 0)
        onAllNodesWithTag(testTag = "endpoint_card_example_health").assertCountEquals(expectedSize = 0)
    }

    @Test
    fun tagFilterChips_unionMultipleSelections() = runComposeUiTest {
        setScreen(uiState = MockScreenTestData.contentState())
        expandFilters()

        clickFilter(rowTag = "tag_filter_row_example", chipTag = "tag_filter_example_Users")
        waitForIdle()
        clickFilter(rowTag = "tag_filter_row_example", chipTag = "tag_filter_example_Admin")
        waitForIdle()

        collapseFilters()
        onNodeWithTag(testTag = "endpoint_card_example_getUser").assertIsDisplayed()
        onNodeWithTag(testTag = "endpoint_card_example_createUser").assertIsDisplayed()
        onAllNodesWithTag(testTag = "endpoint_card_example_health").assertCountEquals(expectedSize = 0)
    }

    @Test
    fun tagFilterChip_deselecting_restoresFullList() = runComposeUiTest {
        setScreen(uiState = MockScreenTestData.contentState())
        expandFilters()

        clickFilter(rowTag = "tag_filter_row_example", chipTag = "tag_filter_example_Admin")
        waitForIdle()
        clickFilter(rowTag = "tag_filter_row_example", chipTag = "tag_filter_example_Admin", selected = false)
        waitForIdle()

        collapseFilters()
        onNodeWithTag(testTag = "endpoint_card_example_getUser").assertIsDisplayed()
        onNodeWithTag(testTag = "endpoint_card_example_createUser").assertIsDisplayed()
        onNodeWithTag(testTag = "endpoint_card_example_health").assertIsDisplayed()
    }

    @Test
    fun sortChip_reordersTheOperationList() = runComposeUiTest {
        setScreen(uiState = MockScreenTestData.contentState())
        val initialCreateUserY = onNodeWithText(text = "Create User").fetchSemanticsNode().positionInRoot.y
        val initialHealthY = onNodeWithText(text = "Health").fetchSemanticsNode().positionInRoot.y
        (initialCreateUserY < initialHealthY) shouldBe true

        expandFilters()
        onNodeWithTag(testTag = "sort_filter_METHOD").performScrollTo().performClick()
        onNodeWithContentDescription(label = "Hide filters, 1 active").performClick()
        waitForIdle()

        val sortedCreateUserY = onNodeWithText(text = "Create User").fetchSemanticsNode().positionInRoot.y
        val sortedHealthY = onNodeWithText(text = "Health").fetchSemanticsNode().positionInRoot.y
        (sortedHealthY < sortedCreateUserY) shouldBe true
    }

    @Test
    fun clearFilters_keepsSearchAndResetsSort() = runComposeUiTest {
        setScreen(uiState = MockScreenTestData.contentState())
        onNodeWithText(text = "Search operations").performTextInput(text = "health")
        expandFilters()
        onNodeWithTag(testTag = "sort_filter_METHOD").performScrollTo().performClick()
        onNodeWithContentDescription(label = "Hide filters, 1 active").assertExists()
        onNodeWithText(text = "Clear filters").performScrollTo().performClick()
        onNodeWithContentDescription(label = "Hide filters, 0 active").performClick()

        onNodeWithTag(testTag = "endpoint_card_example_health").assertIsDisplayed()
        onAllNodesWithTag(testTag = "endpoint_card_example_getUser").assertCountEquals(expectedSize = 0)
    }

    @Test
    fun filteredEmptyState_clearsSearchAndFilters() = runComposeUiTest {
        setScreen(uiState = MockScreenTestData.contentState())
        onNodeWithText(text = "Search operations").performTextInput(text = "no matching endpoint")
        onNodeWithText(text = "Clear search and filters").performClick()
        onNodeWithTag(testTag = "endpoint_card_example_getUser").assertIsDisplayed()
    }

    @Test
    fun searchIsIndependentForEachApiGroup() = runComposeUiTest {
        setScreen(uiState = MockScreenTestData.contentState())
        onNodeWithText(text = "Search operations").performTextInput(text = "health")
        onNodeWithTag(testTag = "spec_tab_catalog").performClick()
        waitForIdle()
        onNodeWithText(text = "Search operations").assertTextEquals("", "Search operations")
        onNodeWithTag(testTag = "spec_tab_example").performClick()
        waitForIdle()
        onNodeWithTag(testTag = "endpoint_card_example_health").assertIsDisplayed()
        onAllNodesWithTag(testTag = "endpoint_card_example_getUser").assertCountEquals(expectedSize = 0)
    }

    @Test
    fun removingSelectedSpec_fallsBackWithoutLosingOtherGroupsSearch() = runComposeUiTest {
        val original = MockScreenTestData.contentState()
        var content by mutableStateOf(value = original)
        setScreen(uiState = original, uiStateProvider = { content })
        onNodeWithText(text = "Search operations").performTextInput(text = "health")
        onNodeWithTag(testTag = "spec_tab_catalog").performClick()
        waitForIdle()
        onNodeWithText(text = "Search operations").performTextInput(text = "create")

        runOnIdle { content = original.copy(specs = original.specs.removingAt(index = 1)) }
        waitForIdle()
        onNodeWithTag(testTag = "spec_tab_example").assertIsSelected()
        onNodeWithTag(testTag = "endpoint_card_example_health").assertIsDisplayed()
        onAllNodesWithTag(testTag = "endpoint_card_example_createUser").assertCountEquals(expectedSize = 0)
    }

    @Test
    fun reorderingSpecs_keepsSelectedGroupAndBothSearches() = runComposeUiTest {
        val original = MockScreenTestData.contentState()
        var content by mutableStateOf(value = original)
        setScreen(uiState = original, uiStateProvider = { content })
        onNodeWithText(text = "Search operations").performTextInput(text = "health")
        onNodeWithTag(testTag = "spec_tab_catalog").performClick()
        waitForIdle()
        onNodeWithText(text = "Search operations").performTextInput(text = "create")

        runOnIdle { content = original.copy(specs = original.specs.removingAt(index = 1).addingAt(index = 0, element = original.specs[1])) }
        waitForIdle()
        onNodeWithTag(testTag = "spec_tab_catalog").assertIsSelected()
        onNodeWithTag(testTag = "endpoint_card_catalog_createUser").assertIsDisplayed()
        onAllNodesWithTag(testTag = "endpoint_card_catalog_health").assertCountEquals(expectedSize = 0)
        onNodeWithTag(testTag = "spec_tab_example").performClick()
        waitForIdle()
        onNodeWithTag(testTag = "endpoint_card_example_health").assertIsDisplayed()
        onAllNodesWithTag(testTag = "endpoint_card_example_createUser").assertCountEquals(expectedSize = 0)
    }

    private fun ComposeUiTest.clickFilter(rowTag: String, chipTag: String, selected: Boolean = true) {
        onNodeWithTag(testTag = rowTag).performScrollTo()
        onNodeWithTag(testTag = chipTag).performScrollTo().assertIsDisplayed().performClick()
        waitForIdle()
        if (selected) onNodeWithTag(testTag = chipTag).assertIsSelected()
        else onNodeWithTag(testTag = chipTag).assertIsNotSelected()
    }

    private fun ComposeUiTest.expandFilters() {
        onNodeWithContentDescription(label = "Show filters", substring = true).performClick()
        waitForIdle()
    }

    private fun ComposeUiTest.collapseFilters() {
        val expanded = onAllNodesWithContentDescription(label = "Hide filters", substring = true)
            .fetchSemanticsNodes().isNotEmpty()
        if (expanded) onNodeWithContentDescription(label = "Hide filters", substring = true).performClick()
        waitForIdle()
    }

    private fun ComposeUiTest.setScreen(
        uiState: NetworkMockUiState,
        onGlobalToggle: (Boolean) -> Unit = {},
        onSelectOperation: (OperationKey) -> Unit = {},
        uiStateProvider: () -> NetworkMockUiState = { uiState }
    ) {
        setContent {
            var listState by remember { mutableStateOf(value = NetworkMockListState()) }
            MaterialTheme {
                NetworkMockScreenContent(
                    uiState = uiStateProvider(),
                    onGlobalToggle = onGlobalToggle,
                    onSelectOperation = onSelectOperation,
                    listState = listState,
                    onSpecSelect = { listState = listState.copy(selectedSpecId = it) },
                    onFiltersExpandedChange = { listState = listState.copy(expanded = it) },
                    onFiltersChange = { specId, filters ->
                        listState = listState.copy(filters = listState.filters + (specId to filters))
                    }
                )
            }
        }
    }
}
