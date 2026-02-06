package com.worldline.devview.utils

import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

/**
 * Creates a DataStore instance with a custom path.
 *
 * This function creates a preferences DataStore at the path provided by the lambda.
 * It's used by platform-specific implementations to create the DataStore with
 * the appropriate file path for each platform.
 *
 * @param producePath A lambda that returns the absolute path where the DataStore file should be created
 * @return A configured DataStore<Preferences> instance
 */
public fun createDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = {
            producePath().toPath()
        }
    )

/**
 * Platform-specific composable that remembers and returns a DataStore instance.
 *
 * Each platform (Android, iOS) implements this to create a DataStore at the
 * appropriate location for that platform.
 *
 * @param dataStoreName The name of the DataStore file
 * @return A remembered DataStore<Preferences> instance
 */
@Composable
public expect fun rememberDataStore(
    dataStoreName: String
): DataStore<Preferences>
