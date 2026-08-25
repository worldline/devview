package com.worldline.devview.networkmock.ktor.fixtures

/**
 * Shared test resource strings for the Ktor plugin tests.
 */
internal object KtorPluginTestData {

    const val SPEC_PATH: String = "files/networkmocks/specs/example.json"

    val defaultSpecJson: String = """
        {
          "info": { "title": "Example" },
          "servers": [
            { "url": "https://staging.api.example.com" },
            { "url": "https://api.example.com" }
          ],
          "paths": {
            "/api/users/{userId}": {
              "get": {
                "operationId": "getUser",
                "responses": {
                  "200": {
                    "content": {
                      "application/json": {
                        "examples": {
                          "default": { "externalValue": "/files/networkmocks/responses/getUser-200.json" }
                        }
                      }
                    }
                  },
                  "404": {
                    "content": {
                      "application/json": {
                        "examples": {
                          "default": { "externalValue": "/files/networkmocks/responses/getUser-404.json" }
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
                "responses": {
                  "201": {
                    "content": {
                      "application/json": {
                        "examples": {
                          "default": { "externalValue": "/files/networkmocks/responses/createUser-201.json" }
                        }
                      }
                    }
                  }
                }
              }
            },
            "/api/products/{productId}": {
              "get": {
                "operationId": "getProduct",
                "responses": {
                  "200": {
                    "content": {
                      "application/json": {
                        "examples": {
                          "default": { "externalValue": "/files/networkmocks/responses/getProduct-200.json" }
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

    /** Response file resources keyed by their path under `files/networkmocks/`. */
    val responseResources: Map<String, String> = mapOf(
        SPEC_PATH to defaultSpecJson,
        "files/networkmocks/responses/getUser-200.json" to """{"id":1,"name":"Alice"}""",
        "files/networkmocks/responses/getUser-404.json" to """{"error":"not found"}""",
        "files/networkmocks/responses/createUser-201.json" to """{"id":2}""",
        "files/networkmocks/responses/getProduct-200.json" to """{"id":10,"name":"Widget"}"""
    )

    /** Resource loader backed by the in-memory map above. */
    fun resourceLoader(
        resources: Map<String, String> = responseResources
    ): suspend (String) -> ByteArray =
        { path -> resources[path]?.encodeToByteArray() ?: error("Resource not found: $path") }
}
