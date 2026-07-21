package com.worldline.devview.networkmock.core

/**
 * Functional interface for loading resource bytes from a path.
 *
 * Implement this to bridge your module's Compose Resources to the network mock engine:
 * ```kotlin
 * NetworkMock(resourceLoader = NetworkMockResourceLoader { path -> Res.readBytes(path) })
 * ```
 *
 * With a DI framework, register an implementation in the module that owns the mock files and
 * inject it where `NetworkMock` is constructed:
 * ```kotlin
 * // In your data module's DI setup:
 * single<NetworkMockResourceLoader> { NetworkMockResourceLoader { Res.readBytes(it) } }
 *
 * // In your presentation module:
 * val loader = koinInject<NetworkMockResourceLoader>()
 * NetworkMock(resourceLoader = loader)
 * ```
 */
public fun interface NetworkMockResourceLoader {
    public suspend fun load(path: String): ByteArray
}
