package com.worldline.devview.sample.network

import com.worldline.devview.networkmock.repository.MockStateRepository
import io.ktor.client.HttpClient

/**
 * Creates an HttpClient with Network Mock support.
 *
 * This function should be called from Composable context where the repository is available.
 *
 * @param mockStateRepository MockStateRepository instance for Network Mock state persistence
 * @param resourceLoader Function to load resource bytes from a path
 * @return Configured HttpClient with Network Mock plugin installed
 */
public expect fun createHttpClientWithMocking(
    mockStateRepository: MockStateRepository,
    resourceLoader: suspend (String) -> ByteArray
): HttpClient
