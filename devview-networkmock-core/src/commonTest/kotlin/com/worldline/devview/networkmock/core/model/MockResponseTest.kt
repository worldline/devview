package com.worldline.devview.networkmock.core.model

import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertNotNull

class MockResponseTest {

    @Test
    fun `fromFile returns null for invalid file names`() {
        MockResponse.fromFile(fileName = "invalid.json", content = "{}") shouldBe null
        MockResponse.fromFile(fileName = "endpoint-no-status.json", content = "{}") shouldBe null
        MockResponse.fromFile(fileName = "", content = "{}") shouldBe null
    }

    @Test
    fun `fromFile builds response with default status text and no suffix`() {
        val response = MockResponse.fromFile(
            fileName = "getUser-200.json",
            content = "{\"id\":\"1\"}"
        )

        assertNotNull(response)
        response.statusCode shouldBe 200
        response.fileName shouldBe "getUser-200.json"
        response.displayName shouldBe "Success (200)"
        response.content shouldBe "{\"id\":\"1\"}"
    }

    @Test
    fun `fromFile capitalizes single suffix token in display name`() {
        val response = MockResponse.fromFile(
            fileName = "getUser-404-simple.json",
            content = "{}"
        )

        assertNotNull(response)
        response.statusCode shouldBe 404
        response.displayName shouldBe "Not Found - Simple (404)"
    }

    @Test
    fun `fromFile capitalizes multi token suffix and preserves spaces`() {
        val response = MockResponse.fromFile(
            fileName = "getUser-404-not-found.json",
            content = "{}"
        )

        assertNotNull(response)
        response.displayName shouldBe "Not Found - Not Found (404)"
    }

    @Test
    fun `fromFile supports hyphenated endpoint id`() {
        val response = MockResponse.fromFile(
            fileName = "get-user-profile-201.json",
            content = "{}"
        )

        assertNotNull(response)
        response.statusCode shouldBe 201
        response.displayName shouldBe "Created (201)"
    }

    @Test
    fun `fromFile falls back to HTTP code text when status is unknown`() {
        val response = MockResponse.fromFile(
            fileName = "batch-599.json",
            content = "{}"
        )

        assertNotNull(response)
        response.statusCode shouldBe 599
        response.displayName shouldBe "HTTP 599 (599)"
    }

    @Test
    fun `fromFile uses custom status text provider`() {
        val response = MockResponse.fromFile(
            fileName = "checkout-422-validation-error.json",
            content = "{}",
            statusTextProvider = { code -> "Code $code" }
        )

        assertNotNull(response)
        response.statusCode shouldBe 422
        response.displayName shouldBe "Code 422 - Validation Error (422)"
    }
}

