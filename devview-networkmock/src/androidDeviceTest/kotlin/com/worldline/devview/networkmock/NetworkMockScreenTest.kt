package com.worldline.devview.networkmock

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
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
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

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

        onNodeWithTag(testTag = "networkmock_search_field").performTextInput(text = "health")
        waitForIdle()

        onNodeWithTag(testTag = "endpoint_card_example_health").assertIsDisplayed()
        onAllNodesWithTag(testTag = "endpoint_card_example_getUser").assertCountEquals(expectedSize = 0)
    }

    @Test
    fun clearSearchButton_restoresFullList() = runComposeUiTest {
        setScreen(uiState = MockScreenTestData.contentState())

        onNodeWithTag(testTag = "networkmock_search_field").performTextInput(text = "health")
        waitForIdle()
        onNodeWithTag(testTag = "networkmock_clear_search_button").performClick()
        waitForIdle()

        onNodeWithTag(testTag = "endpoint_card_example_getUser").assertIsDisplayed()
        onNodeWithTag(testTag = "endpoint_card_example_health").assertIsDisplayed()
    }

    @Test
    fun versionFilterChip_narrowsToThatVersion() = runComposeUiTest {
        setScreen(uiState = MockScreenTestData.contentState())
        expandFilters()

        onNodeWithTag(testTag = "version_filter_example_v1").performClick()
        waitForIdle()

        onNodeWithTag(testTag = "endpoint_card_example_getUser").assertIsDisplayed()
        onAllNodesWithTag(testTag = "endpoint_card_example_createUser").assertCountEquals(expectedSize = 0)
    }

    @Test
    fun versionFilterAllChip_restoresFullList() = runComposeUiTest {
        setScreen(uiState = MockScreenTestData.contentState())
        expandFilters()

        onNodeWithTag(testTag = "version_filter_example_v1").performClick()
        waitForIdle()
        onNodeWithTag(testTag = "version_filter_all_example").performClick()
        waitForIdle()

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

        onNodeWithTag(testTag = "method_filter_example_POST").performClick()
        waitForIdle()

        onNodeWithTag(testTag = "endpoint_card_example_createUser").assertIsDisplayed()
        onAllNodesWithTag(testTag = "endpoint_card_example_getUser").assertCountEquals(expectedSize = 0)
        onAllNodesWithTag(testTag = "endpoint_card_example_health").assertCountEquals(expectedSize = 0)
    }

    @Test
    fun methodFilterChip_deselecting_restoresFullList() = runComposeUiTest {
        setScreen(uiState = MockScreenTestData.contentState())
        expandFilters()

        onNodeWithTag(testTag = "method_filter_example_POST").performClick()
        waitForIdle()
        onNodeWithTag(testTag = "method_filter_example_POST").performClick()
        waitForIdle()

        onNodeWithTag(testTag = "endpoint_card_example_getUser").assertIsDisplayed()
        onNodeWithTag(testTag = "endpoint_card_example_createUser").assertIsDisplayed()
        onNodeWithTag(testTag = "endpoint_card_example_health").assertIsDisplayed()
    }

    @Test
    fun methodFilterChips_unionMultipleSelections() = runComposeUiTest {
        setScreen(uiState = MockScreenTestData.contentState())
        expandFilters()

        onNodeWithTag(testTag = "method_filter_example_GET").performClick()
        waitForIdle()
        onNodeWithTag(testTag = "method_filter_example_POST").performClick()
        waitForIdle()

        onNodeWithTag(testTag = "endpoint_card_example_getUser").assertIsDisplayed()
        onNodeWithTag(testTag = "endpoint_card_example_createUser").assertIsDisplayed()
        onNodeWithTag(testTag = "endpoint_card_example_health").assertIsDisplayed()
    }

    @Test
    fun methodAndVersionFilters_intersect() = runComposeUiTest {
        setScreen(uiState = MockScreenTestData.contentState())
        expandFilters()

        onNodeWithTag(testTag = "version_filter_example_v1").performClick()
        waitForIdle()
        onNodeWithTag(testTag = "method_filter_example_GET").performClick()
        waitForIdle()

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

        onNodeWithTag(testTag = "state_filter_row").assertIsDisplayed()
    }

    @Test
    fun stateFilterChip_mocked_narrowsToMockedOperations() = runComposeUiTest {
        // createUser is the only Mock-state operation in MockScreenTestData; getUser/health are Network.
        setScreen(uiState = MockScreenTestData.contentState())
        expandFilters()

        onNodeWithTag(testTag = "state_filter_chip_MOCKED").performClick()
        waitForIdle()

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

        onNodeWithTag(testTag = "tag_filter_example_Admin").performClick()
        waitForIdle()

        onNodeWithTag(testTag = "endpoint_card_example_createUser").assertIsDisplayed()
        onAllNodesWithTag(testTag = "endpoint_card_example_getUser").assertCountEquals(expectedSize = 0)
        onAllNodesWithTag(testTag = "endpoint_card_example_health").assertCountEquals(expectedSize = 0)
    }

    @Test
    fun tagFilterChips_unionMultipleSelections() = runComposeUiTest {
        setScreen(uiState = MockScreenTestData.contentState())
        expandFilters()

        onNodeWithTag(testTag = "tag_filter_example_Users").performClick()
        waitForIdle()
        onNodeWithTag(testTag = "tag_filter_example_Admin").performClick()
        waitForIdle()

        onNodeWithTag(testTag = "endpoint_card_example_getUser").assertIsDisplayed()
        onNodeWithTag(testTag = "endpoint_card_example_createUser").assertIsDisplayed()
        onAllNodesWithTag(testTag = "endpoint_card_example_health").assertCountEquals(expectedSize = 0)
    }

    @Test
    fun tagFilterChip_deselecting_restoresFullList() = runComposeUiTest {
        setScreen(uiState = MockScreenTestData.contentState())
        expandFilters()

        onNodeWithTag(testTag = "tag_filter_example_Admin").performClick()
        waitForIdle()
        onNodeWithTag(testTag = "tag_filter_example_Admin").performClick()
        waitForIdle()

        onNodeWithTag(testTag = "endpoint_card_example_getUser").assertIsDisplayed()
        onNodeWithTag(testTag = "endpoint_card_example_createUser").assertIsDisplayed()
        onNodeWithTag(testTag = "endpoint_card_example_health").assertIsDisplayed()
    }

    @Test
    fun sortSharedFlow_emittingMethodLabel_reordersTheOperationList() = runComposeUiTest {
        // "example" is tagged, so the dropdown offers [SPEC_ORDER, PATH, METHOD, TAG]. Emitting
        // "Method" directly selects that sort key. Spec order is [getUser(GET), createUser(POST),
        // health(GET)] — sorting by method is a stable sort, so the GET group keeps its
        // relative order (getUser, health) ahead of the POST group (createUser), moving
        // "Health" above "Create User" relative to the untouched spec-order default.
        val sortSharedFlow = MutableSharedFlow<String>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        setScreen(uiState = MockScreenTestData.contentState(), sortSharedFlow = sortSharedFlow)

        val initialCreateUserY = onNodeWithText(text = "Create User").fetchSemanticsNode().positionInRoot.y
        val initialHealthY = onNodeWithText(text = "Health").fetchSemanticsNode().positionInRoot.y
        (initialCreateUserY < initialHealthY) shouldBe true

        sortSharedFlow.tryEmit(value = "Method")
        waitForIdle()

        val sortedCreateUserY = onNodeWithText(text = "Create User").fetchSemanticsNode().positionInRoot.y
        val sortedHealthY = onNodeWithText(text = "Health").fetchSemanticsNode().positionInRoot.y
        (sortedHealthY < sortedCreateUserY) shouldBe true
    }

    private fun ComposeUiTest.expandFilters() {
        onNodeWithTag(testTag = "expand_filter_button").performClick()
        waitForIdle()
    }

    private fun ComposeUiTest.setScreen(
        uiState: NetworkMockUiState,
        onGlobalToggle: (Boolean) -> Unit = {},
        onSelectOperation: (OperationKey) -> Unit = { },
        sortSharedFlow: SharedFlow<String> = MutableSharedFlow(),
    ) {
        setContent {
            MaterialTheme {
                NetworkMockScreenContent(
                    uiState = uiState,
                    onGlobalToggle = onGlobalToggle,
                    onSelectOperation = onSelectOperation,
                    sortSharedFlow = sortSharedFlow,
                )
            }
        }
    }
}
