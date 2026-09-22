package com.worldline.devview.networkmock.ktor.plugin

import co.touchlab.kermit.Logger
import com.worldline.devview.networkmock.core.model.FailureKind
import com.worldline.devview.networkmock.core.model.NetworkMockState
import com.worldline.devview.networkmock.core.model.OperationKey
import com.worldline.devview.networkmock.core.model.OperationMockState
import com.worldline.devview.networkmock.core.repository.MockConfigRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Headers
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
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
import kotlin.text.get
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.io.IOException

private val logger = Logger.withTag(tag = "DevViewNetworkMock")

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
 * - **Failure Simulation**: An operation can deterministically simulate a network failure
 *   (see [com.worldline.devview.networkmock.core.model.OperationMockState.Failure]), or fail a
 *   configurable percentage of the time via `x-devview.failureRate`
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
 * automatically from [com.worldline.devview.networkmock.core.NetworkMockInitializer]:
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
 * Create an OpenAPI 3.x spec, e.g. `composeResources/files/networkmocks/specs/my-backend.json`:
 * ```json
 * {
 *   "info": { "title": "My Backend" },
 *   "servers": [{ "url": "https://api.example.com" }],
 *   "paths": {
 *     "/v1/users/{userId}": {
 *       "get": {
 *         "operationId": "getUser",
 *         "summary": "Get User Profile",
 *         "responses": {
 *           "200": {
 *             "content": {
 *               "application/json": {
 *                 "examples": {
 *                   "default": { "externalValue": "responses/getUser-200.json" }
 *                 }
 *               }
 *             }
 *           }
 *         }
 *       }
 *     }
 *   }
 * }
 * ```
 *
 * ## Error Handling
 * The plugin fails gracefully — if configuration cannot be loaded, a response
 * file is missing, or any exception occurs while loading a declared mock, it falls back to the
 * actual network and logs the reason. The one deliberate exception: a simulated failure (a
 * `Failure` state, or a `failureRate` roll) throws intentionally, mirroring what a real network
 * failure looks like to the app — that's the point, not an error to recover from.
 *
 * ## Thread Safety
 * The plugin is thread-safe. Multiple requests can be intercepted concurrently
 * without issues. State reads are atomic through DataStore.
 *
 * @see NetworkMockConfig
 * @see com.worldline.devview.networkmock.core.repository.MockConfigRepository
 * @see com.worldline.devview.networkmock.core.repository.MockStateRepository
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

        @Suppress("LongMethod", "ThrowsCount")
        override fun install(plugin: NetworkMockPluginConfig, scope: HttpClient) {
            val mockRepository = plugin.config.resolvedMockRepository()
            val stateRepository = plugin.config.resolvedStateRepository()
            val random = plugin.config.random

            logger.d { "NetworkMock plugin installed" }

            val cachedState = MutableStateFlow<NetworkMockState?>(value = null)
            scope.launch {
                stateRepository.observeState().collect { state ->
                    cachedState.value = state
                }
            }

            scope.plugin(plugin = HttpSend).intercept { requestBuilder ->
                val request = requestBuilder.build()
                val host = request.url.host
                val path = request.url.encodedPath
                val method = request.method.value
                val queryParameters = request.url.parameters
                    .entries()
                    .associate { (key, values) -> key to values }

                val currentState = cachedState.value ?: stateRepository.getState()

                if (!currentState.globalMockingEnabled) {
                    logger.d { "$method $path -> NETWORK (global mocking disabled)" }
                    return@intercept execute(requestBuilder = requestBuilder)
                }

                val mockMatch = mockRepository.findMatchingMock(
                    host = host,
                    path = path,
                    method = method,
                    queryParameters = queryParameters
                )

                if (mockMatch == null) {
                    logger.d { "$method $path -> NETWORK (no operation match)" }
                    return@intercept execute(requestBuilder = requestBuilder)
                }

                val endpointState = currentState.getOperationState(key = mockMatch.key)

                if (endpointState == null) {
                    logger.d {
                        "$method $path -> NETWORK (${mockMatch.key.compositeKey} has no configured state)"
                    }
                    return@intercept execute(requestBuilder = requestBuilder)
                }

                when (endpointState) {
                    is OperationMockState.Network -> {
                        logger.d { "$method $path -> NETWORK (operation set to pass-through)" }
                        execute(requestBuilder = requestBuilder)
                    }
                    is OperationMockState.Failure -> {
                        logger.d { "$method $path -> FAILURE (${endpointState.kind}, forced)" }
                        throw simulatedFailure(kind = endpointState.kind, requestData = request)
                    }
                    is OperationMockState.Mock -> {
                        val failureRate = mockMatch.config.failureRate
                        if (failureRate != null && random.nextDouble() < failureRate) {
                            logger.d {
                                "$method $path -> FAILURE (probabilistic, rate=$failureRate)"
                            }
                            throw simulatedFailure(
                                kind = FailureKind.CONNECTION_REFUSED,
                                requestData = request
                            )
                        }

                        @Suppress("TooGenericExceptionCaught")
                        try {
                            val call = buildMockCall(
                                mockRepository = mockRepository,
                                client = scope,
                                request = request,
                                key = mockMatch.key,
                                statusCode = endpointState.statusCode,
                                exampleName = endpointState.exampleName,
                                delayMs = mockMatch.delayMs
                            )

                            if (call == null) {
                                logger.w {
                                    "$method $path -> NETWORK (declared mock " +
                                        "${endpointState.statusCode}/${endpointState.exampleName} not found)"
                                }
                                return@intercept execute(requestBuilder = requestBuilder)
                            }

                            logger.d {
                                "$method $path -> MOCK ${endpointState.statusCode}/${endpointState.exampleName}"
                            }
                            call
                        } catch (e: Exception) {
                            logger.w(
                                throwable = e
                            ) { "$method $path -> NETWORK (error loading mock response)" }
                            execute(requestBuilder = requestBuilder)
                        }
                    }
                    is OperationMockState.Sequence -> {
                        val step = endpointState.currentResponse
                        if (step == null) {
                            logger.w { "$method $path -> NETWORK (sequence has no declared steps)" }
                            return@intercept execute(requestBuilder = requestBuilder)
                        }

                        val failureRate = mockMatch.config.failureRate
                        if (failureRate != null && random.nextDouble() < failureRate) {
                            logger.d {
                                "$method $path -> FAILURE (probabilistic, rate=$failureRate)"
                            }
                            throw simulatedFailure(
                                kind = FailureKind.CONNECTION_REFUSED,
                                requestData = request
                            )
                        }

                        @Suppress("TooGenericExceptionCaught")
                        try {
                            val call = buildMockCall(
                                mockRepository = mockRepository,
                                client = scope,
                                request = request,
                                key = mockMatch.key,
                                statusCode = step.statusCode,
                                exampleName = step.exampleName,
                                delayMs = mockMatch.delayMs
                            )

                            if (call == null) {
                                logger.w {
                                    "$method $path -> NETWORK (declared sequence step " +
                                        "${step.statusCode}/${step.exampleName} not found)"
                                }
                                return@intercept execute(requestBuilder = requestBuilder)
                            }

                            // Advance and persist before returning - sticks on the last index
                            // once exhausted rather than looping back to the start.
                            val nextIndex = (endpointState.currentIndex + 1)
                                .coerceAtMost(maximumValue = endpointState.responses.lastIndex)
                            if (nextIndex != endpointState.currentIndex) {
                                stateRepository.setOperationMockState(
                                    key = mockMatch.key,
                                    state = endpointState.copy(currentIndex = nextIndex)
                                )
                            }

                            logger.d {
                                "$method $path -> MOCK ${step.statusCode}/${step.exampleName} " +
                                    "(sequence ${endpointState.currentIndex + 1}/${endpointState.responses.size})"
                            }
                            call
                        } catch (e: Exception) {
                            logger.w(
                                throwable = e
                            ) { "$method $path -> NETWORK (error loading sequence step)" }
                            execute(requestBuilder = requestBuilder)
                        }
                    }
                }
            }
        }
    }

