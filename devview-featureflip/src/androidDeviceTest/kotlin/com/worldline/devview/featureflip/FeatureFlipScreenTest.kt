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
import com.worldline.devview.featureflip.model.Feature
import com.worldline.devview.featureflip.model.FeatureHandler
import com.worldline.devview.featureflip.model.FeatureState
import com.worldline.devview.featureflip.model.LocalFeatureHandler
import com.worldline.devview.test.FakePreferencesDataStore
import com.worldline.devview.test.waitUntilTagCount
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

        // Wait for lazy item enter animations before asserting visibility.
        waitUntilTagCount(tag = "feature_item_dark_mode", expectedCount = 1)
        waitUntilTagCount(tag = "feature_item_new_checkout", expectedCount = 1)

        // Filter by entering "dark" in the search field
        onNodeWithTag(testTag = "feature_filter_field").performTextInput("dark")

        // After filtering, only dark_mode should be visible
        waitUntilTagCount(tag = "feature_item_dark_mode", expectedCount = 1)
        waitUntilTagCount(tag = "feature_item_new_checkout", expectedCount = 0)

        // Clear the filter by clicking the clear button
        onNodeWithTag(testTag = "clear_feature_filter_button").performClick()

        // After clearing, new_checkout should be visible again
        waitUntilTagCount(tag = "feature_item_new_checkout", expectedCount = 1)
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
        waitUntilTagCount(tag = "feature_item_new_checkout", expectedCount = 1)
        waitUntilTagCount(tag = "feature_item_dark_mode", expectedCount = 0)
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
        waitUntilTagCount(tag = "feature_item_new_checkout", expectedCount = 1)

        onNodeWithContentDescription(label = "LOCAL_ON").performClick()

        // After changing state, feature should be filtered out since it no longer matches OFF filter
        waitUntilTagCount(tag = "feature_item_new_checkout", expectedCount = 0)
    }

    @Test
    fun featureFlipScreen_type_badges_are_displayed() = runComposeUiTest {
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

        waitUntilTagCount(tag = "feature_item_dark_mode", expectedCount = 1)
        waitUntilTagCount(tag = "feature_item_new_checkout", expectedCount = 1)

        onNodeWithTag(testTag = "feature_type_dark_mode").assertIsDisplayed()
        onNodeWithTag(testTag = "feature_type_new_checkout").assertIsDisplayed()
    }

    @Test
    fun featureFlipScreen_selecting_local_and_remote_chips_shows_all_features() = runComposeUiTest {
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

        onNodeWithTag(testTag = "feature_filter_chip_LOCAL").performClick()
        onNodeWithTag(testTag = "feature_filter_chip_REMOTE").performClick()

        waitUntilTagCount(tag = "feature_item_dark_mode", expectedCount = 1)
        waitUntilTagCount(tag = "feature_item_new_checkout", expectedCount = 1)
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

        waitForIdle()

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
        waitUntilTagCount(tag = "feature_item_dark_mode", expectedCount = 1)
        waitUntilTagCount(tag = "feature_item_new_checkout", expectedCount = 1)
    }

    @Test
    fun multipleFeaturesOfDifferentTypesAllRender() = runComposeUiTest {
        val handler = FeatureHandler(
            dataStore = FakePreferencesDataStore(),
            initialFeatures = listOf(
                Feature.LocalFeature(name = "feature_local_a", description = null, isEnabled = false),
                Feature.LocalFeature(name = "feature_local_b", description = null, isEnabled = true),
                Feature.RemoteFeature(name = "feature_remote_a", description = null, defaultRemoteValue = true, state = FeatureState.REMOTE),
            )
        )

        setContent {
            CompositionLocalProvider(LocalFeatureHandler provides handler) {
                FeatureFlipScreen()
            }
        }

        waitUntilTagCount(tag = "feature_item_feature_local_a", expectedCount = 1)
        waitUntilTagCount(tag = "feature_item_feature_local_b", expectedCount = 1)
        waitUntilTagCount(tag = "feature_item_feature_remote_a", expectedCount = 1)
    }
}
