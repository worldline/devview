package com.worldline.devview.networkmock.core.repository

import com.worldline.devview.networkmock.core.NetworkMockResourceLoader
import com.worldline.devview.networkmock.core.model.OperationKey
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
        val repository = createRepository(resources = baseResources())

        val result = repository.loadConfiguration()

        result.isSuccess shouldBe true
        val config = result.getOrThrow()
        config.specs shouldHaveSize 1
        config.specs[0].id shouldBe "example"
        config.specs[0].servers shouldContainExactly listOf(
            "https://staging.api.example.com:8443/v1",
            "https://api.example.com"
        )
    }

    @Test
    fun `loadConfiguration uses cache and avoids second file read`() = runTest {
        val loader = RecordingResourceLoader(resources = baseResources())
        val repository = MockConfigRepository(specPaths = listOf(SPEC_PATH), resourceLoader = loader)

        repository.loadConfiguration().getOrThrow()
        repository.loadConfiguration().getOrThrow()

        loader.callCount(path = SPEC_PATH) shouldBe 1
    }

    @Test
    fun `loadConfiguration returns failure when spec file is missing`() = runTest {
        val repository = createRepository(resources = emptyMap())

        val result = repository.loadConfiguration()

        result.isFailure shouldBe true
    }

    @Test
    fun `loadConfiguration returns failure when spec json is malformed`() = runTest {
        val repository = createRepository(resources = mapOf(SPEC_PATH to """{ "paths": { """))

        val result = repository.loadConfiguration()

        result.isFailure shouldBe true
    }

    @Test
    fun `loadConfiguration returns failure when an operation is missing operationId`() = runTest {
        val spec = """
            {
              "info": { "title": "Example" },
              "servers": [ { "url": "https://api.example.com" } ],
              "paths": {
                "/api/users": {
                  "get": { "responses": {} }
                }
              }
            }
        """.trimIndent()
        val repository = createRepository(resources = mapOf(SPEC_PATH to spec))

        val result = repository.loadConfiguration()

        result.isFailure shouldBe true
    }

    @Test
    fun `findMatchingMock returns operation for exact server host path and method`() = runTest {
        val repository = createRepository(resources = baseResources())

        val match = repository.findMatchingMock(
            host = "staging.api.example.com",
            path = "/api/users/42",
            method = "GET"
        )

        match?.specId shouldBe "example"
        match?.operationId shouldBe "getUser"
        match?.config?.method shouldBe "GET"
    }

    @Test
    fun `findMatchingMock matches a secondary declared server`() = runTest {
        val repository = createRepository(resources = baseResources())

        val match = repository.findMatchingMock(
            host = "api.example.com",
            path = "/api/users/42",
            method = "GET"
        )

        match?.operationId shouldBe "getUser"
    }

    @Test
    fun `findMatchingMock host matching is case insensitive`() = runTest {
        val repository = createRepository(resources = baseResources())

        val match = repository.findMatchingMock(
            host = "STAGING.API.EXAMPLE.COM",
            path = "/api/users/42",
            method = "GET"
        )

        match?.operationId shouldBe "getUser"
    }

    @Test
    fun `findMatchingMock handles server url with scheme port and path`() = runTest {
        val repository = createRepository(resources = baseResources())

        val match = repository.findMatchingMock(
            host = "staging.api.example.com",
            path = "/api/users/42",
            method = "GET"
        )

        match?.specId shouldBe "example"
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
    fun `findMatchingMock resolves query parameter matches from declared examples`() = runTest {
        val spec = """
            {
              "info": { "title": "Example" },
              "servers": [ { "url": "https://api.example.com" } ],
              "paths": {
                "/api/users": {
                  "get": {
                    "operationId": "listUsers",
                    "parameters": [
                      { "name": "type", "in": "query", "example": "user" }
                    ],
                    "responses": {}
                  }
                }
              }
            }
        """.trimIndent()
        val repository = createRepository(resources = mapOf(SPEC_PATH to spec))

        val matches = repository.findMatchingMock(
            host = "api.example.com",
            path = "/api/users",
            method = "GET",
            queryParameters = mapOf("type" to listOf("user"))
        )
        val noMatch = repository.findMatchingMock(
            host = "api.example.com",
            path = "/api/users",
            method = "GET",
            queryParameters = mapOf("type" to listOf("admin"))
        )

        matches?.operationId shouldBe "listUsers"
        noMatch.shouldBeNull()
    }

    @Test
    fun `findMatchingMock picks the first spec that has a matching operation when hosts collide`() = runTest {
        val firstSpec = """
            {
              "info": { "title": "First" },
              "servers": [ { "url": "https://api.example.com" } ],
              "paths": {
                "/api/only-in-first": {
                  "get": { "operationId": "onlyInFirst", "responses": {} }
                }
              }
            }
        """.trimIndent()
        val secondSpec = """
            {
              "info": { "title": "Second" },
              "servers": [ { "url": "https://api.example.com" } ],
              "paths": {
                "/api/only-in-first": {
                  "get": { "operationId": "onlyInSecond", "responses": {} }
                }
              }
            }
        """.trimIndent()
        val loader = RecordingResourceLoader(
            resources = mapOf("specs/first.json" to firstSpec, "specs/second.json" to secondSpec)
        )
        val repository = MockConfigRepository(
            specPaths = listOf("specs/first.json", "specs/second.json"),
            resourceLoader = loader
        )

        val match = repository.findMatchingMock(
            host = "api.example.com",
            path = "/api/only-in-first",
            method = "GET"
        )

        match?.specId shouldBe "first"
        match?.operationId shouldBe "onlyInFirst"
    }

    @Test
    fun `findMatchingMock falls through to the next spec when the matched host has no matching operation`() =
        runTest {
            val firstSpec = """
                {
                  "info": { "title": "First" },
                  "servers": [ { "url": "https://api.example.com" } ],
                  "paths": {
                    "/api/elsewhere": {
                      "get": { "operationId": "elsewhere", "responses": {} }
                    }
                  }
                }
            """.trimIndent()
            val secondSpec = """
                {
                  "info": { "title": "Second" },
                  "servers": [ { "url": "https://api.example.com" } ],
                  "paths": {
                    "/api/target": {
                      "get": { "operationId": "target", "responses": {} }
                    }
                  }
                }
            """.trimIndent()
            val loader = RecordingResourceLoader(
                resources = mapOf("specs/first.json" to firstSpec, "specs/second.json" to secondSpec)
            )
            val repository = MockConfigRepository(
                specPaths = listOf("specs/first.json", "specs/second.json"),
                resourceLoader = loader
            )

            val match = repository.findMatchingMock(host = "api.example.com", path = "/api/target", method = "GET")

            match?.specId shouldBe "second"
            match?.operationId shouldBe "target"
        }

    @Test
    fun `findMatchingMock resolves delay from operation falling back to spec default`() = runTest {
        val spec = """
            {
              "info": { "title": "Example" },
              "servers": [ { "url": "https://api.example.com" } ],
              "x-devview": { "delayMs": 200 },
              "paths": {
                "/api/with-own-delay": {
                  "get": {
                    "operationId": "withOwnDelay",
                    "x-devview": { "delayMs": 500 },
                    "responses": {}
                  }
                },
                "/api/without-own-delay": {
                  "get": { "operationId": "withoutOwnDelay", "responses": {} }
                }
              }
            }
        """.trimIndent()
        val repository = createRepository(resources = mapOf(SPEC_PATH to spec))

        val withOwnDelay = repository.findMatchingMock(
            host = "api.example.com",
            path = "/api/with-own-delay",
            method = "GET"
        )
        val withoutOwnDelay = repository.findMatchingMock(
            host = "api.example.com",
            path = "/api/without-own-delay",
            method = "GET"
        )

        withOwnDelay?.delayMs shouldBe 500
        withoutOwnDelay?.delayMs shouldBe 200
    }

    @Test
    fun `local dollar-ref to a components response resolves correctly`() = runTest {
        val spec = """
            {
              "info": { "title": "Example" },
              "servers": [ { "url": "https://api.example.com" } ],
              "paths": {
                "/api/users/{userId}": {
                  "get": {
                    "operationId": "getUser",
                    "responses": {
                      "200": { "${'$'}ref": "#/components/responses/UserOk" }
                    }
                  }
                }
              },
              "components": {
                "responses": {
                  "UserOk": {
                    "content": {
                      "application/json": {
                        "examples": {
                          "default": { "externalValue": "/responses/getUser-200.json" }
                        }
                      }
                    }
                  }
                }
              }
            }
        """.trimIndent()
        val repository = createRepository(
            resources = mapOf(SPEC_PATH to spec, "responses/getUser-200.json" to """{"id":1}""")
        )

        val responses = repository.discoverResponseFiles(key = OperationKey(specId = "example", operationId = "getUser"))

        responses shouldHaveSize 1
        responses.single().statusCode shouldBe 200
        responses.single().content shouldBe """{"id":1}"""
    }

    @Test
    fun `external dollar-ref to another file's components resolves correctly`() = runTest {
        val spec = """
            {
              "info": { "title": "Example" },
              "servers": [ { "url": "https://api.example.com" } ],
              "paths": {
                "/api/users/{userId}": {
                  "get": {
                    "operationId": "getUser",
                    "responses": {
                      "200": { "${'$'}ref": "./common.json#/components/responses/UserOk" }
                    }
                  }
                }
              }
            }
        """.trimIndent()
        val common = """
            {
              "components": {
                "responses": {
                  "UserOk": {
                    "content": {
                      "application/json": {
                        "examples": {
                          "default": { "externalValue": "/responses/getUser-200.json" }
                        }
                      }
                    }
                  }
                }
              }
            }
        """.trimIndent()
        val repository = createRepository(
            resources = mapOf(
                SPEC_PATH to spec,
                "specs/common.json" to common,
                "responses/getUser-200.json" to """{"id":1}"""
            )
        )

        val responses = repository.discoverResponseFiles(key = OperationKey(specId = "example", operationId = "getUser"))

        responses shouldHaveSize 1
        responses.single().content shouldBe """{"id":1}"""
    }

    @Test
    fun `discoverResponseFiles returns responses sorted by status code`() = runTest {
        val repository = createRepository(resources = baseResources())

        val responses = repository.discoverResponseFiles(key = OperationKey(specId = "example", operationId = "getUser"))

        responses.map { it.statusCode } shouldBe listOf(200, 404)
    }

    @Test
    fun `discoverResponseFiles discovers named example variants`() = runTest {
        val repository = createRepository(resources = multiExampleResources())

        val responses = repository.discoverResponseFiles(key = OperationKey(specId = "example", operationId = "getUser"))

        responses shouldHaveSize 3
        responses.map { it.exampleName } shouldContain "detailed"
    }

    @Test
    fun `discoverResponseFiles preserves declared example order within a status code`() = runTest {
        val repository = createRepository(resources = multiExampleResources())

        val responses = repository.discoverResponseFiles(key = OperationKey(specId = "example", operationId = "getUser"))

        responses.filter { it.statusCode == 404 }.map { it.exampleName }.toSet() shouldBe setOf("default", "detailed")
    }

    @Test
    fun `discoverResponseFiles returns empty list when operation declares no responses`() = runTest {
        val repository = createRepository(resources = baseResources())

        val responses = repository.discoverResponseFiles(
            key = OperationKey(specId = "example", operationId = "doesNotExist")
        )

        responses shouldBe emptyList()
    }

    @Test
    fun `loadMockResponse returns parsed response when example exists`() = runTest {
        val repository = createRepository(resources = baseResources())

        val response = repository.loadMockResponse(
            key = OperationKey(specId = "example", operationId = "getUser"),
            statusCode = 200,
            exampleName = "default"
        )

        response?.statusCode shouldBe 200
        response?.displayName shouldBe "Success (200)"
        response?.content shouldBe """{"id":1}"""
    }

    @Test
    fun `loadMockResponse returns null when example is not declared`() = runTest {
        val repository = createRepository(resources = baseResources())

        val response = repository.loadMockResponse(
            key = OperationKey(specId = "example", operationId = "getUser"),
            statusCode = 999,
            exampleName = "default"
        )

        response.shouldBeNull()
    }

    private fun createRepository(resources: Map<String, String>): MockConfigRepository =
        MockConfigRepository(specPaths = listOf(SPEC_PATH), resourceLoader = RecordingResourceLoader(resources))

    private class RecordingResourceLoader(
        private val resources: Map<String, String>
    ) : NetworkMockResourceLoader {
        private val calls = mutableMapOf<String, Int>()

        override suspend fun load(path: String): ByteArray {
            calls[path] = (calls[path] ?: 0) + 1
            return resources[path]?.encodeToByteArray()
                ?: error("Resource not found: $path")
        }

        fun callCount(path: String): Int = calls[path] ?: 0
    }

    private fun baseResources(): Map<String, String> = mapOf(
        SPEC_PATH to baseSpecJson(),
        "responses/getUser-200.json" to """{"id":1}""",
        "responses/getUser-404.json" to """{"error":"not found"}""",
        "responses/createUser-201.json" to """{"id":2}"""
    )

    private fun baseSpecJson(): String = """
        {
          "info": { "title": "Example" },
          "servers": [
            { "url": "https://staging.api.example.com:8443/v1" },
            { "url": "https://api.example.com" }
          ],
          "paths": {
            "/api/users/{userId}": {
              "get": {
                "operationId": "getUser",
                "summary": "Get User",
                "responses": {
                  "200": {
                    "content": {
                      "application/json": {
                        "examples": {
                          "default": { "externalValue": "/responses/getUser-200.json" }
                        }
                      }
                    }
                  },
                  "404": {
                    "content": {
                      "application/json": {
                        "examples": {
                          "default": { "externalValue": "/responses/getUser-404.json" }
                        }
                      }
                    }
                  }
                }
              }
            },
            "/api/users": {
              "post": {
                "operationId": "createUser",
                "summary": "Create User",
                "responses": {
                  "201": {
                    "content": {
                      "application/json": {
                        "examples": {
                          "default": { "externalValue": "/responses/createUser-201.json" }
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

    private fun multiExampleResources(): Map<String, String> = mapOf(
        SPEC_PATH to """
            {
              "info": { "title": "Example" },
              "servers": [ { "url": "https://api.example.com" } ],
              "paths": {
                "/api/users/{userId}": {
                  "get": {
                    "operationId": "getUser",
                    "responses": {
                      "200": {
                        "content": {
                          "application/json": {
                            "examples": {
                              "default": { "externalValue": "/responses/getUser-200.json" }
                            }
                          }
                        }
                      },
                      "404": {
                        "content": {
                          "application/json": {
                            "examples": {
                              "default": { "externalValue": "/responses/getUser-404.json" },
                              "detailed": { "externalValue": "/responses/getUser-404-detailed.json" }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
        """.trimIndent(),
        "responses/getUser-200.json" to """{"id":1}""",
        "responses/getUser-404.json" to """{"error":"not found"}""",
        "responses/getUser-404-detailed.json" to """{"error":"not found","reason":"deleted"}"""
    )

    private companion object {
        const val SPEC_PATH: String = "specs/example.json"
    }
}
