package com.worldline.devview.sample.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

public actual fun createHttpClientWithMocking(): HttpClient = HttpClient(
    engineFactory = OkHttp,
    block = baseHttpClientConfig {
        engine {
            // Optional: Customize OkHttp engine settings here
        }
    }
)
