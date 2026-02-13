package com.worldline.devview.sample.network

import com.worldline.devview.networkmock.plugin.NetworkMockPlugin
import com.worldline.devview.networkmock.repository.MockConfigRepository
import com.worldline.devview.networkmock.repository.MockStateRepository
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig

public fun <T : HttpClientEngineConfig> baseHttpClientConfig(
    mockStateRepository: MockStateRepository? = null,
    resourceLoader: (suspend (String) -> ByteArray)? = null,
    config: HttpClientConfig<T>.() -> Unit = {}
): HttpClientConfig<T>.() -> Unit = {
    baseHttpResponseValidator()
    baseHttpContentNegotiation()
    baseHttpLogging()

    // Install Network Mock plugin if both repository and resourceLoader are provided
    if (mockStateRepository != null && resourceLoader != null) {
        install(plugin = NetworkMockPlugin) {
            configPath = "files/networkmocks/mocks.json"
            mockRepository = MockConfigRepository(
                configPath = configPath,
                resourceLoader = resourceLoader
            )
            stateRepository = mockStateRepository
        }
    }

    /**
     * OS Specific configuration is applied thanks to this line
     */
    config(this)
}
