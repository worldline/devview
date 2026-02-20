package com.worldline.devview.sample.network

import com.worldline.devview.networkmock.plugin.NetworkMockPlugin
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig

public fun <T : HttpClientEngineConfig> baseHttpClientConfig(
    config: HttpClientConfig<T>.() -> Unit = {}
): HttpClientConfig<T>.() -> Unit = {
    baseHttpResponseValidator()
    baseHttpContentNegotiation()
    baseHttpLogging()

    // Repositories resolved automatically from NetworkMockInitializer
    install(plugin = NetworkMockPlugin)

    /**
     * OS Specific configuration is applied thanks to this line
     */
    config(this)
}
