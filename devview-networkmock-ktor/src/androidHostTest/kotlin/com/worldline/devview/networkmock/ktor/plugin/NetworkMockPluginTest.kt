package com.worldline.devview.networkmock.ktor.plugin

import com.worldline.devview.networkmock.core.model.FailureKind
import com.worldline.devview.networkmock.core.model.NetworkMockState
import com.worldline.devview.networkmock.core.model.OperationKey
import com.worldline.devview.networkmock.core.model.OperationMockState
import com.worldline.devview.networkmock.core.repository.MockConfigRepository
import com.worldline.devview.networkmock.core.repository.MockStateRepository
import com.worldline.devview.networkmock.ktor.fixtures.KtorPluginTestData
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException

class NetworkMockPluginTest {

    @Test
    fun requestPassesThrough_whenGlobalMockingDisabled() = runTest {
        val stateRepo = stateRepositoryMock(state = NetworkMockState(globalMockingEnabled = false))
        val client = buildClient(
            engine = networkEngine(body = """{"source":"network"}"""),
            configRepository = configRepository(),
            stateRepository = stateRepo
        )

        val response: HttpResponse = client.get(
            urlString = "https://staging.api.example.com/api/users/42"
        )

        response.status shouldBe HttpStatusCode.OK
        response.body<String>() shouldBe """{"source":"network"}"""
    }

    // region Matching and mock response returned

    @Test
    fun returnsMockResponse_whenEndpointIsMocked() = runTest {
        val state = NetworkMockState(
            globalMockingEnabled = true,
            operationStates = mapOf(
                "example-getUser" to OperationMockState.Mock(statusCode = 200, exampleName = "default")
            )
        )
        val client = buildClient(
            engine = networkEngine(),
            configRepository = configRepository(),
            stateRepository = stateRepositoryMock(state = state)
        )

        val response: HttpResponse = client.get(
            urlString = "https://staging.api.example.com/api/users/42"
        )

        response.status shouldBe HttpStatusCode.OK
        response.body<String>() shouldBe """{"id":1,"name":"Alice"}"""
    }

    @Test
    fun returnsMockResponse_withCorrectStatusCode_404() = runTest {
        val state = NetworkMockState(
            globalMockingEnabled = true,
            operationStates = mapOf(
                "example-getUser" to OperationMockState.Mock(statusCode = 404, exampleName = "default")
            )
        )
        val client = buildClient(
            engine = networkEngine(),
            configRepository = configRepository(),
            stateRepository = stateRepositoryMock(state = state)
        )

        val response: HttpResponse = client.get(
            urlString = "https://staging.api.example.com/api/users/99"
        )

        response.status shouldBe HttpStatusCode.NotFound
        response.body<String>() shouldBe """{"error":"not found"}"""
    }

    @Test
    fun returnsMockResponse_forPostEndpoint() = runTest {
        val state = NetworkMockState(
            globalMockingEnabled = true,
            operationStates = mapOf(
                "example-createUser" to OperationMockState.Mock(statusCode = 201, exampleName = "default")
            )
        )
        val client = buildClient(
            engine = networkEngine(),
            configRepository = configRepository(),
            stateRepository = stateRepositoryMock(state = state)
        )

        val response: HttpResponse = client.post(
            urlString = "https://staging.api.example.com/api/users"
        ) {
            setBody("""{"name":"Bob"}""")
        }

        response.status shouldBe HttpStatusCode.Created
        response.body<String>() shouldBe """{"id":2}"""
    }

    @Test
    fun returnsMockResponse_forDifferentHost_production() = runTest {
        val state = NetworkMockState(
            globalMockingEnabled = true,
            operationStates = mapOf(
                "example-getProduct" to OperationMockState.Mock(statusCode = 200, exampleName = "default")
            )
        )
        val client = buildClient(
            engine = networkEngine(),
            configRepository = configRepository(),
            stateRepository = stateRepositoryMock(state = state)
        )

        val response: HttpResponse = client.get(
            urlString = "https://api.example.com/api/products/10"
        )

        response.status shouldBe HttpStatusCode.OK
        response.body<String>() shouldBe """{"id":10,"name":"Widget"}"""
    }

