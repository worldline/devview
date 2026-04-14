package com.worldline.devview.networkmock.ktor.plugin

import com.worldline.devview.networkmock.core.model.EndpointMockState
import com.worldline.devview.networkmock.core.model.NetworkMockState
import com.worldline.devview.networkmock.core.repository.MockConfigRepository
import com.worldline.devview.networkmock.core.repository.MockStateRepository
import com.worldline.devview.networkmock.ktor.fixtures.KtorPluginTestData
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

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
            endpointStates = mapOf(
                "example-staging-getUser" to EndpointMockState.Mock(responseFile = "getUser-200.json")
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
            endpointStates = mapOf(
                "example-staging-getUser" to EndpointMockState.Mock(responseFile = "getUser-404.json")
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
            endpointStates = mapOf(
                "example-staging-createUser" to EndpointMockState.Mock(responseFile = "createUser-201.json")
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
            endpointStates = mapOf(
                "example-production-getProduct" to EndpointMockState.Mock(responseFile = "getProduct-200.json")
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
            endpointStates = mapOf(
                "example-staging-getUser" to EndpointMockState.Mock(responseFile = "getUser-200.json")
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
        // Global mocking on, but endpoint left as Network — should pass through
        val state = NetworkMockState(
            globalMockingEnabled = true,
            endpointStates = mapOf("example-staging-getUser" to EndpointMockState.Network)
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
        // Global mocking on, endpoint exists in config but has no entry in endpointStates
        val state = NetworkMockState(
            globalMockingEnabled = true,
            endpointStates = emptyMap()
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
            endpointStates = mapOf(
                "example-staging-getUser" to EndpointMockState.Mock(responseFile = "getUser-200.json")
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
            .filterKeys { key -> key != "files/networkmocks/responses/example/staging/getUser/getUser-200.json" }

        val state = NetworkMockState(
            globalMockingEnabled = true,
            endpointStates = mapOf(
                "example-staging-getUser" to EndpointMockState.Mock(responseFile = "getUser-200.json")
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
            .filterKeys { key -> key != "files/networkmocks/mocks.json" }

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
            "files/networkmocks/mocks.json" to """{ "apiGroups": [ {"""
        )

        val state = NetworkMockState(
            globalMockingEnabled = true,
            endpointStates = mapOf(
                "example-staging-getUser" to EndpointMockState.Mock(responseFile = "getUser-200.json")
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
    fun requestFallsBackToNetwork_whenResponseFileNameIsMalformed() = runTest {
        val resourcesWithMalformedFileName = KtorPluginTestData.responseResources + mapOf(
            "files/networkmocks/responses/example/staging/getUser/getUser-invalid.json" to """{"id":1}"""
        )

        val state = NetworkMockState(
            globalMockingEnabled = true,
            endpointStates = mapOf(
                "example-staging-getUser" to EndpointMockState.Mock(responseFile = "getUser-invalid.json")
            )
        )
        val client = buildClient(
            engine = networkEngine(body = """{"source":"network"}"""),
            configRepository = configRepository(resources = resourcesWithMalformedFileName),
            stateRepository = stateRepositoryMock(state = state)
        )

        // File exists, but MockResponse.fromFile cannot parse status from the malformed name.
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
        stateRepository: MockStateRepository
    ): HttpClient = HttpClient(engine = engine) {
        install(plugin = NetworkMockPlugin) {
            mockRepository = configRepository
            this.stateRepository = stateRepository
        }
    }

    private fun configRepository(
        resources: Map<String, String> = KtorPluginTestData.responseResources
    ): MockConfigRepository = MockConfigRepository(
        configPath = "files/networkmocks/mocks.json",
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

