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
        var selected: OperationKey? = null

        setScreen(
            uiState = MockScreenTestData.contentState(),
            navigateToEndpointScreen = { operationKey -> selected = operationKey }
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

    private fun ComposeUiTest.setScreen(
        uiState: NetworkMockUiState,
        onGlobalToggle: (Boolean) -> Unit = {},
        navigateToEndpointScreen: (OperationKey) -> Unit = { },
    ) {
        setContent {
            MaterialTheme {
                NetworkMockScreenContent(
                    uiState = uiState,
                    onGlobalToggle = onGlobalToggle,
                    navigateToEndpointScreen = navigateToEndpointScreen,
                )
            }
        }
    }
}
