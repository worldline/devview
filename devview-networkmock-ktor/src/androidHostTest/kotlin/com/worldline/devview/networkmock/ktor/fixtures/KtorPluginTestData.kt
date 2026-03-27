package com.worldline.devview.networkmock.ktor.fixtures

/**
 * Shared test resource strings for the Ktor plugin tests.
 */
internal object KtorPluginTestData {

    val defaultConfigJson: String = """
        {
          "hosts": [
            {
              "id": "staging",
              "url": "https://staging.api.example.com",
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

    /** Response file resources keyed by their path under `files/networkmocks/`. */
    val responseResources: Map<String, String> = mapOf(
        "files/networkmocks/mocks.json" to defaultConfigJson,
        "files/networkmocks/responses/getUser/getUser-200.json" to """{"id":1,"name":"Alice"}""",
        "files/networkmocks/responses/getUser/getUser-404.json" to """{"error":"not found"}""",
        "files/networkmocks/responses/createUser/createUser-201.json" to """{"id":2}""",
        "files/networkmocks/responses/getProduct/getProduct-200.json" to """{"id":10,"name":"Widget"}"""
    )

    /** Resource loader backed by the in-memory map above. */
    fun resourceLoader(
        resources: Map<String, String> = responseResources
    ): suspend (String) -> ByteArray =
        { path -> resources[path]?.encodeToByteArray() ?: error("Resource not found: $path") }
}

