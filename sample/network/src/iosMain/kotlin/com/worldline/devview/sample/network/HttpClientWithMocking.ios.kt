package com.worldline.devview.sample.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

public actual fun createHttpClientWithMocking(): HttpClient = HttpClient(
    engineFactory = Darwin,
    block = baseHttpClientConfig {
        engine {
            // Optional: Customize Darwin engine settings here
        }
    }
)
