package com.worldline.devview.sample.network

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.worldline.devview.networkmock.repository.MockStateRepository
import com.worldline.devview.utils.rememberDataStore

private const val NETWORK_MOCK_DATASTORE_NAME = "network_mock_datastore.preferences_pb"

/**
 * Remember a shared MockStateRepository instance.
 *
 * This creates a single MockStateRepository that can be shared between
 * the HttpClient plugin and the DevView UI to ensure they read/write
 * from the same repository instance.
 *
 * @return A remembered MockStateRepository instance
 */
@Composable
public fun rememberMockStateRepository(): MockStateRepository {
    val dataStore = rememberDataStore(dataStoreName = NETWORK_MOCK_DATASTORE_NAME)
    return remember(key1 = dataStore) {
        MockStateRepository(dataStore = dataStore)
    }
}
