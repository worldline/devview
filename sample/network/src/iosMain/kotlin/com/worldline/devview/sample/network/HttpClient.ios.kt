package com.worldline.devview.sample.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

public actual val httpClient: HttpClient = HttpClient(
    Darwin,
    baseHttpClientConfig {
        engine {
            // Configure the Darwin engine if needed
        }
    }
)
