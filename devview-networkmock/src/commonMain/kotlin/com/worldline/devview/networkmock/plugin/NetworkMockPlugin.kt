@file:Suppress("StringLiteralDuplication")

package com.worldline.devview.networkmock.plugin

import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Headers
import io.ktor.http.HttpMethod
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.content.OutgoingContent
import io.ktor.util.AttributeKey
import io.ktor.util.Attributes
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import kotlin.coroutines.CoroutineContext

private const val LOG_PREFIX = "[NetworkMock][Plugin]"

/**
 * Plugin configuration wrapper.
 *
 * This class wraps the [NetworkMockConfig] to provide the plugin installation context.
 * It's used internally by the [NetworkMockPlugin] during the prepare phase.
 *
 * @property config The network mock configuration
 */
public data class NetworkMockPluginConfig(internal val config: NetworkMockConfig)

/**
 * Ktor client plugin for intercepting HTTP requests and returning mock responses.
 *
 * This plugin integrates with the DevView Network Mock module to allow developers
 * to mock API calls during development and testing. It intercepts outgoing HTTP
 * requests and can return predefined mock responses instead of making actual
 * network calls.
 *
 * ## Features
 * - **Request Interception**: Intercepts HTTP requests at the send phase
 * - **Selective Mocking**: Mock individual endpoints while others use real network
 * - **Global Toggle**: Master switch to enable/disable all mocking
 * - **Path Parameters**: Supports path parameters like `/users/{userId}`
 * - **Multiple Hosts**: Can mock different hosts (staging, production, etc.)
 * - **State Persistence**: Mock configuration persists across app restarts
 *
 * ## How It Works
 * 1. Plugin intercepts every HTTP request using Ktor's HttpSend mechanism
 * 2. Checks if global mocking is enabled via DataStore state
 * 3. Attempts to match the request (host, path, method) to a configured endpoint
 * 4. If matched and mock is enabled for that endpoint, loads and returns the mock response
 * 5. Otherwise, proceeds with the actual network call
 *
 * ## Installation
 *
 * ```kotlin
 * val dataStore = createDataStore { ... }
 *
 * val client = HttpClient(OkHttp) {
 *     install(NetworkMockPlugin) {
 *         configPath = "files/networkmocks/mocks.json"
 *         mockRepository = MockConfigRepository(configPath)
 *         stateRepository = MockStateRepository(dataStore)
 *     }
 *
 *     // Other plugins can be installed as usual
 *     install(ContentNegotiation) { ... }
 *     install(Logging) { ... }
 * }
 * ```
 *
 * ## Usage Flow
 *
 * ### 1. Configure Mocks
 * Create `composeResources/files/networkmocks/mocks.json`:
 * ```json
 * {
 *   "hosts": [{
 *     "id": "staging",
 *     "url": "https://staging.api.example.com",
 *     "endpoints": [{
 *       "id": "getUser",
 *       "name": "Get User Profile",
 *       "path": "/api/v1/user/{userId}",
 *       "method": "GET"
 *     }]
 *   }]
 * }
 * ```
 *
 * ### 2. Add Response Files
 * Create response files following the naming convention:
 * ```
 * composeResources/files/networkmocks/responses/getUser/
 * ├── getUser-200.json
 * ├── getUser-404.json
 * └── getUser-500.json
 * ```
 *
 * ### 3. Control via DevView UI
 * Users can enable/disable mocking through the DevView UI screen:
 * - Toggle global mocking on/off
 * - Enable specific endpoint mocks
 * - Select which response to return (200, 404, 500, etc.)
 *
 * ### 4. Make Requests
 * ```kotlin
 * // This request can be mocked if configured
 * val response = client.get("https://staging.api.example.com/api/v1/user/123")
 * ```
 *
 * ## State Management
 * The plugin accesses DataStore state by suspending directly inside Ktor's
 * `HttpSend.intercept` lambda, which is itself a suspend context. No `runBlocking`
 * is required or used. For better performance, consider implementing in-memory
 * caching of state in future versions.
 *
 * ## Error Handling
 * The plugin fails gracefully:
 * - If configuration cannot be loaded → logs error, uses actual network
 * - If response file cannot be loaded → logs error, uses actual network
 * - If state cannot be read → logs error, uses actual network
 * - If any exception occurs → logs error, uses actual network
 *
 * ## Thread Safety
 * The plugin is thread-safe. Multiple requests can be intercepted concurrently
 * without issues. State reads are atomic through DataStore.
 *
 * @see NetworkMockConfig
 * @see com.worldline.devview.networkmock.repository.MockConfigRepository
 * @see com.worldline.devview.networkmock.repository.MockStateRepository
 */
