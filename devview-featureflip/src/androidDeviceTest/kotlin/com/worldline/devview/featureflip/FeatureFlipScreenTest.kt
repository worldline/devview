package com.worldline.devview.featureflip

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.worldline.devview.featureflip.model.Feature
import com.worldline.devview.featureflip.model.FeatureHandler
import com.worldline.devview.featureflip.model.FeatureState
import com.worldline.devview.featureflip.model.LocalFeatureHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test

class FeatureFlipScreenTest {

    @Test
    fun featureFlipScreen_renders_features_and_filters_by_query() = runComposeUiTest {
        val handler = FeatureHandler(
            dataStore = FakePreferencesDataStore(),
            initialFeatures = listOf(
                Feature.LocalFeature("dark_mode", "Dark mode", false),
                Feature.RemoteFeature("new_checkout", "Checkout v2", false, FeatureState.REMOTE)
            )
        )

        setContent {
            CompositionLocalProvider(LocalFeatureHandler provides handler) {
                FeatureFlipScreen()
            }
        }

        // Verify both features are displayed
        onNodeWithTag(testTag = "feature_item_dark_mode").assertIsDisplayed()
        onNodeWithTag(testTag = "feature_item_new_checkout").assertIsDisplayed()

        // Filter by entering "dark" in the search field
        onNodeWithTag(testTag = "feature_filter_field").performTextInput("dark")

        // After filtering, only dark_mode should be visible
        onNodeWithTag(testTag = "feature_item_dark_mode").assertIsDisplayed()
        onAllNodesWithTag(testTag = "feature_item_new_checkout").assertCountEquals(0)

        // Clear the filter by clicking the clear button
        onNodeWithTag(testTag = "clear_feature_filter_button").performClick()

        // After clearing, new_checkout should be visible again
        onNodeWithTag(testTag = "feature_item_new_checkout").assertIsDisplayed()
    }

    @Test
    fun featureFlipScreen_remote_chip_filters_out_local_features() = runComposeUiTest {
        val handler = FeatureHandler(
            dataStore = FakePreferencesDataStore(),
            initialFeatures = listOf(
                Feature.LocalFeature("dark_mode", null, false),
                Feature.RemoteFeature("new_checkout", null, true, FeatureState.REMOTE)
            )
        )

        setContent {
            CompositionLocalProvider(LocalFeatureHandler provides handler) {
                FeatureFlipScreen()
            }
        }

        // Click the Remote filter chip
        onNodeWithTag(testTag = "feature_filter_chip_REMOTE").performClick()

        // After filtering to Remote only, only new_checkout should be visible
        onNodeWithTag(testTag = "feature_item_new_checkout").assertIsDisplayed()
        onAllNodesWithTag(testTag = "feature_item_dark_mode").assertCountEquals(0)
    }

    @Test
    fun featureFlipScreen_state_changes_are_reflected_in_filters() = runComposeUiTest {
        val handler = FeatureHandler(
            dataStore = FakePreferencesDataStore(),
            initialFeatures = listOf(
                Feature.RemoteFeature(
                    name = "new_checkout",
                    description = null,
                    defaultRemoteValue = false,
                    state = FeatureState.REMOTE
                )
            )
        )

        setContent {
            CompositionLocalProvider(LocalFeatureHandler provides handler) {
                FeatureFlipScreen()
            }
        }

        // Click the OFF filter chip to show only disabled features
        onNodeWithTag(testTag = "feature_filter_chip_OFF").performClick()
        onNodeWithTag(testTag = "feature_item_new_checkout").assertIsDisplayed()

        onNodeWithContentDescription(label = "LOCAL_ON").performClick()

        // After changing state, feature should be filtered out since it no longer matches OFF filter
        onAllNodesWithTag(testTag = "feature_item_new_checkout").assertCountEquals(0)
    }

    @Test
    fun featureFlipScreen_clear_filter_icon_visibility_and_behavior() = runComposeUiTest {
        val handler = FeatureHandler(
            dataStore = FakePreferencesDataStore(),
            initialFeatures = listOf(
                Feature.LocalFeature("dark_mode", null, false),
                Feature.RemoteFeature("new_checkout", null, true, FeatureState.REMOTE)
            )
        )

        setContent {
            CompositionLocalProvider(LocalFeatureHandler provides handler) {
                FeatureFlipScreen()
            }
        }

        // Initially, clear button should not be visible (no text input)
        onAllNodesWithTag(testTag = "clear_feature_filter_button").assertCountEquals(0)

        // Type in filter field
        onNodeWithTag(testTag = "feature_filter_field").performTextInput("dark")
        
        // Clear button should now be visible
        onAllNodesWithTag(testTag = "clear_feature_filter_button").assertCountEquals(1)

        // Click the clear button
        onNodeWithTag(testTag = "clear_feature_filter_button").performClick()

        // Wait for AnimatedVisibility animation to finish
        waitForIdle()

        // After clearing, button should be gone again
        onAllNodesWithTag(testTag = "clear_feature_filter_button").assertCountEquals(0)
        
        // Both features should be visible again
        onNodeWithTag(testTag = "feature_item_dark_mode").assertIsDisplayed()
        onNodeWithTag(testTag = "feature_item_new_checkout").assertIsDisplayed()
    }
}

private class FakePreferencesDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}
