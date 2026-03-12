@file:Suppress("StringLiteralDuplication")

package com.worldline.devview.networkmock.plugin

import com.worldline.devview.networkmock.model.EndpointMockState
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
 * Wraps [NetworkMockConfig] to provide the plugin installation context.
 * Used internally by [NetworkMockPlugin] during the prepare phase.
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
 * 1. Plugin intercepts every HTTP request using Ktor's `HttpSend` mechanism
 * 2. Checks if global mocking is enabled via DataStore state
 * 3. Attempts to match the request (host, path, method) to a configured endpoint
 * 4. If matched and mock is enabled for that endpoint, loads and returns the mock response
 * 5. Otherwise, proceeds with the actual network call
 *
 * ## Default Installation
 * When `NetworkMock` is registered via `rememberModules`, repositories are resolved
 * automatically from [com.worldline.devview.networkmock.NetworkMockInitializer]:
 * ```kotlin
 * val client = HttpClient(OkHttp) {
 *     install(NetworkMockPlugin)
 * }
 * ```
 *
 * ## Custom Installation
 * For testing or advanced scenarios, repositories can be injected explicitly:
 * ```kotlin
 * val client = HttpClient(OkHttp) {
 *     install(NetworkMockPlugin) {
 *         mockRepository = myMockConfigRepository
 *         stateRepository = myMockStateRepository
 *     }
 * }
 * ```
 *
 * ## Mock File Setup
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
 * Add response files following the naming convention:
 * ```
 * composeResources/files/networkmocks/responses/getUser/
 * ├── getUser-200.json
 * ├── getUser-404.json
 * └── getUser-500.json
 * ```
 *
 * ## Error Handling
 * The plugin fails gracefully — if configuration cannot be loaded, a response
 * file is missing, or any exception occurs, it falls back to the actual network
 * and logs the reason.
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
            val mockRepository = plugin.config.resolvedMockRepository()
            val stateRepository = plugin.config.resolvedStateRepository()

            println(message = "$LOG_PREFIX NetworkMock plugin installed successfully")

            scope.plugin(plugin = HttpSend).intercept { requestBuilder ->
                val request = requestBuilder.build()
                val host = request.url.host
                val path = request.url.encodedPath
                val method = request.method.value

                println(message = "$LOG_PREFIX ========================================")
                println(message = "$LOG_PREFIX Intercepted request: $method $host$path")

                val currentState = stateRepository.getState()

                if (!currentState.globalMockingEnabled) {
                    println(
                        message = "$LOG_PREFIX Global mocking is DISABLED - using actual network"
                    )
                    println(message = "$LOG_PREFIX ========================================")
                    return@intercept execute(requestBuilder = requestBuilder)
                }

                println(message = "$LOG_PREFIX Global mocking is ENABLED - checking for mock")

                val mockMatch = mockRepository.findMatchingMock(
                    host = host,
                    path = path,
                    method = method
                )

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
                            "$LOG_PREFIX Endpoint state: ${
                                when (endpointState) {
                                    is EndpointMockState.Network -> "network"
                                    is EndpointMockState.Mock ->
                                        "mock, file=${endpointState.responseFile}, " +
                                            "status=${endpointState.statusCode}"
                                }
                            }"
                    )

                    when (endpointState) {
                        is EndpointMockState.Network -> {
                            println(
                                message = "$LOG_PREFIX Endpoint mock not enabled"
                            )
                            println(message = "$LOG_PREFIX Using actual network")
                            println(
                                message = "$LOG_PREFIX ========================================"
                            )
                        }
                        is EndpointMockState.Mock -> {
                            println(
                                message = "$LOG_PREFIX Mock is enabled with file: " +
                                    endpointState.responseFile
                            )

                            @Suppress("TooGenericExceptionCaught")
                            try {
                                val mockResponse = mockRepository.loadMockResponse(
                                    endpointId = match.endpointId,
                                    fileName = endpointState.responseFile
                                )

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
                                    println(
                                        message = "$LOG_PREFIX ERROR: Mock response loaded as null"
                                    )
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
                            }
                        }
                    }
                }

                if (mockMatch == null) {
                    println(message = "$LOG_PREFIX No matching endpoint config found")
                    println(message = "$LOG_PREFIX Using actual network")
                }

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
 * Constructs a complete [HttpClientCall] that mimics a real HTTP response but
 * without any network activity. Used by the plugin to return mock responses.
 *
 * @param client The [HttpClient] instance
 * @param requestData The original request data
 * @param statusCode The HTTP status code for the mock response
 * @param content The response body content as a string
 * @return A mock [HttpClientCall] that appears as a real HTTP call to the application
 */
@Suppress("DocumentationOverPrivateFunction")
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

/**
 * A mock implementation of [HttpClientCall] that returns a predefined response
 * without making an actual network request.
 *
 * Constructed by [createMockHttpClientCall] with the original request data and
 * a synthetic response. Both [request] and [response] are set immediately in
 * the secondary constructor so the call is fully usable upon creation.
 */
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
