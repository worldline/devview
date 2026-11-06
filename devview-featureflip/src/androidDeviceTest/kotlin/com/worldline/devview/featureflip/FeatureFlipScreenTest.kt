package com.worldline.devview.featureflip

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.compose.ui.test.StateRestorationTester
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.worldline.devview.featureflip.model.Feature
import com.worldline.devview.featureflip.model.FeatureHandler
import com.worldline.devview.featureflip.model.FeatureState
import com.worldline.devview.featureflip.model.LocalFeatureHandler
import com.worldline.devview.test.FakePreferencesDataStore
import com.worldline.devview.test.waitUntilTagCount
import kotlinx.coroutines.CompletableDeferred
import okio.IOException
import org.junit.Test
import kotlin.test.assertEquals

class FeatureFlipScreenTest {
    @Test
    fun search_filters_features_and_clearing_restores_them() = runComposeUiTest {
        val handler = FeatureHandler(
            dataStore = FakePreferencesDataStore(),
            initialFeatures = listOf(
                Feature.LocalFeature(name = "dark_mode", description = "Dark mode", isEnabled = false),
                Feature.RemoteFeature(name = "checkout", description = null, defaultRemoteValue = true, state = FeatureState.REMOTE)
            )
        )
        setContent {
            CompositionLocalProvider(value = LocalFeatureHandler provides handler) { FeatureFlipScreen() }
        }
        waitUntilTagCount(tag = "feature_item_checkout", expectedCount = 1)
        onNodeWithText(text = "Filter features").performTextInput(text = "dark")
        waitUntilTagCount(tag = "feature_item_checkout", expectedCount = 0)
        onNodeWithTag(testTag = "feature_item_dark_mode").assertIsDisplayed()
        onNodeWithText(text = "Filter features").performTextClearance()
        waitUntilTagCount(tag = "feature_item_checkout", expectedCount = 1)
    }

    @Test
    fun type_filters_use_or_and_expose_count_when_collapsed() = runComposeUiTest {
        val handler = FeatureHandler(
            dataStore = FakePreferencesDataStore(),
            initialFeatures = listOf(
                Feature.LocalFeature(name = "dark_mode", description = null, isEnabled = false),
                Feature.RemoteFeature(name = "checkout", description = null, defaultRemoteValue = true, state = FeatureState.REMOTE)
            )
        )
        setContent {
            CompositionLocalProvider(value = LocalFeatureHandler provides handler) { FeatureFlipScreen() }
        }
        onNodeWithTag(testTag = "feature_filter_chip_REMOTE").assertDoesNotExist()
        onNodeWithContentDescription(label = "Show filters, 0 active").performClick()
        onNodeWithText(text = "Clear filters").assertIsNotEnabled()
        onNodeWithTag(testTag = "feature_filter_chip_REMOTE").performClick().assertIsSelected()
        waitUntilTagCount(tag = "feature_item_dark_mode", expectedCount = 0)
        onNodeWithTag(testTag = "feature_filter_chip_LOCAL").performClick()
        waitUntilTagCount(tag = "feature_item_dark_mode", expectedCount = 1)
        waitUntilTagCount(tag = "feature_item_checkout", expectedCount = 1)
        onNodeWithContentDescription(label = "Hide filters, 2 active").performClick()
        onNodeWithContentDescription(label = "Show filters, 2 active").assertIsDisplayed().performClick()
        onNodeWithText(text = "Clear filters").performClick().assertIsNotEnabled()
        onNodeWithContentDescription(label = "Hide filters, 0 active").assertIsDisplayed()
    }

