package com.worldline.devview.featureflip.model

import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.worldline.devview.utils.rememberDataStore

/**
 * The filename used for the feature flip DataStore preferences file.
 */
internal const val FEATURE_FLIP_DATASTORE_NAME: String = "feature_flip_datastore.preferences_pb"

/**
 * Composable that remembers and returns a DataStore instance for feature flip persistence.
 *
 * This delegates to the platform-specific implementation in devview-utils, providing
 * the feature flip specific DataStore filename.
 *
 * @return A remembered DataStore<Preferences> instance for feature flag persistence
 */
@Composable
internal fun rememberDataStore(): DataStore<Preferences> = rememberDataStore(
    dataStoreName = FEATURE_FLIP_DATASTORE_NAME
)
