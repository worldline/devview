package com.worldline.devview.sample.network

import com.worldline.devview.networkmock.repository.MockStateRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

public actual fun createHttpClientWithMocking(
    mockStateRepository: MockStateRepository,
    resourceLoader: suspend (String) -> ByteArray
): HttpClient = HttpClient(
    engineFactory = Darwin,
    block = baseHttpClientConfig(
        mockStateRepository = mockStateRepository,
        resourceLoader = resourceLoader
    ) {
        engine {
            // Optional: Customize Darwin engine settings here
        }
    }
)