    // endregion

    // region Response headers and content type

    @Test
    fun returnsMockResponse_withDefaultContentTypeHeader() = runTest {
        val state = NetworkMockState(
            globalMockingEnabled = true,
            operationStates = mapOf(
                "example-getUser" to OperationMockState.Mock(statusCode = 200, exampleName = "default")
            )
        )
        val client = buildClient(
            engine = networkEngine(),
            configRepository = configRepository(),
            stateRepository = stateRepositoryMock(state = state)
        )

        val response: HttpResponse = client.get(
            urlString = "https://staging.api.example.com/api/users/42"
        )

        // getUser's 200 response declares no explicit headers - Content-Type still defaults.
        response.headers[HttpHeaders.ContentType] shouldBe "application/json"
    }

    @Test
    fun returnsMockResponse_withDeclaredHeadersAndContentType() = runTest {
        val specWithHeaders = """
            {
              "info": { "title": "Example" },
              "servers": [ { "url": "https://staging.api.example.com" } ],
              "paths": {
                "/api/users/{userId}": {
                  "get": {
                    "operationId": "getUser",
                    "responses": {
                      "200": {
                        "headers": {
                          "X-RateLimit-Remaining": { "example": "42" }
                        },
                        "content": {
                          "application/vnd.example+json": {
                            "examples": {
                              "default": { "externalValue": "/files/networkmocks/responses/getUser-200.json" }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
        """.trimIndent()
        val resources = mapOf(
            KtorPluginTestData.SPEC_PATH to specWithHeaders,
            "files/networkmocks/responses/getUser-200.json" to """{"id":1,"name":"Alice"}"""
        )
        val state = NetworkMockState(
            globalMockingEnabled = true,
            operationStates = mapOf(
                "example-getUser" to OperationMockState.Mock(statusCode = 200, exampleName = "default")
            )
        )
        val client = buildClient(
            engine = networkEngine(),
            configRepository = configRepository(resources = resources),
            stateRepository = stateRepositoryMock(state = state)
        )

        val response: HttpResponse = client.get(
            urlString = "https://staging.api.example.com/api/users/42"
        )

        response.headers["X-RateLimit-Remaining"] shouldBe "42"
        response.headers[HttpHeaders.ContentType] shouldBe "application/vnd.example+json"
    }

    // endregion

    // region Sequential mocks

    @Test
    fun sequence_advancesThroughStepsOnSuccessiveRequests_andSticksOnLastOnceExhausted() = runTest {
        val steps = listOf(
            OperationMockState.Mock(statusCode = 200, exampleName = "default"),
            OperationMockState.Mock(statusCode = 404, exampleName = "default")
        )
        val stateRepository = mutableStateRepositoryMock(
            initial = NetworkMockState(
                globalMockingEnabled = true,
                operationStates = mapOf(
                    "example-getUser" to OperationMockState.Sequence(responses = steps)
                )
            )
        )
        val client = buildClient(
            engine = networkEngine(),
            configRepository = configRepository(),
            stateRepository = stateRepository
        )

        // Step 1: the first response, then advances to index 1.
        client.get(urlString = "https://staging.api.example.com/api/users/42")
            .status shouldBe HttpStatusCode.OK
        // Step 2: the second (last) response, then sticks at index 1 - no third step exists.
        client.get(urlString = "https://staging.api.example.com/api/users/42")
            .status shouldBe HttpStatusCode.NotFound
        // Step 3 onward: still the last response.
        client.get(urlString = "https://staging.api.example.com/api/users/42")
            .status shouldBe HttpStatusCode.NotFound
    }

