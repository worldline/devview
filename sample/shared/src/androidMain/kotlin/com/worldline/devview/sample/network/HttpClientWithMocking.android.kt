package com.worldline.devview.sample.network

import com.worldline.devview.networkmock.repository.MockConfigRepository
import com.worldline.devview.networkmock.repository.MockStateRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

public actual fun createHttpClientWithMocking(
    mockStateRepository: MockStateRepository,
    mockConfigRepository: MockConfigRepository
): HttpClient = HttpClient(
    engineFactory = OkHttp,
    block = baseHttpClientConfig(
        mockStateRepository = mockStateRepository,
        mockConfigRepository = mockConfigRepository
    ) {
        engine {
            // Optional: Customize OkHttp engine settings here
        }
    }
)
