package com.worldline.devview.networkmock.ktor.plugin

import com.worldline.devview.networkmock.core.NetworkMockInitializer
import com.worldline.devview.networkmock.core.repository.MockConfigRepository
import com.worldline.devview.networkmock.core.repository.MockStateRepository

/**
 * Configuration class for the [NetworkMockPlugin].
 *
 * Holds optional overrides for the repositories used by the plugin. When not
 * explicitly set, both repositories are resolved automatically from
 * [NetworkMockInitializer] — which is initialised by `devview-networkmock`
 * when `NetworkMock` is registered via `rememberModules { }`.
 *
 * ## Default usage — zero configuration needed
 * When `NetworkMock` is registered via `rememberModules`, [NetworkMockInitializer]
 * is populated automatically. The plugin can then be installed with no configuration:
 * ```kotlin
 * val client = HttpClient(OkHttp) {
 *     install(NetworkMockPlugin)
 * }
 * ```
 *
 * ## Custom usage — explicit repository injection
 * For testing or advanced scenarios, repositories can be provided explicitly,
 * bypassing [NetworkMockInitializer] entirely:
 * ```kotlin
 * val client = HttpClient(OkHttp) {
 *     install(NetworkMockPlugin) {
 *         mockRepository = myMockConfigRepository
 *         stateRepository = myMockStateRepository
 *     }
 * }
 * ```
 *
 * @see NetworkMockPlugin
 * @see NetworkMockInitializer
 * @see MockConfigRepository
 * @see MockStateRepository
 */
public class NetworkMockConfig {
    /**
     * Optional override for the mock configuration repository.
     *
     * When `null` (the default), the repository is resolved from
     * [NetworkMockInitializer.requireConfigRepository]. Set this explicitly
     * for testing or when not using `rememberModules`.
     */
    public var mockRepository: MockConfigRepository? = null

    /**
     * Optional override for the mock state repository.
     *
     * When `null` (the default), the repository is resolved from
     * [NetworkMockInitializer.requireStateRepository]. Set this explicitly
     * for testing or when not using `rememberModules`.
     */
    public var stateRepository: MockStateRepository? = null

    /**
     * Resolves the [MockConfigRepository] to use, falling back to
     * [NetworkMockInitializer.requireConfigRepository] if not explicitly set.
     */
    internal fun resolvedMockRepository(): MockConfigRepository =
        mockRepository ?: NetworkMockInitializer.requireConfigRepository()

    /**
     * Resolves the [MockStateRepository] to use, falling back to
     * [NetworkMockInitializer.requireStateRepository] if not explicitly set.
     */
    internal fun resolvedStateRepository(): MockStateRepository =
        stateRepository ?: NetworkMockInitializer.requireStateRepository()
}