    @Test
    fun probabilisticFailure_appliesToSequenceStatesToo() = runTest {
        val resources = flakySpecResources(failureRate = 1.0)
        val stateRepository = mutableStateRepositoryMock(
            initial = NetworkMockState(
                globalMockingEnabled = true,
                operationStates = mapOf(
                    "example-getUser" to OperationMockState.Sequence(
                        responses = listOf(OperationMockState.Mock(statusCode = 200, exampleName = "default"))
                    )
                )
            )
        )
        val client = buildClient(
            engine = networkEngine(),
            configRepository = configRepository(resources = resources),
            stateRepository = stateRepository,
            random = FixedRandom(value = 0.0)
        )

        assertFailsWith<IOException> {
            client.get(urlString = "https://staging.api.example.com/api/users/42")
        }
    }

    /**
     * Unlike [stateRepositoryMock], writes actually mutate the backing state, so a test can
     * make more than one request and observe the plugin's own advance-and-persist write.
     */
    private fun mutableStateRepositoryMock(initial: NetworkMockState): MockStateRepository {
        val stateFlow = MutableStateFlow(initial)
        return mockk<MockStateRepository>(relaxed = true) {
            coEvery { getState() } answers { stateFlow.value }
            every { observeState() } returns stateFlow
            coEvery { setOperationMockState(key = any(), state = any()) } answers {
                val key = firstArg<OperationKey>()
                val newState = secondArg<OperationMockState>()
                stateFlow.value = stateFlow.value.withOperationState(key = key, state = newState)
            }
        }
    }

    // endregion

    // region Request body matching

    @Test
    fun requestBodyDisambiguation_selectsCorrectOperationByDiscriminatorValue() = runTest {
        val resources = requestBodyDisambiguationResources()
        val state = NetworkMockState(
            globalMockingEnabled = true,
            operationStates = mapOf(
                "card-payByCard" to OperationMockState.Mock(statusCode = 200, exampleName = "default"),
                "bank-payByBankTransfer" to OperationMockState.Mock(statusCode = 200, exampleName = "default")
            )
        )
        val client = buildClient(
            engine = networkEngine(body = """{"source":"network"}"""),
            configRepository = requestBodyDisambiguationRepository(resources = resources),
            stateRepository = stateRepositoryMock(state = state)
        )

        val cardResponse = client.post(urlString = "https://staging.api.example.com/api/payments") {
            setBody("""{"type":"card","number":"4242"}""")
        }
        val bankResponse = client.post(urlString = "https://staging.api.example.com/api/payments") {
            setBody("""{"type":"bank_transfer","iban":"DE00"}""")
        }

        cardResponse.body<String>() shouldBe """{"method":"card"}"""
        bankResponse.body<String>() shouldBe """{"method":"bank_transfer"}"""
    }

    @Test
    fun requestBodyDisambiguation_fallsBackToNetworkWithOriginalBodyIntact_whenNoShapeMatches() = runTest {
        val resources = requestBodyDisambiguationResources()
        var capturedBody: String? = null
        val engine = MockEngine { request ->
            capturedBody = (request.body as? OutgoingContent.ByteArrayContent)
                ?.bytes()
                ?.decodeToString()
            respond(
                content = """{"source":"network"}""",
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json")
            )
        }
        val state = NetworkMockState(
            globalMockingEnabled = true,
            operationStates = mapOf(
                "card-payByCard" to OperationMockState.Mock(statusCode = 200, exampleName = "default"),
                "bank-payByBankTransfer" to OperationMockState.Mock(statusCode = 200, exampleName = "default")
            )
        )
        val client = buildClient(
            engine = engine,
            configRepository = requestBodyDisambiguationRepository(resources = resources),
            stateRepository = stateRepositoryMock(state = state)
        )
        val originalBody = """{"type":"crypto","wallet":"abc123"}"""

        val response: HttpResponse = client.post(
            urlString = "https://staging.api.example.com/api/payments"
        ) {
            setBody(originalBody)
        }

        // No declared operation's requestBody shape matches "crypto" - falls through to network,
        // and the original body must still reach it byte-for-byte, unconsumed.
        response.body<String>() shouldBe """{"source":"network"}"""
        capturedBody shouldBe originalBody
    }

