package com.worldline.devview.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

/**
 * Creates a DataStore instance for Android.
 *
 * The DataStore file is created in the app's files directory.
 *
 * @param context The Android context used to access the files directory
 * @param dataStoreName The name of the DataStore file
 * @return A configured DataStore<Preferences> instance
 */
internal fun createDataStore(context: Context, dataStoreName: String): DataStore<Preferences> =
    createDataStore(
        producePath = {
            context.filesDir.resolve(relative = dataStoreName).absolutePath
        }
    )

/**
 * Android implementation of [rememberDataStore].
 *
 * Creates and remembers a DataStore instance using the Android app's files directory.
 *
 * @param dataStoreName The name of the DataStore file
 * @return A remembered DataStore<Preferences> instance
 */
@Composable
public actual fun rememberDataStore(dataStoreName: String): DataStore<Preferences> {
    val context = LocalContext.current

    return remember(key1 = dataStoreName) {
        createDataStore(
            context = context,
            dataStoreName = dataStoreName
        )
    }
}
