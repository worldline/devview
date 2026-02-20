package com.worldline.devview.sample.network

import com.worldline.devview.networkmock.repository.MockConfigRepository
import com.worldline.devview.networkmock.repository.MockStateRepository
import io.ktor.client.HttpClient

/**
 * Creates an HttpClient with Network Mock support.
 *
 * This function should be called with the shared [MockConfigRepository] instance
 * from [com.worldline.devview.networkmock.NetworkMock.configRepository] to ensure
 * the plugin and the DevView UI share the same configuration cache.
 *
 * @param mockStateRepository MockStateRepository instance for Network Mock state persistence
 * @param mockConfigRepository Shared MockConfigRepository instance from NetworkMock
 * @return Configured HttpClient with Network Mock plugin installed
 */
public expect fun createHttpClientWithMocking(
    mockStateRepository: MockStateRepository,
    mockConfigRepository: MockConfigRepository
): HttpClient