    /**
     * Two specs, sharing a host and declaring the identical `POST /api/payments` path/method,
     * disambiguated only by a `type` discriminator in their respective `requestBody` schemas -
     * the scenario [MockConfigRepository.findMatchingMock]'s own KDoc describes for why
     * request-body matching exists.
     */
    private fun requestBodyDisambiguationResources(): Map<String, String> {
        val cardSpec = """
            {
              "info": { "title": "Card" },
              "servers": [ { "url": "https://staging.api.example.com" } ],
              "paths": {
                "/api/payments": {
                  "post": {
                    "operationId": "payByCard",
                    "requestBody": {
                      "content": {
                        "application/json": {
                          "schema": {
                            "type": "object",
                            "discriminator": { "propertyName": "type" },
                            "properties": { "type": { "type": "string", "enum": ["card"] } }
                          }
                        }
                      }
                    },
                    "responses": {
                      "200": {
                        "content": {
                          "application/json": {
                            "examples": {
                              "default": { "externalValue": "/files/networkmocks/responses/payByCard-200.json" }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
        """.trimIndent()
        val bankSpec = """
            {
              "info": { "title": "Bank" },
              "servers": [ { "url": "https://staging.api.example.com" } ],
              "paths": {
                "/api/payments": {
                  "post": {
                    "operationId": "payByBankTransfer",
                    "requestBody": {
                      "content": {
                        "application/json": {
                          "schema": {
                            "type": "object",
                            "discriminator": { "propertyName": "type" },
                            "properties": { "type": { "type": "string", "enum": ["bank_transfer"] } }
                          }
                        }
                      }
                    },
                    "responses": {
                      "200": {
                        "content": {
                          "application/json": {
                            "examples": {
                              "default": {
                                "externalValue": "/files/networkmocks/responses/payByBankTransfer-200.json"
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
        """.trimIndent()
        return mapOf(
            "files/networkmocks/specs/card.json" to cardSpec,
            "files/networkmocks/specs/bank.json" to bankSpec,
            "files/networkmocks/responses/payByCard-200.json" to """{"method":"card"}""",
            "files/networkmocks/responses/payByBankTransfer-200.json" to """{"method":"bank_transfer"}"""
        )
    }

    private fun requestBodyDisambiguationRepository(resources: Map<String, String>): MockConfigRepository =
        MockConfigRepository(
            specPaths = listOf(
                "files/networkmocks/specs/card.json",
                "files/networkmocks/specs/bank.json"
            ),
            resourceLoader = KtorPluginTestData.resourceLoader(resources = resources)
        )

    // endregion

    // region Failure simulation

    @Test
    fun returnsFailure_whenEndpointStateIsFailureTimeout() = runTest {
        val state = NetworkMockState(
            globalMockingEnabled = true,
            operationStates = mapOf(
                "example-getUser" to OperationMockState.Failure(kind = FailureKind.TIMEOUT)
            )
        )
        val client = buildClient(
            engine = networkEngine(),
            configRepository = configRepository(),
            stateRepository = stateRepositoryMock(state = state)
        )

        assertFailsWith<HttpRequestTimeoutException> {
            client.get(urlString = "https://staging.api.example.com/api/users/42")
        }
    }

    @Test
    fun returnsFailure_whenEndpointStateIsFailureConnectionRefused() = runTest {
        val state = NetworkMockState(
            globalMockingEnabled = true,
            operationStates = mapOf(
                "example-getUser" to OperationMockState.Failure(kind = FailureKind.CONNECTION_REFUSED)
            )
        )
        val client = buildClient(
            engine = networkEngine(),
            configRepository = configRepository(),
            stateRepository = stateRepositoryMock(state = state)
        )

        assertFailsWith<IOException> {
            client.get(urlString = "https://staging.api.example.com/api/users/42")
        }
    }

    @Test
    fun probabilisticFailure_throwsWhenRandomRollHitsTheConfiguredRate() = runTest {
        val resources = flakySpecResources(failureRate = 0.5)
        val state = NetworkMockState(
            globalMockingEnabled = true,
            operationStates = mapOf(
                "example-getUser" to OperationMockState.Mock(statusCode = 200, exampleName = "default")
            )
        )
        val client = buildClient(
            engine = networkEngine(),
            configRepository = configRepository(resources = resources),
            stateRepository = stateRepositoryMock(state = state),
            // 0.0 < 0.5 -> always "hits" the configured rate.
            random = FixedRandom(value = 0.0)
        )

        assertFailsWith<IOException> {
            client.get(urlString = "https://staging.api.example.com/api/users/42")
        }
    }

