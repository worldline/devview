package com.worldline.devview.sample.network

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.ktor.client.HttpClient

/**
 * Remember an HttpClient with Network Mock support.
 *
 * Creates and remembers an HttpClient configured with the Network Mock plugin.
 * Repositories are resolved automatically from
 * [com.worldline.devview.networkmock.NetworkMockInitializer] — no manual
 * wiring required as long as [com.worldline.devview.networkmock.NetworkMock]
 * is registered via `rememberModules { }`.
 *
 * @return A remembered HttpClient instance with Network Mock plugin installed
 */
@Composable
public fun rememberHttpClientWithMocking(): HttpClient = remember {
    createHttpClientWithMocking()
}
