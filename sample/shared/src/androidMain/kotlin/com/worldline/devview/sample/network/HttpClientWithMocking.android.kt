package com.worldline.devview.sample.network

import com.worldline.devview.networkmock.repository.MockStateRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

public actual fun createHttpClientWithMocking(
    mockStateRepository: MockStateRepository,
    resourceLoader: suspend (String) -> ByteArray
): HttpClient = HttpClient(
    engineFactory = OkHttp,
    block = baseHttpClientConfig(
        mockStateRepository = mockStateRepository,
        resourceLoader = resourceLoader
    ) {
        engine {
            // Optional: Customize OkHttp engine settings here
        }
    }
)