public val NetworkMockPlugin: HttpClientPlugin<NetworkMockConfig, NetworkMockPluginConfig> =
    object : HttpClientPlugin<NetworkMockConfig, NetworkMockPluginConfig> {
        override val key: AttributeKey<NetworkMockPluginConfig> = AttributeKey(
            name = "NetworkMockPlugin"
        )

        override fun prepare(block: NetworkMockConfig.() -> Unit): NetworkMockPluginConfig {
            val config = NetworkMockConfig().apply(block = block)
            return NetworkMockPluginConfig(config = config)
        }

        @Suppress("LongMethod")
        override fun install(plugin: NetworkMockPluginConfig, scope: HttpClient) {
            val mockRepository = plugin.config.mockRepository
            val stateRepository = plugin.config.stateRepository

            println(message = "$LOG_PREFIX NetworkMock plugin installed successfully")

            // Intercept at the HttpSend phase to control whether requests are sent
            scope.plugin(plugin = HttpSend).intercept { requestBuilder ->
                // Extract request details
                val request = requestBuilder.build()
                val host = request.url.host
                val path = request.url.encodedPath
                val method = request.method.value

                println(message = "$LOG_PREFIX ========================================")
                println(message = "$LOG_PREFIX Intercepted request: $method $host$path")

                // Get current mock state from DataStore
                val currentState = stateRepository.getState()

                // If global mocking is disabled, proceed with actual network call
                if (!currentState.globalMockingEnabled) {
                    println(
                        message = "$LOG_PREFIX Global mocking is DISABLED - using actual network"
                    )
                    println(message = "$LOG_PREFIX ========================================")
                    return@intercept execute(requestBuilder = requestBuilder)
                }

                println(message = "$LOG_PREFIX Global mocking is ENABLED - checking for mock")

                // Try to find a matching mock configuration
                val mockMatch = mockRepository.findMatchingMock(
                    host = host,
                    path = path,
                    method = method
                )

                // If we found a matching endpoint in config
                mockMatch?.let { match ->
                    println(
                        message = "$LOG_PREFIX Found matching endpoint: ${match.hostId}-${match.endpointId}"
                    )

                    val endpointKey = "${match.hostId}-${match.endpointId}"
                    val endpointState = currentState.endpointStates[endpointKey]

                    if (endpointState == null) {
                        println(
                            message = "$LOG_PREFIX No state found for endpoint key: $endpointKey"
                        )
                        println(
                            message = "$LOG_PREFIX Available endpoint states: ${currentState.endpointStates.keys}"
                        )
                        println(message = "$LOG_PREFIX Using actual network")
                        println(message = "$LOG_PREFIX ========================================")
                        return@intercept execute(requestBuilder = requestBuilder)
                    }

                    println(
                        message =
                        "$LOG_PREFIX Endpoint state: enabled=${endpointState.mockEnabled}, " +
                            "file=${endpointState.selectedResponseFile}"
                    )

                    // Check if this specific endpoint has mocking enabled and a response selected
                    if (endpointState.shouldUseMock()) {
                        println(
                            message = "$LOG_PREFIX Mock is enabled with file: " +
                                "${endpointState.selectedResponseFile}"
                        )

                        @Suppress("TooGenericExceptionCaught")
                        try {
                            // Load the mock response from file
                            val mockResponse = endpointState.selectedResponseFile?.let {
                                mockRepository.loadMockResponse(
                                    endpointId = match.endpointId,
                                    fileName = it
                                )
                            }

                            mockResponse?.let { response ->
                                println(
                                    message = "$LOG_PREFIX Successfully loaded mock response " +
                                        "(status ${response.statusCode})"
                                )
                                println(
                                    message = "$LOG_PREFIX Returning MOCK response - " +
                                        "NO network call will be made"
                                )
                                println(
                                    message = "$LOG_PREFIX ========================================"
                                )

                                // Create and return a mock HttpClientCall without making the actual network request
                                return@intercept createMockHttpClientCall(
                                    client = scope,
                                    requestData = request,
                                    statusCode = HttpStatusCode.fromValue(
                                        value = response.statusCode
                                    ),
                                    content = response.content
                                )
                            }

                            if (mockResponse == null) {
                                println(message = "$LOG_PREFIX ERROR: Mock response loaded as null")
                                println(message = "$LOG_PREFIX Falling back to actual network")
                                println(
                                    message = "$LOG_PREFIX ========================================"
                                )
                            }
                        } catch (e: Exception) {
                            println(
                                message = "$LOG_PREFIX ERROR: Exception loading mock response - ${e.message}"
                            )
                            e.printStackTrace()
                            println(message = "$LOG_PREFIX Falling back to actual network")
                            println(
                                message = "$LOG_PREFIX ========================================"
                            )
                            // Fall through to actual network call on error
                        }
                    } else {
                        println(
                            message = "$LOG_PREFIX Endpoint mock not enabled or no response selected"
                        )
                        println(
                            message = "$LOG_PREFIX   shouldUseMock() = ${endpointState.shouldUseMock()}"
                        )
                        println(message = "$LOG_PREFIX Using actual network")
                        println(message = "$LOG_PREFIX ========================================")
                    }
                }

                if (mockMatch == null) {
                    println(message = "$LOG_PREFIX No matching endpoint config found")
                    println(message = "$LOG_PREFIX Using actual network")
                }

                // No mock matched or not enabled - proceed with actual network call
                println(
                    message = "$LOG_PREFIX No mock enabled for $method $path, using actual network"
                )
                println(message = "$LOG_PREFIX ========================================")
                execute(requestBuilder = requestBuilder)
            }
        }
    }

