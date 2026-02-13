package com.worldline.devview.sample.network

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.worldline.devview.networkmock.repository.MockStateRepository
import devview_root.sample.shared.generated.resources.Res
import io.ktor.client.HttpClient
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Remember an HttpClient with Network Mock support.
 *
 * Creates and remembers an HttpClient configured with the Network Mock plugin.
 * Uses the provided MockStateRepository to ensure the same instance is shared
 * between the HttpClient and the DevView UI.
 *
 * This allows the HttpClient to intercept requests and return mock responses
 * based on the configuration in the DevView Network Mock UI.
 *
 * @param mockStateRepository The shared MockStateRepository instance
 * @return A remembered HttpClient instance with Network Mock plugin installed
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
public fun rememberHttpClientWithMocking(
    mockStateRepository: MockStateRepository = rememberMockStateRepository()
): HttpClient = remember(key1 = mockStateRepository) {
    createHttpClientWithMocking(
        mockStateRepository = mockStateRepository,
        resourceLoader = { path -> Res.readBytes(path = path) }
    )
}
