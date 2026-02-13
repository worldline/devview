package com.worldline.devview.networkmock.plugin

import com.worldline.devview.networkmock.repository.MockConfigRepository
import com.worldline.devview.networkmock.repository.MockStateRepository

/**
 * Configuration class for the Network Mock plugin.
 *
 * This class holds the configuration needed by the [NetworkMockPlugin] to
 * intercept HTTP requests and return mock responses. It allows integrators
 * to customize the plugin behavior and provide necessary dependencies.
 *
 * ## Properties
 * - **configPath**: Path to the mocks.json file in Compose Resources
 * - **mockRepository**: Repository for loading mock configurations and response files
 * - **stateRepository**: Repository for persisting mock state to DataStore
 *
 * ## Usage
 *
 * ### Basic Setup
 * ```kotlin
 * val dataStore = createDataStore { ... }
 *
 * val client = HttpClient(OkHttp) {
 *     install(NetworkMockPlugin) {
 *         configPath = "files/networkmocks/mocks.json"
 *         mockRepository = MockConfigRepository(configPath)
 *         stateRepository = MockStateRepository(dataStore)
 *     }
 * }
 * ```
 *
 * ### With Custom Configuration Path
 * ```kotlin
 * val client = HttpClient(OkHttp) {
 *     install(NetworkMockPlugin) {
 *         configPath = "files/custom/path/mocks.json"
 *         mockRepository = MockConfigRepository(configPath)
 *         stateRepository = MockStateRepository(dataStore)
 *     }
 * }
 * ```
 *
 * ## Repository Initialization
 * The repositories must be explicitly provided by the integrator to ensure:
 * - Proper dependency injection if using Koin or similar
 * - Testability (can inject mock repositories for testing)
 * - Flexibility (integrator controls lifecycle and scope)
 *
 * ## Default Values
 * - **configPath**: Defaults to `"files/networkmocks/mocks.json"`
 * - **mockRepository** and **stateRepository**: Must be set by integrator (lateinit)
 *
 * @see NetworkMockPlugin
 * @see MockConfigRepository
 * @see MockStateRepository
 */
public class NetworkMockConfig {
    /**
     * Path to the mock configuration file relative to composeResources.
     *
     * This should point to the `mocks.json` file in your project's resources.
     * The default path assumes the standard convention:
     * `composeResources/files/networkmocks/mocks.json`
     *
     * ## Example Paths
     * - Default: `"files/networkmocks/mocks.json"`
     * - Custom: `"files/custom/mocks.json"`
     * - Environment-specific: `"files/networkmocks/staging-mocks.json"`
     */
    public var configPath: String = "files/networkmocks/mocks.json"

    /**
     * Repository for loading mock configurations and response files.
     *
     * This repository handles:
     * - Loading the mocks.json configuration
     * - Discovering available response files
     * - Matching HTTP requests to configured endpoints
     * - Loading response file contents
     *
     * Must be initialized before the plugin is used.
     *
     * ## Initialization
     * ```kotlin
     * install(NetworkMockPlugin) {
     *     mockRepository = MockConfigRepository(configPath)
     * }
     * ```
     */
    public lateinit var mockRepository: MockConfigRepository

    /**
     * Repository for persisting and retrieving mock state.
     *
     * This repository handles:
     * - Persisting the global mocking toggle
     * - Persisting individual endpoint mock states
     * - Providing reactive state updates for the UI
     * - Providing one-time state reads for the plugin
     *
     * Must be initialized with a DataStore instance before the plugin is used.
     *
     * ## Initialization
     * ```kotlin
     * val dataStore = createDataStore { ... }
     *
     * install(NetworkMockPlugin) {
     *     stateRepository = MockStateRepository(dataStore)
     * }
     * ```
     */
    public lateinit var stateRepository: MockStateRepository
}
