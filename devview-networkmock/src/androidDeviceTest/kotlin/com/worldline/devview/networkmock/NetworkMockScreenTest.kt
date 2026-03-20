package com.worldline.devview.networkmock

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.worldline.devview.networkmock.core.model.EndpointMockState
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
    fun rendersHostTabs_forContentState() = runComposeUiTest {
        setScreen(uiState = MockScreenTestData.contentState())

        onNodeWithTag(testTag = "host_tab_staging").assertIsDisplayed()
        onNodeWithTag(testTag = "host_tab_production").assertIsDisplayed()
    }


    @Test
    fun initialSelectedTab_isFirstHost() = runComposeUiTest {
        setScreen(uiState = MockScreenTestData.contentState())

        onNodeWithTag(testTag = "host_tab_staging").assertIsSelected()

    }

    @Test
    fun tabSwitching_changesVisibleEndpoints() = runComposeUiTest {
        setScreen(uiState = MockScreenTestData.contentState())

        onNodeWithTag(testTag = "endpoint_card_staging_getUser").assertIsDisplayed()

        onNodeWithTag(testTag = "host_tab_production").performClick()
        waitForIdle()

        onNodeWithTag(testTag = "endpoint_card_production_getProduct").assertIsDisplayed()
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
    fun endpointSelection_invokesSelectEndpointCallback() = runComposeUiTest {
        var selected: Pair<String, String>? = null

        setScreen(
            uiState = MockScreenTestData.contentState(),
            selectEndpoint = { hostId, endpointId -> selected = hostId to endpointId }
        )

        onNodeWithTag(testTag = "endpoint_card_staging_getUser").performClick()

        selected shouldBe ("staging" to "getUser")
    }

    private fun ComposeUiTest.setScreen(
        uiState: NetworkMockUiState,
        onGlobalToggle: (Boolean) -> Unit = {},
        setEndpointMockState: (String, String, String?) -> Unit = { _, _, _ -> },
        selectEndpoint: (String, String) -> Unit = { _, _ -> },
        clearSelectedEndpoint: () -> Unit = {},
    ) {
        setContent {
            MaterialTheme {
                NetworkMockScreenContent(
                    uiState = uiState,
                    onGlobalToggle = onGlobalToggle,
                    setEndpointMockState = setEndpointMockState,
                    selectEndpoint = selectEndpoint,
                    clearSelectedEndpoint = clearSelectedEndpoint,
                    selectedDescriptor = null,
                    selectedEndpointState = EndpointMockState.Network
                )
            }
        }
    }
}
