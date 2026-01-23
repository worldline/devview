package com.worldline.devview.featureflip.model

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
 * @return A configured DataStore instance for storing feature preferences
 */
internal fun createDataStore(context: Context): DataStore<Preferences> = createDataStore(
    producePath = {
        context.filesDir.resolve(relative = FEATURE_FLIP_DATASTORE_NAME).absolutePath
    }
)

/**
 * Android implementation of [rememberDataStore].
 *
 * Creates and remembers a DataStore instance using the Android app's files directory.
 *
 * @return A remembered DataStore instance for feature flag persistence
 */
@Composable
internal actual fun rememberDataStore(): DataStore<Preferences> {
    val context = LocalContext.current

    return remember {
        createDataStore(context = context)
    }
}
