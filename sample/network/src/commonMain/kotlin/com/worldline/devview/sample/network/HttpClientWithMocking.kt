package com.worldline.devview.sample.network

import io.ktor.client.HttpClient

/**
 * Creates an HttpClient with Network Mock support.
 *
 * Repositories are resolved automatically from
 * [com.worldline.devview.networkmock.NetworkMockInitializer] via
 * [com.worldline.devview.networkmock.plugin.NetworkMockPlugin]'s default
 * configuration — no manual wiring required.
 *
 * @return Configured HttpClient with Network Mock plugin installed
 */
public expect fun createHttpClientWithMocking(): HttpClient
