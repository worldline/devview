package com.worldline.devview.networkmock.model

import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.worldline.devview.utils.rememberDataStore

/**
 * The filename used for the network mock DataStore preferences file.
 */
internal const val NETWORK_MOCK_DATASTORE_NAME: String = "network_mock_datastore.preferences_pb"

/**
 * Composable that remembers and returns a DataStore instance for network mock persistence.
 *
 * This delegates to the platform-specific implementation in devview-utils, providing
 * the network mock specific DataStore filename.
 *
 * @return A remembered DataStore<Preferences> instance for network mock persistence
 */
@Composable
internal fun rememberDataStore(): DataStore<Preferences> = rememberDataStore(
    dataStoreName = NETWORK_MOCK_DATASTORE_NAME
)