    @Test
    fun override_changes_effective_state_and_filter_empty_state_is_recoverable() = runComposeUiTest {
        val handler = FeatureHandler(
            dataStore = FakePreferencesDataStore(),
            initialFeatures = listOf(
                Feature.RemoteFeature(name = "checkout", description = null, defaultRemoteValue = false, state = FeatureState.REMOTE)
            )
        )
        setContent {
            CompositionLocalProvider(value = LocalFeatureHandler provides handler) { FeatureFlipScreen() }
        }
        onNodeWithContentDescription(label = "Show filters, 0 active").performClick()
        onNodeWithTag(testTag = "feature_filter_chip_LOCAL").assertDoesNotExist()
        onNodeWithTag(testTag = "feature_filter_chip_OFF").performClick()
        waitUntilTagCount(tag = "feature_item_checkout", expectedCount = 1)
        onNodeWithContentDescription(label = "checkout: On").performClick()
        waitUntilTagCount(tag = "feature_item_checkout", expectedCount = 0)
        onNodeWithText(text = "No features match your filters").assertIsDisplayed()
        onNodeWithText(text = "Clear search and filters").performClick()
        waitUntilTagCount(tag = "feature_item_checkout", expectedCount = 1)
        onNodeWithContentDescription(label = "checkout: On").assertIsSelected()
        onNodeWithText(text = "Remote value: off").assertIsDisplayed()
    }

    @Test
    fun search_filters_and_panel_survive_saved_state_restoration() = runComposeUiTest {
        val handler = FeatureHandler(
            dataStore = FakePreferencesDataStore(),
            initialFeatures = listOf(
                Feature.LocalFeature(name = "dark_mode", description = null, isEnabled = false),
                Feature.RemoteFeature(name = "checkout", description = null, defaultRemoteValue = true, state = FeatureState.REMOTE)
            )
        )
        val restoration = StateRestorationTester(this)
        restoration.setContent {
            CompositionLocalProvider(value = LocalFeatureHandler provides handler) { FeatureFlipScreen() }
        }
        onNodeWithText(text = "Filter features").performTextInput(text = "check")
        onNodeWithContentDescription(label = "Show filters, 0 active").performClick()
        waitUntilTagCount(tag = "feature_filter_chip_REMOTE", expectedCount = 1)
        onNodeWithTag(testTag = "feature_filter_chip_REMOTE").performScrollTo().performClick()
        restoration.emulateSaveAndRestore()
        onNodeWithText(text = "Filter features").assertTextEquals("Filter features", "check")
        onNodeWithContentDescription(label = "Hide filters, 1 active").assertIsDisplayed()
        onNodeWithTag(testTag = "feature_filter_chip_REMOTE").assertIsSelected()
        waitUntilTagCount(tag = "feature_item_dark_mode", expectedCount = 0)
        waitUntilTagCount(tag = "feature_item_checkout", expectedCount = 1)
        onNodeWithContentDescription(label = "Hide filters, 1 active").performClick()
        restoration.emulateSaveAndRestore()
        onNodeWithContentDescription(label = "Show filters, 1 active").assertIsDisplayed()
    }

    @Test
    fun restored_type_filter_survives_a_changed_registry() = runComposeUiTest {
        val local = Feature.LocalFeature(name = "local", description = null, isEnabled = false)
        val handler = mutableStateOf(
            value = FeatureHandler(
                dataStore = FakePreferencesDataStore(),
                initialFeatures = listOf(
                    local,
                    Feature.RemoteFeature(name = "remote", description = null, defaultRemoteValue = true, state = FeatureState.REMOTE)
                )
            )
        )
        val restoration = StateRestorationTester(this)
        restoration.setContent {
            CompositionLocalProvider(value = LocalFeatureHandler provides handler.value) { FeatureFlipScreen() }
        }
        onNodeWithContentDescription(label = "Show filters, 0 active").performClick()
        onNodeWithTag(testTag = "feature_filter_chip_REMOTE").performClick()
        runOnIdle {
            handler.value = FeatureHandler(dataStore = FakePreferencesDataStore(), initialFeatures = listOf(local))
        }
        restoration.emulateSaveAndRestore()
        onNodeWithContentDescription(label = "Hide filters, 1 active").assertIsDisplayed()
        onNodeWithTag(testTag = "feature_filter_chip_REMOTE").assertIsSelected()
        onNodeWithText(text = "No features match your filters").assertIsDisplayed()
        onNodeWithTag(testTag = "feature_item_local").assertDoesNotExist()
        onNodeWithText(text = "Clear search and filters").performClick()
        waitUntilTagCount(tag = "feature_item_local", expectedCount = 1)
    }