/**
 * Creates a mock [HttpClientCall] without making an actual network request.
 *
 * This function constructs a complete [HttpClientCall] object that mimics a
 * real HTTP response but without any network activity. It's used by the plugin
 * to return mock responses.
 *
 * ## Implementation Details
 * The function uses Ktor's HttpClientCall constructor with properly formatted
 * HttpRequestData and HttpResponseData objects.
 *
 * @param client The HttpClient instance
 * @param requestData The original request data
 * @param statusCode The HTTP status code for the mock response
 * @param content The response body content as a string
 * @return A mock [HttpClientCall] that appears as a real HTTP call to the application
 */
@Suppress("CommentOverPrivateFunction")
private fun createMockHttpClientCall(
    client: HttpClient,
    requestData: HttpRequestData,
    statusCode: HttpStatusCode,
    content: String
): HttpClientCall {
    val responseData = HttpResponseData(
        statusCode = statusCode,
        requestTime = GMTDate(),
        headers = Headers.Empty,
        version = HttpProtocolVersion.HTTP_1_1,
        body = ByteReadChannel(content = content.encodeToByteArray()),
        callContext = requestData.executionContext
    )

    return MockHttpClientCall(
        client = client,
        mockRequestData = requestData,
        mockResponseData = responseData
    )
}

public class MockHttpClientCall(client: HttpClient) : HttpClientCall(client) {
    public constructor(
        client: HttpClient,
        mockRequestData: HttpRequestData,
        mockResponseData: HttpResponseData
    ) : this(client = client) {
        request = object : HttpRequest {
            override val call: HttpClientCall
                get() = this@MockHttpClientCall
            override val method: HttpMethod
                get() = mockRequestData.method
            override val url: Url
                get() = mockRequestData.url
            override val attributes: Attributes
                get() = mockRequestData.attributes
            override val content: OutgoingContent
                get() = mockRequestData.body
            override val headers: Headers
                get() = mockRequestData.headers
            override val coroutineContext: CoroutineContext
                get() = super.coroutineContext
        }
        response = object : HttpResponse() {
            override val call: HttpClientCall
                get() = this@MockHttpClientCall
            override val status: HttpStatusCode
                get() = mockResponseData.statusCode
            override val version: HttpProtocolVersion
                get() = mockResponseData.version
            override val requestTime: GMTDate
                get() = mockResponseData.requestTime
            override val responseTime: GMTDate
                get() = mockResponseData.responseTime

            @InternalAPI
            override val rawContent: ByteReadChannel
                get() = mockResponseData.body as? ByteReadChannel
                    ?: ByteReadChannel.Empty
            override val headers: Headers
                get() = mockResponseData.headers
            override val coroutineContext: CoroutineContext
                get() = mockResponseData.callContext
        }

        attributes.remove(key = AttributeKey(name = "CustomResponse"))
        if (mockResponseData.body !is ByteReadChannel) {
            attributes.put(
                key = AttributeKey(name = "CustomResponse"),
                value = mockResponseData.body
            )
        }
    }
}