    @Test
    fun probabilisticFailure_servesMockWhenRandomRollMissesTheConfiguredRate() = runTest {
        val resources = flakySpecResources(failureRate = 0.5)
        val state = NetworkMockState(
            globalMockingEnabled = true,
            operationStates = mapOf(
                "example-getUser" to OperationMockState.Mock(statusCode = 200, exampleName = "default")
            )
        )
        val client = buildClient(
            engine = networkEngine(),
            configRepository = configRepository(resources = resources),
            stateRepository = stateRepositoryMock(state = state),
            // 0.99 >= 0.5 -> always "misses" the configured rate.
            random = FixedRandom(value = 0.99)
        )

        val response: HttpResponse = client.get(
            urlString = "https://staging.api.example.com/api/users/42"
        )

        response.status shouldBe HttpStatusCode.OK
        response.body<String>() shouldBe """{"id":1,"name":"Alice"}"""
    }

    @Test
    fun probabilisticFailure_doesNotApply_whenEndpointStateIsNetwork() = runTest {
        // The roll only applies to otherwise-mocked requests - see the plugin's own doc note.
        val resources = flakySpecResources(failureRate = 1.0)
        val state = NetworkMockState(
            globalMockingEnabled = true,
            operationStates = mapOf("example-getUser" to OperationMockState.Network)
        )
        val client = buildClient(
            engine = networkEngine(body = """{"source":"network"}"""),
            configRepository = configRepository(resources = resources),
            stateRepository = stateRepositoryMock(state = state),
            random = FixedRandom(value = 0.0)
        )

        val response: HttpResponse = client.get(
            urlString = "https://staging.api.example.com/api/users/42"
        )

        response.body<String>() shouldBe """{"source":"network"}"""
    }

    /** A spec with `x-devview.failureRate` declared on `getUser`, backed by its response file. */
    private fun flakySpecResources(failureRate: Double): Map<String, String> {
        val spec = """
            {
              "info": { "title": "Example" },
              "servers": [ { "url": "https://staging.api.example.com" } ],
              "paths": {
                "/api/users/{userId}": {
                  "get": {
                    "operationId": "getUser",
                    "x-devview": { "failureRate": $failureRate },
                    "responses": {
                      "200": {
                        "content": {
                          "application/json": {
                            "examples": {
                              "default": { "externalValue": "/files/networkmocks/responses/getUser-200.json" }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
        """.trimIndent()
        return mapOf(
            KtorPluginTestData.SPEC_PATH to spec,
            "files/networkmocks/responses/getUser-200.json" to """{"id":1,"name":"Alice"}"""
        )
    }

    /** A [Random] pinned to always return [value] from [nextDouble], for deterministic rolls. */
    private class FixedRandom(private val value: Double) : Random() {
        override fun nextBits(bitCount: Int): Int = 0
        override fun nextDouble(): Double = value
    }

    // endregion

    // region Non-matching requests pass through

    @Test
    fun requestPassesThrough_whenHostDoesNotMatch() = runTest {
        val state = NetworkMockState(globalMockingEnabled = true)
        val client = buildClient(
            engine = networkEngine(body = """{"source":"network"}"""),
            configRepository = configRepository(),
            stateRepository = stateRepositoryMock(state = state)
        )

        val response: HttpResponse = client.get(
            urlString = "https://unknown.host.example.com/api/users/1"
        )

        response.body<String>() shouldBe """{"source":"network"}"""
    }

