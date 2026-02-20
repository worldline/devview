package com.worldline.devview.sample.network

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.worldline.devview.networkmock.repository.MockConfigRepository
import com.worldline.devview.networkmock.repository.MockStateRepository
import devview_root.sample.network.generated.resources.Res
import io.ktor.client.HttpClient

/**
 * Remember an HttpClient with Network Mock support.
 *
 * Creates and remembers an HttpClient configured with the Network Mock plugin.
 * Pass the [mockConfigRepository] from [com.worldline.devview.networkmock.NetworkMock.configRepository]
 * to ensure the plugin and the DevView UI share the same configuration cache (NM-001).
 *
 * @param mockStateRepository The shared MockStateRepository instance
 * @param mockConfigRepository The shared MockConfigRepository instance from NetworkMock
 * @return A remembered HttpClient instance with Network Mock plugin installed
 */
@Composable
public fun rememberHttpClientWithMocking(
    mockStateRepository: MockStateRepository,
    mockConfigRepository: MockConfigRepository
): HttpClient = remember(key1 = mockStateRepository, key2 = mockConfigRepository) {
    createHttpClientWithMocking(
        mockStateRepository = mockStateRepository,
        mockConfigRepository = mockConfigRepository
    )
}
