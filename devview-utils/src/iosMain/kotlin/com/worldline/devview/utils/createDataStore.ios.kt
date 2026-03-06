package com.worldline.devview.utils

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
 * @param dataStoreName The name of the DataStore file
 * @return A configured DataStore<Preferences> instance
 */
@OptIn(ExperimentalForeignApi::class)
internal fun createDataStore(dataStoreName: String): DataStore<Preferences> = createDataStore(
    producePath = {
        val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null
        )
        requireNotNull(documentDirectory).path + "/$dataStoreName"
    }
)

/**
 * iOS implementation of [rememberDataStore].
 *
 * Creates and remembers a DataStore instance using the iOS app's document directory.
 *
 * @param dataStoreName The name of the DataStore file
 * @return A remembered DataStore<Preferences> instance
 */
@Composable
public actual fun rememberDataStore(dataStoreName: String): DataStore<Preferences> = remember(
    key1 = dataStoreName
) {
    createDataStore(
        dataStoreName = dataStoreName
    )
}