    @Test
    fun requestPassesThrough_whenPathDoesNotMatch() = runTest {
        val state = NetworkMockState(globalMockingEnabled = true)
        val client = buildClient(
            engine = networkEngine(body = """{"source":"network"}"""),
            configRepository = configRepository(),
            stateRepository = stateRepositoryMock(state = state)
        )

        val response: HttpResponse = client.get(
            urlString = "https://staging.api.example.com/api/completely/different/path"
        )

        response.body<String>() shouldBe """{"source":"network"}"""
    }

    @Test
    fun requestPassesThrough_whenMethodDoesNotMatch() = runTest {
        // getUser is GET-only; sending POST should fall through to network
        val state = NetworkMockState(
            globalMockingEnabled = true,
            operationStates = mapOf(
                "example-getUser" to OperationMockState.Mock(statusCode = 200, exampleName = "default")
            )
        )
        val client = buildClient(
            engine = networkEngine(body = """{"source":"network"}"""),
            configRepository = configRepository(),
            stateRepository = stateRepositoryMock(state = state)
        )

        val response: HttpResponse = client.post(
            urlString = "https://staging.api.example.com/api/users/42"
        )

        response.body<String>() shouldBe """{"source":"network"}"""
    }

    @Test
    fun requestPassesThrough_whenEndpointStateIsNetwork() = runTest {
        // Global mocking on, but operation left as Network — should pass through
        val state = NetworkMockState(
            globalMockingEnabled = true,
            operationStates = mapOf("example-getUser" to OperationMockState.Network)
        )
        val client = buildClient(
            engine = networkEngine(body = """{"source":"network"}"""),
            configRepository = configRepository(),
            stateRepository = stateRepositoryMock(state = state)
        )

        val response: HttpResponse = client.get(
            urlString = "https://staging.api.example.com/api/users/42"
        )

        response.body<String>() shouldBe """{"source":"network"}"""
    }

    @Test
    fun requestPassesThrough_whenEndpointHasNoStoredState() = runTest {
        // Global mocking on, operation exists in config but has no entry in operationStates
        val state = NetworkMockState(
            globalMockingEnabled = true,
            operationStates = emptyMap()
        )
        val client = buildClient(
            engine = networkEngine(body = """{"source":"network"}"""),
            configRepository = configRepository(),
            stateRepository = stateRepositoryMock(state = state)
        )

        val response: HttpResponse = client.get(
            urlString = "https://staging.api.example.com/api/users/42"
        )

        response.body<String>() shouldBe """{"source":"network"}"""
    }

    // endregion

    // region Path parameter matching

    @Test
    fun pathParameterMatching_matchesDifferentConcreteValues() = runTest {
        val state = NetworkMockState(
            globalMockingEnabled = true,
            operationStates = mapOf(
                "example-getUser" to OperationMockState.Mock(statusCode = 200, exampleName = "default")
            )
        )
        val client = buildClient(
            engine = networkEngine(),
            configRepository = configRepository(),
            stateRepository = stateRepositoryMock(state = state)
        )

        // Both /api/users/1 and /api/users/abc-uuid should match /api/users/{userId}
        client.get(urlString = "https://staging.api.example.com/api/users/1")
            .status shouldBe HttpStatusCode.OK
        client.get(urlString = "https://staging.api.example.com/api/users/abc-uuid-123")
            .status shouldBe HttpStatusCode.OK
    }

    // endregion

    // region Error / fallback behaviour

    @Test
    fun requestFallsBackToNetwork_whenResponseFileIsMissing() = runTest {
        val resourcesWithoutResponseFile = KtorPluginTestData.responseResources
            .filterKeys { key -> key != "files/networkmocks/responses/getUser-200.json" }

        val state = NetworkMockState(
            globalMockingEnabled = true,
            operationStates = mapOf(
                "example-getUser" to OperationMockState.Mock(statusCode = 200, exampleName = "default")
            )
        )
        val client = buildClient(
            engine = networkEngine(body = """{"source":"network"}"""),
            configRepository = configRepository(resources = resourcesWithoutResponseFile),
            stateRepository = stateRepositoryMock(state = state)
        )

        // Plugin catches the load failure and falls back to network
        val response: HttpResponse = client.get(
            urlString = "https://staging.api.example.com/api/users/42"
        )

        response.body<String>() shouldBe """{"source":"network"}"""
    }

