package com.worldline.devview.networkmock.core.repository

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class MockConfigRepositoryTest {

    @Test
    fun `loadConfiguration returns parsed configuration`() = runTest {
        val repository = createRepository(
            resources = baseResources()
        )

        val result = repository.loadConfiguration()

        result.isSuccess shouldBe true
        val config = result.getOrThrow()
        config.hosts shouldHaveSize 2
        config.hosts[0].id shouldBe "staging"
        config.hosts[1].id shouldBe "production"
    }

    @Test
    fun `loadConfiguration uses cache and avoids second file read`() = runTest {
        val loader = RecordingResourceLoader(resources = baseResources())
        val repository = MockConfigRepository(
            configPath = CONFIG_PATH,
            resourceLoader = loader::load
        )

        repository.loadConfiguration().getOrThrow()
        repository.loadConfiguration().getOrThrow()

        loader.callCount(path = CONFIG_PATH) shouldBe 1
    }

    @Test
    fun `loadConfiguration returns failure when config file is missing`() = runTest {
        val repository = createRepository(resources = emptyMap())

        val result = repository.loadConfiguration()

        result.isFailure shouldBe true
    }

    @Test
    fun `loadConfiguration returns failure when config json is malformed`() = runTest {
        val repository = createRepository(
            resources = mapOf(
                CONFIG_PATH to """{ "hosts": [ {"""
            )
        )

        val result = repository.loadConfiguration()

        result.isFailure shouldBe true
    }

    @Test
    fun `findMatchingMock returns endpoint for exact host path and method`() = runTest {
        val repository = createRepository(resources = baseResources())

        val match = repository.findMatchingMock(
            host = "staging.api.example.com",
            path = "/api/users/42",
            method = "GET"
        )

        match?.hostId shouldBe "staging"
        match?.endpointId shouldBe "getUser"
        match?.config?.method shouldBe "GET"
    }

    @Test
    fun `findMatchingMock host matching is case insensitive`() = runTest {
        val repository = createRepository(resources = baseResources())

        val match = repository.findMatchingMock(
            host = "STAGING.API.EXAMPLE.COM",
            path = "/api/users/42",
            method = "GET"
        )

        match?.endpointId shouldBe "getUser"
    }

    @Test
    fun `findMatchingMock handles configured url with scheme port and path`() = runTest {
        val repository = createRepository(resources = baseResources())

        val match = repository.findMatchingMock(
            host = "staging.api.example.com",
            path = "/api/users/42",
            method = "GET"
        )

        match?.hostId shouldBe "staging"
    }

    @Test
    fun `findMatchingMock method matching is case sensitive`() = runTest {
        val repository = createRepository(resources = baseResources())

        val match = repository.findMatchingMock(
            host = "staging.api.example.com",
            path = "/api/users/42",
            method = "get"
        )

        match.shouldBeNull()
    }

    @Test
    fun `findMatchingMock returns null when path does not match`() = runTest {
        val repository = createRepository(resources = baseResources())

        val match = repository.findMatchingMock(
            host = "staging.api.example.com",
            path = "/api/unknown",
            method = "GET"
        )

        match.shouldBeNull()
    }

    @Test
    fun `findMatchingMock returns null when host does not match`() = runTest {
        val repository = createRepository(resources = baseResources())

        val match = repository.findMatchingMock(
            host = "unknown.example.com",
            path = "/api/users/42",
            method = "GET"
        )

        match.shouldBeNull()
    }

    @Test
    fun `findMatchingMock returns null when configuration cannot be loaded`() = runTest {
        val repository = createRepository(resources = emptyMap())

        val match = repository.findMatchingMock(
            host = "staging.api.example.com",
            path = "/api/users/42",
            method = "GET"
        )

        match.shouldBeNull()
    }

    @Test
    fun `discoverResponseFiles returns responses sorted by status code`() = runTest {
        val repository = createRepository(
            resources = baseResources(),
            statusCodesToDiscover = listOf(500, 200, 404)
        )

        val responses = repository.discoverResponseFiles(endpointId = "getUser")

        responses.map { response -> response.statusCode } shouldBe listOf(200, 404)
    }

    @Test
    fun `discoverResponseFiles discovers suffix variants`() = runTest {
        val resources = baseResources() + mapOf(
            "files/networkmocks/responses/getUser/getUser-404-simple.json" to """{"error":"simple"}"""
        )
        val repository = createRepository(
            resources = resources,
            statusCodesToDiscover = listOf(404)
        )

        val responses = repository.discoverResponseFiles(endpointId = "getUser")

        responses shouldHaveSize 2
        responses.map { response -> response.fileName } shouldContain "getUser-404-simple.json"
    }

    @Test
    fun `discoverResponseFiles preserves deterministic order for same status suffix variants`() = runTest {
        val resources = baseResources() + mapOf(
            "files/networkmocks/responses/getUser/getUser-404-simple.json" to """{"error":"simple"}""",
            "files/networkmocks/responses/getUser/getUser-404-detailed.json" to """{"error":"detailed"}"""
        )
        val repository = createRepository(
            resources = resources,
            statusCodesToDiscover = listOf(404)
        )

        val responses = repository.discoverResponseFiles(endpointId = "getUser")

        responses.map { response -> response.fileName } shouldContainExactly listOf(
            "getUser-404.json",
            "getUser-404-simple.json",
            "getUser-404-detailed.json"
        )
    }

    @Test
    fun `discoverResponseFiles deduplicates entries when statusCodesToDiscover contains duplicates`() = runTest {
        val repository = createRepository(
            resources = baseResources(),
            statusCodesToDiscover = listOf(404, 404)
        )

        val responses = repository.discoverResponseFiles(endpointId = "getUser")

        responses shouldHaveSize 1
        responses.single().fileName shouldBe "getUser-404.json"
    }

    @Test
    fun `discoverResponseFiles honors custom statusCodesToDiscover`() = runTest {
        val repository = createRepository(
            resources = baseResources(),
            statusCodesToDiscover = listOf(200)
        )

        val responses = repository.discoverResponseFiles(endpointId = "getUser")

        responses shouldHaveSize 1
        responses.single().statusCode shouldBe 200
    }

    @Test
    fun `loadMockResponse returns parsed response when file exists`() = runTest {
        val repository = createRepository(resources = baseResources())

        val response = repository.loadMockResponse(
            endpointId = "getUser",
            fileName = "getUser-200.json"
        )

        response?.statusCode shouldBe 200
        response?.displayName shouldBe "Success (200)"
        response?.content shouldBe """{"id":1}"""
    }

    @Test
    fun `loadMockResponse returns null when file is missing`() = runTest {
        val repository = createRepository(resources = baseResources())

        val response = repository.loadMockResponse(
            endpointId = "getUser",
            fileName = "getUser-999.json"
        )

        response.shouldBeNull()
    }

    @Test
    fun `loadMockResponse returns null when file name is malformed`() = runTest {
        val resources = baseResources() + mapOf(
            "files/networkmocks/responses/getUser/getUser-invalid.json" to """{"id":1}"""
        )
        val repository = createRepository(resources = resources)

        val response = repository.loadMockResponse(
            endpointId = "getUser",
            fileName = "getUser-invalid.json"
        )

        response.shouldBeNull()
    }

    @Test
    fun `discoverResponseFiles returns empty list when endpoint has no files`() = runTest {
        val repository = createRepository(resources = baseResources())

        val responses = repository.discoverResponseFiles(endpointId = "doesNotExist")

        responses shouldBe emptyList()
    }

    private fun createRepository(
        resources: Map<String, String>,
        statusCodesToDiscover: List<Int> = MockConfigRepository.DEFAULT_STATUS_CODES
    ): MockConfigRepository {
        val loader = RecordingResourceLoader(resources = resources)
        return MockConfigRepository(
            configPath = CONFIG_PATH,
            resourceLoader = loader::load,
            statusCodesToDiscover = statusCodesToDiscover
        )
    }

    private class RecordingResourceLoader(
        private val resources: Map<String, String>
    ) {
        private val calls = mutableMapOf<String, Int>()

        fun load(path: String): ByteArray {
            calls[path] = (calls[path] ?: 0) + 1
            return resources[path]?.encodeToByteArray()
                ?: error("Resource not found: $path")
        }

        fun callCount(path: String): Int = calls[path] ?: 0
    }

    private fun baseResources(): Map<String, String> = mapOf(
        CONFIG_PATH to baseConfigJson(),
        "files/networkmocks/responses/getUser/getUser-200.json" to """{"id":1}""",
        "files/networkmocks/responses/getUser/getUser-404.json" to """{"error":"not found"}""",
        "files/networkmocks/responses/createUser/createUser-201.json" to """{"id":2}"""
    )

    @Suppress("MaxLineLength")
    private fun baseConfigJson(): String = """
        {
          "hosts": [
            {
              "id": "staging",
              "url": "https://staging.api.example.com:8443/v1",
              "endpoints": [
                {
                  "id": "getUser",
                  "name": "Get User",
                  "path": "/api/users/{userId}",
                  "method": "GET"
                },
                {
                  "id": "createUser",
                  "name": "Create User",
                  "path": "/api/users",
                  "method": "POST"
                }
              ]
            },
            {
              "id": "production",
              "url": "https://api.example.com",
              "endpoints": [
                {
                  "id": "getProduct",
                  "name": "Get Product",
                  "path": "/api/products/{productId}",
                  "method": "GET"
                }
              ]
            }
          ]
        }
    """.trimIndent()

    private companion object {
        const val CONFIG_PATH: String = "files/networkmocks/mocks.json"
    }
}

