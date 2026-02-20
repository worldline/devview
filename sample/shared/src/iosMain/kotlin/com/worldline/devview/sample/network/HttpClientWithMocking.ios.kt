package com.worldline.devview.sample.network

import com.worldline.devview.networkmock.repository.MockConfigRepository
import com.worldline.devview.networkmock.repository.MockStateRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

public actual fun createHttpClientWithMocking(
    mockStateRepository: MockStateRepository,
    mockConfigRepository: MockConfigRepository
): HttpClient = HttpClient(
    engineFactory = Darwin,
    block = baseHttpClientConfig(
        mockStateRepository = mockStateRepository,
        mockConfigRepository = mockConfigRepository
    ) {
        engine {
            // Optional: Customize Darwin engine settings here
        }
    }
)
