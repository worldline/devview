package com.worldline.devview.networkmock.core.repository

import com.worldline.devview.networkmock.core.model.RequestBodyMatch
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class RequestMatcherTest {

    // region Exact path matching (no parameters)

    @Test
    fun `matches identical single-segment paths`() {
        RequestMatcher.matchesPath(
            configPath = "/api",
            requestPath = "/api"
        ) shouldBe true
    }

    @Test
    fun `matches identical multi-segment paths`() {
        RequestMatcher.matchesPath(
            configPath = "/api/users",
            requestPath = "/api/users"
        ) shouldBe true
    }

    @Test
    fun `rejects paths with different final segment`() {
        RequestMatcher.matchesPath(
            configPath = "/api/users",
            requestPath = "/api/posts"
        ) shouldBe false
    }

    @Test
    fun `rejects paths with different leading segment`() {
        RequestMatcher.matchesPath(
            configPath = "/v1/users",
            requestPath = "/v2/users"
        ) shouldBe false
    }

    // endregion

    // region Segment count mismatch

    @Test
    fun `rejects request path with more segments than config`() {
        RequestMatcher.matchesPath(
            configPath = "/api/users",
            requestPath = "/api/users/123"
        ) shouldBe false
    }

    @Test
    fun `rejects request path with fewer segments than config`() {
        RequestMatcher.matchesPath(
            configPath = "/api/users/profile",
            requestPath = "/api/users"
        ) shouldBe false
    }

    @Test
    fun `rejects parameterized config path with extra request segment`() {
        RequestMatcher.matchesPath(
            configPath = "/api/users/{id}",
            requestPath = "/api/users/42/details"
        ) shouldBe false
    }

    // endregion

    // region Single path parameter

    @Test
    fun `matches single path parameter with numeric value`() {
        RequestMatcher.matchesPath(
            configPath = "/api/users/{id}",
            requestPath = "/api/users/42"
        ) shouldBe true
    }

    @Test
    fun `matches single path parameter with alphabetic value`() {
        RequestMatcher.matchesPath(
            configPath = "/api/users/{id}",
            requestPath = "/api/users/abc"
        ) shouldBe true
    }

    @Test
    fun `matches single path parameter with hyphenated value`() {
        RequestMatcher.matchesPath(
            configPath = "/api/users/{id}",
            requestPath = "/api/users/user-123"
        ) shouldBe true
    }

    @Test
    fun `rejects single parameter when non-parameter segment differs`() {
        RequestMatcher.matchesPath(
            configPath = "/api/users/{id}",
            requestPath = "/api/posts/42"
        ) shouldBe false
    }

    // endregion

    // region Multiple path parameters

    @Test
    fun `matches multiple path parameters with numeric values`() {
        RequestMatcher.matchesPath(
            configPath = "/api/posts/{postId}/comments/{commentId}",
            requestPath = "/api/posts/123/comments/456"
        ) shouldBe true
    }

    @Test
    fun `matches multiple path parameters with mixed values`() {
        RequestMatcher.matchesPath(
            configPath = "/api/posts/{postId}/comments/{commentId}",
            requestPath = "/api/posts/abc/comments/xyz"
        ) shouldBe true
    }

    @Test
    fun `rejects multiple parameters when a literal segment differs`() {
        RequestMatcher.matchesPath(
            configPath = "/api/posts/{postId}/comments/{commentId}",
            requestPath = "/api/posts/123/likes/456"
        ) shouldBe false
    }

    // endregion

    // region Leading and trailing slash normalisation

    @Test
    fun `matches when request path has no leading slash`() {
        RequestMatcher.matchesPath(
            configPath = "/api/users",
            requestPath = "api/users"
        ) shouldBe true
    }

    @Test
    fun `matches when config path has no leading slash`() {
        RequestMatcher.matchesPath(
            configPath = "api/users",
            requestPath = "/api/users"
        ) shouldBe true
    }

    @Test
    fun `matches when config path has trailing slash`() {
        RequestMatcher.matchesPath(
            configPath = "/api/users/",
            requestPath = "/api/users"
        ) shouldBe true
    }

    @Test
    fun `matches when request path has trailing slash`() {
        RequestMatcher.matchesPath(
            configPath = "/api/users",
            requestPath = "/api/users/"
        ) shouldBe true
    }

    @Test
    fun `matches when both paths have no leading or trailing slashes`() {
        RequestMatcher.matchesPath(
            configPath = "api/users",
            requestPath = "api/users/"
        ) shouldBe true
    }

    // endregion

    // region Case sensitivity

    @Test
    fun `is case-sensitive for literal segments`() {
        RequestMatcher.matchesPath(
            configPath = "/api/Users",
            requestPath = "/api/users"
        ) shouldBe false
    }

    @Test
    fun `is case-sensitive for method segment`() {
        RequestMatcher.matchesPath(
            configPath = "/api/users/Profile",
            requestPath = "/api/users/profile"
        ) shouldBe false
    }

    // endregion

    // region Edge cases — empty and root paths

    @Test
    fun `matches two empty strings`() {
        RequestMatcher.matchesPath(
            configPath = "",
            requestPath = ""
        ) shouldBe true
    }

    @Test
    fun `matches two root slash paths`() {
        RequestMatcher.matchesPath(
            configPath = "/",
            requestPath = "/"
        ) shouldBe true
    }

    @Test
    fun `matches root slash config against empty request`() {
        // Both reduce to zero segments after filtering
        RequestMatcher.matchesPath(
            configPath = "/",
            requestPath = ""
        ) shouldBe true
    }

    @Test
    fun `rejects non-empty config against empty request`() {
        RequestMatcher.matchesPath(
            configPath = "/api",
            requestPath = ""
        ) shouldBe false
    }

    @Test
    fun `rejects non-empty config against root slash request`() {
        RequestMatcher.matchesPath(
            configPath = "/api",
            requestPath = "/"
        ) shouldBe false
    }

    // endregion

    // region Edge cases — double slashes (empty segments are ignored)

    @Test
    fun `matches when config path has consecutive slashes`() {
        // double slash in the middle produces an empty segment that is filtered out
        RequestMatcher.matchesPath(
            configPath = "/api//users",
            requestPath = "/api/users"
        ) shouldBe true
    }

    @Test
    fun `matches when request path has consecutive slashes`() {
        RequestMatcher.matchesPath(
            configPath = "/api/users",
            requestPath = "/api//users"
        ) shouldBe true
    }

    @Test
    fun `matches when both paths have consecutive slashes in the same position`() {
        RequestMatcher.matchesPath(
            configPath = "/api//users",
            requestPath = "/api//users"
        ) shouldBe true
    }

    // endregion

    // region Edge cases — special characters in literal segments

    @Test
    fun `matches path segments containing dots`() {
        RequestMatcher.matchesPath(
            configPath = "/api/v1.0/users",
            requestPath = "/api/v1.0/users"
        ) shouldBe true
    }

    @Test
    fun `matches path segments containing underscores`() {
        RequestMatcher.matchesPath(
            configPath = "/api/user_profile",
            requestPath = "/api/user_profile"
        ) shouldBe true
    }

    @Test
    fun `matches path segments containing percent-encoded characters`() {
        RequestMatcher.matchesPath(
            configPath = "/api/search/hello%20world",
            requestPath = "/api/search/hello%20world"
        ) shouldBe true
    }

    @Test
    fun `rejects path segments with different percent-encoded values`() {
        RequestMatcher.matchesPath(
            configPath = "/api/search/hello%20world",
            requestPath = "/api/search/hello+world"
        ) shouldBe false
    }

    // endregion

    // region Edge cases — parameter boundary conditions

    @Test
    fun `treats empty braces as a literal segment not a parameter`() {
        // "{}" has length 2 so isParameterSegment returns false — must match literally
        RequestMatcher.matchesPath(
            configPath = "/api/{}",
            requestPath = "/api/anything"
        ) shouldBe false
    }

    @Test
    fun `treats empty braces literal as exact match`() {
        RequestMatcher.matchesPath(
            configPath = "/api/{}",
            requestPath = "/api/{}"
        ) shouldBe true
    }

    @Test
    fun `matches parameter segment that contains only one character name`() {
        RequestMatcher.matchesPath(
            configPath = "/api/{x}",
            requestPath = "/api/42"
        ) shouldBe true
    }

    @Test
    fun `matches parameter segment with hyphenated parameter name`() {
        RequestMatcher.matchesPath(
            configPath = "/api/{post-id}",
            requestPath = "/api/123"
        ) shouldBe true
    }

    @Test
    fun `matches path parameter whose value contains dots`() {
        RequestMatcher.matchesPath(
            configPath = "/api/files/{filename}",
            requestPath = "/api/files/report.pdf"
        ) shouldBe true
    }

    @Test
    fun `matches path parameter whose value is a UUID`() {
        RequestMatcher.matchesPath(
            configPath = "/api/users/{id}",
            requestPath = "/api/users/550e8400-e29b-41d4-a716-446655440000"
        ) shouldBe true
    }

    // endregion

    // region Query parameter matching

    @Test
    fun `matchesQueryParams returns true when configQueryParams is null`() {
        RequestMatcher.matchesQueryParams(
            configQueryParams = null,
            requestQueryParams = mapOf("page" to listOf("1"))
        ) shouldBe true
    }

    @Test
    fun `matchesQueryParams returns true when configQueryParams is empty`() {
        RequestMatcher.matchesQueryParams(
            configQueryParams = emptyMap(),
            requestQueryParams = mapOf("page" to listOf("1"))
        ) shouldBe true
    }

    @Test
    fun `matchesQueryParams returns true when all config params are present`() {
        RequestMatcher.matchesQueryParams(
            configQueryParams = mapOf("type" to "user"),
            requestQueryParams = mapOf("type" to listOf("user"), "page" to listOf("1"))
        ) shouldBe true
    }

    @Test
    fun `matchesQueryParams returns false when a config param is missing from request`() {
        RequestMatcher.matchesQueryParams(
            configQueryParams = mapOf("type" to "user", "status" to "active"),
            requestQueryParams = mapOf("type" to listOf("user"))
        ) shouldBe false
    }

    @Test
    fun `matchesQueryParams returns false when a config param has wrong value`() {
        RequestMatcher.matchesQueryParams(
            configQueryParams = mapOf("type" to "user"),
            requestQueryParams = mapOf("type" to listOf("admin"))
        ) shouldBe false
    }

    @Test
    fun `matchesQueryParams returns true when config param value is one of multiple request values`() {
        RequestMatcher.matchesQueryParams(
            configQueryParams = mapOf("type" to "user"),
            requestQueryParams = mapOf("type" to listOf("admin", "user"))
        ) shouldBe true
    }

    @Test
    fun `matchesQueryParams returns true with multiple config params all present`() {
        RequestMatcher.matchesQueryParams(
            configQueryParams = mapOf("type" to "user", "status" to "active"),
            requestQueryParams = mapOf(
                "type" to listOf("user"),
                "status" to listOf("active"),
                "page" to listOf("1")
            )
        ) shouldBe true
    }

    @Test
    fun `matchesQueryParams returns false when request has no query params but config requires some`() {
        RequestMatcher.matchesQueryParams(
            configQueryParams = mapOf("type" to "user"),
            requestQueryParams = emptyMap()
        ) shouldBe false
    }

    // endregion

    // region Request body matching

    @Test
    fun `matchesRequestBody returns true when configMatch is null regardless of body`() {
        RequestMatcher.matchesRequestBody(
            configMatch = null,
            requestBody = """{"anything":"goes"}"""
        ) shouldBe true
        RequestMatcher.matchesRequestBody(configMatch = null, requestBody = null) shouldBe true
    }

    @Test
    fun `matchesRequestBody returns false when configMatch is non-null but requestBody is null`() {
        RequestMatcher.matchesRequestBody(
            configMatch = RequestBodyMatch(requiredFields = listOf("name")),
            requestBody = null
        ) shouldBe false
    }

    @Test
    fun `matchesRequestBody returns false when requestBody is not valid JSON`() {
        RequestMatcher.matchesRequestBody(
            configMatch = RequestBodyMatch(requiredFields = listOf("name")),
            requestBody = "not json"
        ) shouldBe false
    }

    @Test
    fun `matchesRequestBody returns false when requestBody is valid JSON but not an object`() {
        RequestMatcher.matchesRequestBody(
            configMatch = RequestBodyMatch(requiredFields = listOf("name")),
            requestBody = """["name"]"""
        ) shouldBe false
    }

    @Test
    fun `matchesRequestBody returns true when all required fields are present`() {
        RequestMatcher.matchesRequestBody(
            configMatch = RequestBodyMatch(requiredFields = listOf("name", "email")),
            requestBody = """{"name":"Bob","email":"bob@example.com"}"""
        ) shouldBe true
    }

    @Test
    fun `matchesRequestBody returns false when a required field is missing`() {
        RequestMatcher.matchesRequestBody(
            configMatch = RequestBodyMatch(requiredFields = listOf("name", "email")),
            requestBody = """{"name":"Bob"}"""
        ) shouldBe false
    }

    @Test
    fun `matchesRequestBody ignores the required field's actual value, only checks presence`() {
        RequestMatcher.matchesRequestBody(
            configMatch = RequestBodyMatch(requiredFields = listOf("name")),
            requestBody = """{"name":null}"""
        ) shouldBe true
    }

    @Test
    fun `matchesRequestBody returns true when discriminator field matches expected value`() {
        RequestMatcher.matchesRequestBody(
            configMatch = RequestBodyMatch(
                discriminatorField = "type",
                discriminatorValue = "card"
            ),
            requestBody = """{"type":"card","number":"4242"}"""
        ) shouldBe true
    }

    @Test
    fun `matchesRequestBody returns false when discriminator field has a different value`() {
        RequestMatcher.matchesRequestBody(
            configMatch = RequestBodyMatch(
                discriminatorField = "type",
                discriminatorValue = "card"
            ),
            requestBody = """{"type":"bank_transfer","iban":"..."}"""
        ) shouldBe false
    }

    @Test
    fun `matchesRequestBody returns false when discriminator field is absent from the body`() {
        RequestMatcher.matchesRequestBody(
            configMatch = RequestBodyMatch(
                discriminatorField = "type",
                discriminatorValue = "card"
            ),
            requestBody = """{"number":"4242"}"""
        ) shouldBe false
    }

    @Test
    fun `matchesRequestBody only checks discriminator field presence when no discriminatorValue is declared`() {
        val configMatch = RequestBodyMatch(discriminatorField = "type", discriminatorValue = null)

        RequestMatcher.matchesRequestBody(
            configMatch = configMatch,
            requestBody = """{"type":"anything"}"""
        ) shouldBe true
        RequestMatcher.matchesRequestBody(
            configMatch = configMatch,
            requestBody = """{"other":"field"}"""
        ) shouldBe false
    }

    @Test
    fun `matchesRequestBody requires both required fields and discriminator when both are declared`() {
        val configMatch = RequestBodyMatch(
            requiredFields = listOf("amount"),
            discriminatorField = "type",
            discriminatorValue = "card"
        )

        RequestMatcher.matchesRequestBody(
            configMatch = configMatch,
            requestBody = """{"amount":100,"type":"card"}"""
        ) shouldBe true
        RequestMatcher.matchesRequestBody(
            configMatch = configMatch,
            requestBody = """{"type":"card"}"""
        ) shouldBe false
        RequestMatcher.matchesRequestBody(
            configMatch = configMatch,
            requestBody = """{"amount":100,"type":"bank_transfer"}"""
        ) shouldBe false
    }

    // endregion
}

