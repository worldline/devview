package com.worldline.devview.featureflip.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/**
 * Creates a DataStore instance for iOS.
 *
 * The DataStore file is created in the iOS app's document directory.
 *
 * @return A configured DataStore instance for storing feature preferences
 */
@OptIn(ExperimentalForeignApi::class)
internal fun createDataStore(): DataStore<Preferences> = createDataStore(
    producePath = {
        val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null
        )
        requireNotNull(documentDirectory).path + "/$FEATURE_FLIP_DATASTORE_NAME"
    }
)

/**
 * iOS implementation of [rememberDataStore].
 *
 * Creates and remembers a DataStore instance using the iOS app's document directory.
 *
 * @return A remembered DataStore instance for feature flag persistence
 */
@Composable
internal actual fun rememberDataStore(): DataStore<Preferences> = remember {
    createDataStore()
}