    @Test
    fun write_failure_keeps_selection_and_retry_persists_it() = runComposeUiTest {
        var fail = true
        val store = FakePreferencesDataStore()
        val failingStore = object : DataStore<Preferences> by store {
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
                if (fail) throw IOException("Disk full")
                return store.updateData(transform = transform)
            }
        }
        val handler = FeatureHandler(
            dataStore = failingStore,
            initialFeatures = listOf(
                Feature.RemoteFeature(name = "checkout", description = null, defaultRemoteValue = false, state = FeatureState.REMOTE)
            )
        )
        setContent {
            CompositionLocalProvider(value = LocalFeatureHandler provides handler) { FeatureFlipScreen() }
        }
        onNodeWithContentDescription(label = "checkout: On").performClick()
        onNodeWithText(text = "Could not save checkout. The previous value is unchanged.").assertIsDisplayed()
        onNodeWithContentDescription(label = "checkout: Remote").assertIsSelected()
        runOnIdle { fail = false }
        onNodeWithText(text = "Retry").performClick()
        onNodeWithContentDescription(label = "checkout: On").assertIsSelected()
        onNodeWithText(text = "Could not save checkout. The previous value is unchanged.").assertDoesNotExist()
    }

    @Test
    fun selections_persist_in_click_order_when_the_first_write_waits() = runComposeUiTest {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var writes = 0
        val store = FakePreferencesDataStore()
        val delayedStore = object : DataStore<Preferences> by store {
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
                writes++
                if (writes == 1) {
                    entered.complete(value = Unit)
                    release.await()
                }
                return store.updateData(transform = transform)
            }
        }
        val handler = FeatureHandler(
            dataStore = delayedStore,
            initialFeatures = listOf(
                Feature.RemoteFeature(name = "checkout", description = null, defaultRemoteValue = false, state = FeatureState.REMOTE)
            )
        )
        setContent {
            CompositionLocalProvider(value = LocalFeatureHandler provides handler) { FeatureFlipScreen() }
        }
        onNodeWithContentDescription(label = "checkout: Off").performClick()
        waitUntil { entered.isCompleted }
        onNodeWithContentDescription(label = "checkout: On").performClick()
        runOnIdle { assertEquals(expected = 1, actual = writes) }
        release.complete(value = Unit)
        onNodeWithContentDescription(label = "checkout: On").assertIsSelected()
        runOnIdle { assertEquals(expected = 2, actual = writes) }
    }

    @Test
    fun unregistered_features_show_guidance_without_recovery_button() = runComposeUiTest {
        val handler = FeatureHandler(dataStore = FakePreferencesDataStore(), initialFeatures = emptyList())
        setContent {
            CompositionLocalProvider(value = LocalFeatureHandler provides handler) { FeatureFlipScreen() }
        }
        onNodeWithText(text = "No features registered").assertIsDisplayed()
        onNodeWithText(text = "Register flags with FeatureHandler.").assertIsDisplayed()
        onNodeWithText(text = "Clear search and filters").assertDoesNotExist()
    }

    @Test
    fun type_and_state_filters_combine_with_and() = runComposeUiTest {
        val handler = FeatureHandler(
            dataStore = FakePreferencesDataStore(),
            initialFeatures = listOf(
                Feature.LocalFeature(name = "local_off", description = null, isEnabled = false),
                Feature.LocalFeature(name = "local_on", description = null, isEnabled = true),
                Feature.RemoteFeature(name = "remote_on", description = null, defaultRemoteValue = true, state = FeatureState.REMOTE)
            )
        )
        setContent {
            CompositionLocalProvider(value = LocalFeatureHandler provides handler) { FeatureFlipScreen() }
        }
        onNodeWithContentDescription(label = "Show filters, 0 active").performClick()
        onNodeWithTag(testTag = "feature_filter_chip_LOCAL").performClick()
        onNodeWithTag(testTag = "feature_filter_chip_ON").performClick()
        waitUntilTagCount(tag = "feature_item_local_off", expectedCount = 0)
        waitUntilTagCount(tag = "feature_item_remote_on", expectedCount = 0)
        onNodeWithTag(testTag = "feature_item_local_on").assertIsDisplayed()
    }
}
