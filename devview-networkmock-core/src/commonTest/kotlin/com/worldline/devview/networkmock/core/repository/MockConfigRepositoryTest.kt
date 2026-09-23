package com.worldline.devview.networkmock.core.repository

import com.worldline.devview.networkmock.core.NetworkMockResourceLoader
import com.worldline.devview.networkmock.core.model.HttpMethod
import com.worldline.devview.networkmock.core.model.OperationKey
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
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
        val repository =
            MockConfigRepository(specPaths = listOf(SPEC_PATH), resourceLoader = loader)

        repository.loadConfiguration().getOrThrow()
        repository.loadConfiguration().getOrThrow()

        loader.callCount(path = SPEC_PATH) shouldBe 1
    }

    @Test
    fun `invalidate forces loadConfiguration to re-read the spec file`() = runTest {
        val loader = RecordingResourceLoader(resources = baseResources())
        val repository =
            MockConfigRepository(specPaths = listOf(SPEC_PATH), resourceLoader = loader)

        repository.loadConfiguration().getOrThrow()
        repository.invalidate()
        repository.loadConfiguration().getOrThrow()

        loader.callCount(path = SPEC_PATH) shouldBe 2
    }

    @Test
    fun `invalidate then loadConfiguration reflects a changed spec file`() = runTest {
        val loader = MutableResourceLoader(resources = baseResources())
        val repository =
            MockConfigRepository(specPaths = listOf(SPEC_PATH), resourceLoader = loader)

        val before = repository.loadConfiguration().getOrThrow()
        before.specs[0].operations.map { it.operationId } shouldContainExactly
            listOf("getUser", "createUser")

        loader.replace(path = SPEC_PATH, content = specJsonWithDeleteUserAdded())
        repository.invalidate()
        val after = repository.loadConfiguration().getOrThrow()

        after.specs[0].operations.map { it.operationId } shouldContainExactly
            listOf("getUser", "deleteUser", "createUser")
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
        match?.config?.method shouldBe HttpMethod.Get
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
    fun `parses required fields from a requestBody schema into requestBodyMatch`() = runTest {
        val spec = """
            {
              "info": { "title": "Example" },
              "servers": [ { "url": "https://api.example.com" } ],
              "paths": {
                "/api/users": {
                  "post": {
                    "operationId": "createUser",
                    "requestBody": {
                      "content": {
                        "application/json": {
                          "schema": {
                            "type": "object",
                            "required": ["name", "email"]
                          }
                        }
                      }
                    },
                    "responses": {}
                  }
                }
              }
            }
        """.trimIndent()
        val repository = createRepository(resources = mapOf(SPEC_PATH to spec))

        val config = repository.loadConfiguration().getOrThrow()

        val operation = config.specs[0].operations.single()
        operation.requestBodyMatch?.requiredFields shouldContainExactly listOf("name", "email")
        operation.requestBodyMatch?.discriminatorField.shouldBeNull()
    }

    @Test
    fun `parses discriminator field and its single-value enum from a requestBody schema`() = runTest {
        val spec = """
            {
              "info": { "title": "Example" },
              "servers": [ { "url": "https://api.example.com" } ],
              "paths": {
                "/api/payments": {
                  "post": {
                    "operationId": "createCardPayment",
                    "requestBody": {
                      "content": {
                        "application/json": {
                          "schema": {
                            "type": "object",
                            "discriminator": { "propertyName": "type" },
                            "properties": {
                              "type": { "type": "string", "enum": ["card"] }
                            }
                          }
                        }
                      }
                    },
                    "responses": {}
                  }
                }
              }
            }
        """.trimIndent()
        val repository = createRepository(resources = mapOf(SPEC_PATH to spec))

        val config = repository.loadConfiguration().getOrThrow()

        val requestBodyMatch = config.specs[0].operations.single().requestBodyMatch
        requestBodyMatch?.discriminatorField shouldBe "type"
        requestBodyMatch?.discriminatorValue shouldBe "card"
    }

    @Test
    fun `requestBodyMatch is null when an operation declares no requestBody`() = runTest {
        val repository = createRepository(resources = baseResources())

        val config = repository.loadConfiguration().getOrThrow()

        config.specs[0].operations.first { it.operationId == "getUser" }
            .requestBodyMatch.shouldBeNull()
    }

    @Test
    fun `requestBodyMatch is null when the requestBody schema has neither required fields nor a discriminator`() =
        runTest {
            val spec = """
                {
                  "info": { "title": "Example" },
                  "servers": [ { "url": "https://api.example.com" } ],
                  "paths": {
                    "/api/users": {
                      "post": {
                        "operationId": "createUser",
                        "requestBody": {
                          "content": {
                            "application/json": {
                              "schema": { "type": "object" }
                            }
                          }
                        },
                        "responses": {}
                      }
                    }
                  }
                }
            """.trimIndent()
            val repository = createRepository(resources = mapOf(SPEC_PATH to spec))

            val config = repository.loadConfiguration().getOrThrow()

            config.specs[0].operations.single().requestBodyMatch.shouldBeNull()
        }

    @Test
    fun `findMatchingMock disambiguates two operations colliding on path and method by request body shape`() =
        runTest {
            val spec = """
                {
                  "info": { "title": "Example" },
                  "servers": [ { "url": "https://api.example.com" } ],
                  "paths": {
                    "/api/payments/card": {
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
                        "responses": {}
                      }
                    }
                  }
                }
            """.trimIndent()
            val repository = createRepository(resources = mapOf(SPEC_PATH to spec))

            val matchesCard = repository.findMatchingMock(
                host = "api.example.com",
                path = "/api/payments/card",
                method = "POST",
                requestBody = """{"type":"card","number":"4242"}"""
            )
            val doesNotMatchOtherType = repository.findMatchingMock(
                host = "api.example.com",
                path = "/api/payments/card",
                method = "POST",
                requestBody = """{"type":"bank_transfer"}"""
            )

            matchesCard?.operationId shouldBe "payByCard"
            doesNotMatchOtherType.shouldBeNull()
        }

    @Test
    fun `findMatchingMock resolves a dollar-ref'd requestBody schema via components schemas`() = runTest {
        val spec = $$"""
            {
              "info": { "title": "Example" },
              "servers": [ { "url": "https://api.example.com" } ],
              "paths": {
                "/api/users": {
                  "post": {
                    "operationId": "createUser",
                    "requestBody": {
                      "content": {
                        "application/json": {
                          "schema": { "$ref": "#/components/schemas/NewUser" }
                        }
                      }
                    },
                    "responses": {}
                  }
                }
              },
              "components": {
                "schemas": {
                  "NewUser": { "type": "object", "required": ["email"] }
                }
              }
            }
        """.trimIndent()
        val repository = createRepository(resources = mapOf(SPEC_PATH to spec))

        val matches = repository.findMatchingMock(
            host = "api.example.com",
            path = "/api/users",
            method = "POST",
            requestBody = """{"email":"bob@example.com"}"""
        )
        val noMatch = repository.findMatchingMock(
            host = "api.example.com",
            path = "/api/users",
            method = "POST",
            requestBody = """{"name":"Bob"}"""
        )

        matches?.operationId shouldBe "createUser"
        noMatch.shouldBeNull()
    }

    @Test
    fun `findMatchingMock picks the first spec that has a matching operation when hosts collide`() =
        runTest {
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
                resources = mapOf(
                    "specs/first.json" to firstSpec,
                    "specs/second.json" to secondSpec
                )
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
                resources = mapOf(
                    "specs/first.json" to firstSpec,
                    "specs/second.json" to secondSpec
                )
            )
            val repository = MockConfigRepository(
                specPaths = listOf("specs/first.json", "specs/second.json"),
                resourceLoader = loader
            )

            val match = repository.findMatchingMock(
                host = "api.example.com",
                path = "/api/target",
                method = "GET"
            )

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
    fun `findMatchingMock delayMs is null when neither operation nor spec declares one`() = runTest {
        // baseSpecJson declares no x-devview at any level - completes the precedence chain
        // (operation override, spec default) the test above covers with the "no delay at all" case.
        val repository = createRepository(resources = baseResources())

        val match = repository.findMatchingMock(
            host = "api.example.com",
            path = "/api/users/42",
            method = "GET"
        )

        match?.delayMs.shouldBeNull()
    }

    @Test
    fun `x-devview failureRate is parsed as an operation-level field with no spec-wide default`() =
        runTest {
            val spec = """
            {
              "info": { "title": "Example" },
              "servers": [ { "url": "https://api.example.com" } ],
              "x-devview": { "failureRate": 0.5 },
              "paths": {
                "/api/flaky": {
                  "get": {
                    "operationId": "flaky",
                    "x-devview": { "failureRate": 0.1 },
                    "responses": {}
                  }
                },
                "/api/steady": {
                  "get": { "operationId": "steady", "responses": {} }
                }
              }
            }
        """.trimIndent()
            val repository = createRepository(resources = mapOf(SPEC_PATH to spec))

            val config = repository.loadConfiguration().getOrThrow()
            val operations = config.specs[0].operations.associateBy { it.operationId }

            // Unlike delayMs, a document-root failureRate is not a spec-wide default.
            operations.getValue("flaky").failureRate shouldBe 0.1
            operations.getValue("steady").failureRate shouldBe null
        }

    @Test
    fun `operation version is extracted from a v-n path segment`() = runTest {
        val cases = mapOf(
            "/api/v1/profile/{userId}" to "v1",
            "/v2/users" to "v2",
            "/api/v10/x" to "v10",
            "/health" to null,
            "/users/{id}" to null,
            "/version/x" to null
        )
        val paths = cases.keys.mapIndexed { index, path ->
            """
            "$path": {
              "get": {
                "operationId": "op$index",
                "responses": {}
              }
            }
            """.trimIndent()
        }.joinToString(separator = ",\n")
        val spec = """
            {
              "info": { "title": "Example" },
              "servers": [ { "url": "https://api.example.com" } ],
              "paths": { $paths }
            }
        """.trimIndent()
        val repository = createRepository(resources = mapOf(SPEC_PATH to spec))

        val config = repository.loadConfiguration().getOrThrow()

        val versionByPath = config.specs.single().operations.associate { it.path to it.version }
        versionByPath shouldBe cases
    }

    @Test
    fun `operation tags are parsed from the OpenAPI tags array`() = runTest {
        val spec = """
            {
              "info": { "title": "Example" },
              "servers": [ { "url": "https://api.example.com" } ],
              "paths": {
                "/api/users": {
                  "get": {
                    "operationId": "listUsers",
                    "tags": ["Users", "Admin"],
                    "responses": {}
                  },
                  "post": {
                    "operationId": "createUser",
                    "responses": {}
                  }
                }
              }
            }
        """.trimIndent()
        val repository = createRepository(resources = mapOf(SPEC_PATH to spec))

        val config = repository.loadConfiguration().getOrThrow()
        val operations = config.specs[0].operations.associateBy { it.operationId }

        operations.getValue("listUsers").tags shouldContainExactly listOf("Users", "Admin")
        // No tags declared at all - defaults to an empty list, not null, same as queryParameters
        // defaulting to null rather than every operation carrying a placeholder.
        operations.getValue("createUser").tags shouldBe emptyList()
    }

    @Test
    fun `local dollar-ref to a components response resolves correctly`() = runTest {
        val spec = $$"""
            {
              "info": { "title": "Example" },
              "servers": [ { "url": "https://api.example.com" } ],
              "paths": {
                "/api/users/{userId}": {
                  "get": {
                    "operationId": "getUser",
                    "responses": {
                      "200": { "$ref": "#/components/responses/UserOk" }
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

        val responses = repository.discoverResponseFiles(
            key = OperationKey(
                specId = "example",
                operationId = "getUser"
            )
        )

        responses shouldHaveSize 1
        responses.single().statusCode shouldBe 200
        responses.single().content shouldBe """{"id":1}"""
    }

    @Test
    fun `external dollar-ref to another file's components resolves correctly`() = runTest {
        val spec = $$"""
            {
              "info": { "title": "Example" },
              "servers": [ { "url": "https://api.example.com" } ],
              "paths": {
                "/api/users/{userId}": {
                  "get": {
                    "operationId": "getUser",
                    "responses": {
                      "200": { "$ref": "./common.json#/components/responses/UserOk" }
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

        val responses = repository.discoverResponseFiles(
            key = OperationKey(
                specId = "example",
                operationId = "getUser"
            )
        )

        responses shouldHaveSize 1
        responses.single().content shouldBe """{"id":1}"""
    }

    @Test
    fun `dollar-ref naming the wrong components section is rejected even if a same-named entry exists there`() =
        runTest {
            val spec = $$"""
            {
              "info": { "title": "Example" },
              "servers": [ { "url": "https://api.example.com" } ],
              "paths": {
                "/api/users/{userId}": {
                  "get": {
                    "operationId": "getUser",
                    "responses": {
                      "200": { "$ref": "#/components/parameters/UserOk" }
                    }
                  }
                }
              },
              "components": {
                "parameters": {
                  "UserOk": { "name": "userOk", "in": "query", "example": "not-a-response" }
                },
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

            val result = repository.loadConfiguration()

            result.isFailure shouldBe true
            result.exceptionOrNull()?.message.orEmpty() shouldContain "expected a 'responses' entry"
        }

    @Test
    fun `local dollar-ref chain of two hops resolves to the final non-ref entry`() = runTest {
        val spec = $$"""
            {
              "info": { "title": "Example" },
              "servers": [ { "url": "https://api.example.com" } ],
              "paths": {
                "/api/users/{userId}": {
                  "get": {
                    "operationId": "getUser",
                    "responses": {
                      "200": { "$ref": "#/components/responses/A" }
                    }
                  }
                }
              },
              "components": {
                "responses": {
                  "A": { "$ref": "#/components/responses/B" },
                  "B": {
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

        val responses = repository.discoverResponseFiles(
            key = OperationKey(
                specId = "example",
                operationId = "getUser"
            )
        )

        responses shouldHaveSize 1
        responses.single().content shouldBe """{"id":1}"""
    }

    @Test
    fun `cyclic dollar-ref chain fails clearly instead of hanging`() = runTest {
        val spec = $$"""
            {
              "info": { "title": "Example" },
              "servers": [ { "url": "https://api.example.com" } ],
              "paths": {
                "/api/users/{userId}": {
                  "get": {
                    "operationId": "getUser",
                    "responses": {
                      "200": { "$ref": "#/components/responses/A" }
                    }
                  }
                }
              },
              "components": {
                "responses": {
                  "A": { "$ref": "#/components/responses/B" },
                  "B": { "$ref": "#/components/responses/A" }
                }
              }
            }
        """.trimIndent()
        val repository = createRepository(resources = mapOf(SPEC_PATH to spec))

        val result = repository.loadConfiguration()

        result.isFailure shouldBe true
        result.exceptionOrNull()?.message.orEmpty() shouldContain "cyclic reference detected"
    }

    @Test
    fun `discoverResponseFiles synthesizes a body from schema when a status code declares no examples`() = runTest {
        val spec = """
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
                            "schema": {
                              "type": "object",
                              "properties": {
                                "id": { "type": "integer" },
                                "name": { "type": "string" }
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
        val repository = createRepository(resources = mapOf(SPEC_PATH to spec))

        val responses = repository.discoverResponseFiles(
            key = OperationKey(specId = "example", operationId = "getUser")
        )

        responses shouldHaveSize 1
        val synthesized = responses.single()
        synthesized.isSynthesized shouldBe true
        synthesized.content shouldBe """{"id":0,"name":"string"}"""
    }

    @Test
    fun `discoverResponseFiles prefers declared examples over schema synthesis`() = runTest {
        val spec = """
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
                            "schema": { "type": "object" },
                            "examples": {
                              "default": { "externalValue": "/responses/getUser-200.json" }
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
        val repository = createRepository(
            resources = mapOf(SPEC_PATH to spec, "responses/getUser-200.json" to """{"id":1}""")
        )

        val responses = repository.discoverResponseFiles(
            key = OperationKey(specId = "example", operationId = "getUser")
        )

        responses shouldHaveSize 1
        val response = responses.single()
        response.isSynthesized shouldBe false
        response.content shouldBe """{"id":1}"""
    }

    @Test
    fun `discoverResponseFiles resolves a dollar-ref'd schema via components schemas before synthesizing`() = runTest {
        val spec = $$"""
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
                            "schema": { "$ref": "#/components/schemas/User" }
                          }
                        }
                      }
                    }
                  }
                }
              },
              "components": {
                "schemas": {
                  "User": {
                    "type": "object",
                    "properties": { "id": { "type": "integer" } }
                  }
                }
              }
            }
        """.trimIndent()
        val repository = createRepository(resources = mapOf(SPEC_PATH to spec))

        val responses = repository.discoverResponseFiles(
            key = OperationKey(specId = "example", operationId = "getUser")
        )

        responses shouldHaveSize 1
        responses.single().content shouldBe """{"id":0}"""
    }

    @Test
    fun `parses a real-world YAML spec with dollar-ref schemas declared after paths`() = runTest {
        // Mirrors a shape seen in real API docs: a YAML spec whose components.schemas section
        // sits after paths, a requestBody with its own $ref'd schema and a requestBody.required
        // boolean (a different concept from schema.required, and not modeled at all - must be
        // silently ignored), three status codes all $ref-ing the *same* response schema, a
        // folded (unquoted, line-wrapped) summary string, a double-quoted description with a
        // backslash line continuation, and a tags block sequence.
        val yamlSpec = $$"""
            info:
              title: Example
            servers:
            - url: https://api.example.com
            paths:
              /api/v1/authentication/mobile-auth/login:
                post:
                  operationId: mobileLogin
                  requestBody:
                    content:
                      application/json:
                        schema:
                          $ref: "#/components/schemas/MobileLoginRequest"
                    required: true
                  responses:
                    "200":
                      content:
                        application/json:
                          schema:
                            $ref: "#/components/schemas/MobileLoginResponse"
                      description: "Successful call, returns a challenge that needs to be signed\
                        \ to complete the activation"
                    "401":
                      content:
                        application/json:
                          schema:
                            $ref: "#/components/schemas/MobileLoginResponse"
                      description: User not authenticated
                    "422":
                      content:
                        application/json:
                          schema:
                            $ref: "#/components/schemas/MobileLoginResponse"
                      description: Invalid parameters
                  summary: Init mobile authentication activation workflow. It will reset any previously
                    activated mobile authentication for this user and device.
                  tags:
                  - Authentication V1
                  - Authentication
            components:
              schemas:
                MobileLoginRequest:
                  type: object
                  required:
                  - deviceId
                  properties:
                    deviceId:
                      type: string
                MobileLoginResponse:
                  type: object
                  properties:
                    challenge:
                      type: string
                    expiresInSeconds:
                      type: integer
        """.trimIndent()
        val yamlSpecPath = "specs/mobile-auth.yaml"
        val repository = MockConfigRepository(
            specPaths = listOf(yamlSpecPath),
            resourceLoader = RecordingResourceLoader(resources = mapOf(yamlSpecPath to yamlSpec))
        )

        val config = repository.loadConfiguration().getOrThrow()
        val operation = config.specs[0].operations.single()

        operation.operationId shouldBe "mobileLogin"
        operation.path shouldBe "/api/v1/authentication/mobile-auth/login"
        operation.method shouldBe HttpMethod.Post
        // Folded YAML scalar: the line break becomes a single space.
        operation.name shouldBe "Init mobile authentication activation workflow. It will reset " +
            "any previously activated mobile authentication for this user and device."
        operation.requestBodyMatch?.requiredFields shouldContainExactly listOf("deviceId")
        operation.tags shouldContainExactly listOf("Authentication V1", "Authentication")

        val responses = repository.discoverResponseFiles(
            key = OperationKey(specId = "example", operationId = "mobileLogin")
        )

        responses shouldHaveSize 3
        responses.map { it.statusCode }.sorted() shouldContainExactly listOf(200, 401, 422)
        responses.all { it.isSynthesized } shouldBe true
        responses.all { it.content == """{"challenge":"string","expiresInSeconds":0}""" } shouldBe true
    }

    @Test
    fun `discoverResponseFiles returns responses sorted by status code`() = runTest {
        val repository = createRepository(resources = baseResources())

        val responses = repository.discoverResponseFiles(
            key = OperationKey(
                specId = "example",
                operationId = "getUser"
            )
        )

        responses.map { it.statusCode } shouldBe listOf(200, 404)
    }

    @Test
    fun `discoverResponseFiles discovers named example variants`() = runTest {
        val repository = createRepository(resources = multiExampleResources())

        val responses = repository.discoverResponseFiles(
            key = OperationKey(
                specId = "example",
                operationId = "getUser"
            )
        )

        responses shouldHaveSize 3
        responses.map { it.exampleName } shouldContain "detailed"
    }

    @Test
    fun `discoverResponseFiles preserves declared example order within a status code`() = runTest {
        val repository = createRepository(resources = multiExampleResources())

        val responses = repository.discoverResponseFiles(
            key = OperationKey(
                specId = "example",
                operationId = "getUser"
            )
        )

        responses.filter { it.statusCode == 404 }.map { it.exampleName }
            .toSet() shouldBe setOf("default", "detailed")
    }

    @Test
    fun `discoverResponseFiles returns empty list when operation declares no responses`() =
        runTest {
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
        response?.contentType shouldBe "application/json"
        response?.headers shouldBe emptyMap()
    }

    @Test
    fun `loadMockResponse resolves declared content type and headers`() = runTest {
        val resources = mapOf(
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
                            "headers": {
                              "X-RateLimit-Remaining": { "example": "42" },
                              "Cache-Control": { "example": "no-store" }
                            },
                            "content": {
                              "application/vnd.example+json": {
                                "examples": {
                                  "default": { "externalValue": "/responses/getUser-200.json" }
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
            "responses/getUser-200.json" to """{"id":1}"""
        )
        val repository = createRepository(resources = resources)

        val response = repository.loadMockResponse(
            key = OperationKey(specId = "example", operationId = "getUser"),
            statusCode = 200,
            exampleName = "default"
        )

        response?.contentType shouldBe "application/vnd.example+json"
        response?.headers shouldBe mapOf(
            "X-RateLimit-Remaining" to "42",
            "Cache-Control" to "no-store"
        )
    }

    @Test
    fun `loadMockResponse resolves a dollar-ref'd header via components`() = runTest {
        val resources = mapOf(
            SPEC_PATH to $$"""
                {
                  "info": { "title": "Example" },
                  "servers": [ { "url": "https://api.example.com" } ],
                  "paths": {
                    "/api/users/{userId}": {
                      "get": {
                        "operationId": "getUser",
                        "responses": {
                          "200": {
                            "headers": {
                              "X-RateLimit-Remaining": { "$ref": "#/components/headers/RateLimit" }
                            },
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
                  },
                  "components": {
                    "headers": {
                      "RateLimit": { "example": "10" }
                    }
                  }
                }
            """.trimIndent(),
            "responses/getUser-200.json" to """{"id":1}"""
        )
        val repository = createRepository(resources = resources)

        val response = repository.loadMockResponse(
            key = OperationKey(specId = "example", operationId = "getUser"),
            statusCode = 200,
            exampleName = "default"
        )

        response?.headers shouldBe mapOf("X-RateLimit-Remaining" to "10")
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
        MockConfigRepository(
            specPaths = listOf(SPEC_PATH),
            resourceLoader = RecordingResourceLoader(resources)
        )

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

    /** Like [RecordingResourceLoader], but [replace] lets a test simulate an edited spec file. */
    private class MutableResourceLoader(
        resources: Map<String, String>
    ) : NetworkMockResourceLoader {
        private val resources = resources.toMutableMap()

        override suspend fun load(path: String): ByteArray =
            resources[path]?.encodeToByteArray() ?: error("Resource not found: $path")

        fun replace(path: String, content: String) {
            resources[path] = content
        }
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

    /** [baseSpecJson] with a `deleteUser` operation added — simulates an edited spec file. */
    private fun specJsonWithDeleteUserAdded(): String = """
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
              },
              "delete": {
                "operationId": "deleteUser",
                "summary": "Delete User",
                "responses": {}
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