    @Test
    fun requestFallsBackToNetwork_whenConfigurationIsMissing() = runTest {
        val resourcesWithoutConfig = KtorPluginTestData.responseResources
            .filterKeys { key -> key != KtorPluginTestData.SPEC_PATH }

        val state = NetworkMockState(globalMockingEnabled = true)
        val client = buildClient(
            engine = networkEngine(body = """{"source":"network"}"""),
            configRepository = configRepository(resources = resourcesWithoutConfig),
            stateRepository = stateRepositoryMock(state = state)
        )

        val response: HttpResponse = client.get(
            urlString = "https://staging.api.example.com/api/users/42"
        )

        response.body<String>() shouldBe """{"source":"network"}"""
    }

    @Test
    fun requestFallsBackToNetwork_whenConfigurationIsMalformed_butEndpointStateIsMocked() = runTest {
        val resourcesWithMalformedConfig = KtorPluginTestData.responseResources + mapOf(
            KtorPluginTestData.SPEC_PATH to """{ "paths": { """
        )

        val state = NetworkMockState(
            globalMockingEnabled = true,
            operationStates = mapOf(
                "example-getUser" to OperationMockState.Mock(statusCode = 200, exampleName = "default")
            )
        )
        val client = buildClient(
            engine = networkEngine(body = """{"source":"network"}"""),
            configRepository = configRepository(resources = resourcesWithMalformedConfig),
            stateRepository = stateRepositoryMock(state = state)
        )

        val response: HttpResponse = client.get(
            urlString = "https://staging.api.example.com/api/users/42"
        )

        response.body<String>() shouldBe """{"source":"network"}"""
    }

    @Test
    fun requestFallsBackToNetwork_whenSelectedExampleIsNotDeclared() = runTest {
        val state = NetworkMockState(
            globalMockingEnabled = true,
            operationStates = mapOf(
                "example-getUser" to OperationMockState.Mock(statusCode = 200, exampleName = "doesNotExist")
            )
        )
        val client = buildClient(
            engine = networkEngine(body = """{"source":"network"}"""),
            configRepository = configRepository(),
            stateRepository = stateRepositoryMock(state = state)
        )

        // The (statusCode, exampleName) pair isn't declared in the spec — falls back to network.
        val response: HttpResponse = client.get(
            urlString = "https://staging.api.example.com/api/users/42"
        )

        response.body<String>() shouldBe """{"source":"network"}"""
    }

    // endregion

    // region Helpers

    /**
     * A [MockEngine] that always responds with [body] and [status], standing in
     * for "the real network".
     */
    private fun networkEngine(
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = """{"source":"network"}"""
    ): MockEngine = MockEngine { _ ->
        respond(
            content = body,
            status = status,
            headers = headersOf("Content-Type", "application/json")
        )
    }

    private fun buildClient(
        engine: MockEngine,
        configRepository: MockConfigRepository,
        stateRepository: MockStateRepository,
        random: Random? = null
    ): HttpClient = HttpClient(engine = engine) {
        install(plugin = NetworkMockPlugin) {
            mockRepository = configRepository
            this.stateRepository = stateRepository
            if (random != null) {
                this.random = random
            }
        }
    }

    private fun configRepository(
        resources: Map<String, String> = KtorPluginTestData.responseResources
    ): MockConfigRepository = MockConfigRepository(
        specPaths = listOf(KtorPluginTestData.SPEC_PATH),
        resourceLoader = KtorPluginTestData.resourceLoader(resources = resources)
    )

    /**
     * Creates a MockK mock of [MockStateRepository] whose [MockStateRepository.getState]
     * returns [state] and whose [MockStateRepository.observeState] emits it.
     * All write operations are stubbed to do nothing.
     */
    private fun stateRepositoryMock(
        state: NetworkMockState = NetworkMockState()
    ): MockStateRepository = mockk<MockStateRepository>(relaxed = true) {
        coEvery { getState() } returns state
        every { observeState() } returns flowOf(state)
    }

    // endregion
}