/**
 * Builds the [Throwable] to throw for a simulated [FailureKind], mirroring what a real Ktor
 * HTTP engine throws for the equivalent real condition so an app's existing error handling
 * exercises the same code path against the simulated failure as it would the real one.
 *
 * `NamedArguments` is suppressed below because on the JVM target, [IOException] is a plain
 * `java.io.IOException` constructor with no retained parameter name to reference.
 *
 * @param kind Which failure to simulate
 * @param requestData The original request data, used to build a realistic timeout exception
 * @return The exception to throw — never returns normally, the caller always `throw`s the result
 */
@Suppress("DocumentationOverPrivateFunction", "NamedArguments")
private fun simulatedFailure(kind: FailureKind, requestData: HttpRequestData): Throwable =
    when (kind) {
        FailureKind.TIMEOUT -> HttpRequestTimeoutException(request = requestData)
        FailureKind.CONNECTION_REFUSED ->
            IOException("Connection refused (simulated by DevView NetworkMock)")
    }

/**
 * Loads the declared `(statusCode, exampleName)` variant and builds a mock [HttpClientCall] for
 * it, applying [delayMs] first — shared by the [OperationMockState.Mock] and
 * [OperationMockState.Sequence] branches, which differ only in where the pair comes from.
 *
 * @return The call to return, or `null` if the variant isn't declared in the spec (caller falls
 *   back to the real network).
 */
@Suppress("DocumentationOverPrivateFunction")
private suspend fun buildMockCall(
    mockRepository: MockConfigRepository,
    client: HttpClient,
    request: HttpRequestData,
    key: OperationKey,
    statusCode: Int,
    exampleName: String,
    delayMs: Long?
): HttpClientCall? {
    val mockResponse = mockRepository.loadMockResponse(
        key = key,
        statusCode = statusCode,
        exampleName = exampleName
    ) ?: return null
    delayMs?.let { ms -> delay(timeMillis = ms) }
    return createMockHttpClientCall(
        client = client,
        requestData = request,
        statusCode = HttpStatusCode.fromValue(value = mockResponse.statusCode),
        content = mockResponse.content,
        contentType = mockResponse.contentType,
        headers = mockResponse.headers
    )
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
 * @param contentType The response's declared media type
 *   (see [com.worldline.devview.networkmock.core.model.MockResponse.contentType])
 * @param headers Additional headers declared on the response (see
 *   [com.worldline.devview.networkmock.core.model.MockResponse.headers]) — merged over the
 *   [contentType]-derived `Content-Type`, not replacing it, unless the spec explicitly
 *   declares its own `Content-Type` header, which then wins.
 * @return A mock [HttpClientCall] that appears as a real HTTP call to the application
 */
@Suppress("DocumentationOverPrivateFunction")
private fun createMockHttpClientCall(
    client: HttpClient,
    requestData: HttpRequestData,
    statusCode: HttpStatusCode,
    content: String,
    contentType: String,
    headers: Map<String, String>
): HttpClientCall {
    val responseData = HttpResponseData(
        statusCode = statusCode,
        requestTime = GMTDate(),
        headers = HeadersBuilder()
            .apply {
                set(name = HttpHeaders.ContentType, value = contentType)
                headers.forEach { (name, value) -> set(name = name, value = value) }
            }.build(),
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
