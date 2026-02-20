package com.worldline.devview.sample.network

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

public fun HttpClientConfig<*>.baseHttpContentNegotiation() {
    install(plugin = ContentNegotiation) {
        json(
            json = Json {
                allowStructuredMapKeys = true
                ignoreUnknownKeys = true
                useAlternativeNames = false
            }
        )
    }
}
