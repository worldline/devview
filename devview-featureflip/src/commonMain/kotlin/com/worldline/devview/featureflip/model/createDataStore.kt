package com.worldline.devview.featureflip.model

import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

/**
 * Creates a DataStore instance for feature flag persistence.
 *
 * This function creates a preferences DataStore at the path provided by the lambda.
 * It's used by platform-specific implementations to create the DataStore with
 * the appropriate file path for each platform.
 *
 * @param producePath A lambda that returns the absolute path where the DataStore file should be created
 * @return A configured DataStore instance for storing feature preferences
 */
public fun createDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = {
            producePath().toPath()
        }
    )

/**
 * The filename used for the feature flip DataStore preferences file.
 */
internal const val FEATURE_FLIP_DATASTORE_NAME: String = "feature_flip_datastore.preferences_pb"

/**
 * Platform-specific composable that remembers and returns a DataStore instance.
 *
 * Each platform (Android, iOS) implements this to create a DataStore at the
 * appropriate location for that platform.
 *
 * @return A remembered DataStore instance for feature flag persistence
 */
@Composable
internal expect fun rememberDataStore(): DataStore<Preferences>
