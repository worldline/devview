package com.worldline.devview.sample.network

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig

public fun <T : HttpClientEngineConfig> baseHttpClientConfig(
    config: HttpClientConfig<T>.() -> Unit = {}
): HttpClientConfig<T>.() -> Unit = {
    baseHttpResponseValidator()
    baseHttpContentNegotiation()
    baseHttpLogging()

    /**
     * OS Specific configuration is applied thanks to this line
     */
    config(this)
}
