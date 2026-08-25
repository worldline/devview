package com.worldline.devview.networkmock.core.model

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class MockResponseTest {

    @Test
    fun `create builds response with default status text and no suffix for the default example`() {
        val response = MockResponse.create(
            statusCode = 200,
            exampleName = "default",
            content = "{\"id\":\"1\"}"
        )

        response.statusCode shouldBe 200
        response.exampleName shouldBe "default"
        response.displayName shouldBe "Success (200)"
        response.content shouldBe "{\"id\":\"1\"}"
    }

    @Test
    fun `create is case-insensitive when detecting the default example`() {
        val response = MockResponse.create(statusCode = 200, exampleName = "Default", content = "{}")

        response.displayName shouldBe "Success (200)"
    }

    @Test
    fun `create capitalizes a single-word example name in display name`() {
        val response = MockResponse.create(statusCode = 404, exampleName = "simple", content = "{}")

        response.displayName shouldBe "Not Found - Simple (404)"
    }

    @Test
    fun `create capitalizes a multi-word hyphenated example name and preserves spaces`() {
        val response = MockResponse.create(statusCode = 404, exampleName = "not-found", content = "{}")

        response.displayName shouldBe "Not Found - Not Found (404)"
    }

    @Test
    fun `create falls back to HTTP code text when status is unknown`() {
        val response = MockResponse.create(statusCode = 599, exampleName = "default", content = "{}")

        response.displayName shouldBe "HTTP 599 (599)"
    }

    @Test
    fun `create uses custom status text provider`() {
        val response = MockResponse.create(
            statusCode = 422,
            exampleName = "validation-error",
            content = "{}",
            statusTextProvider = { code -> "Code $code" }
        )

        response.displayName shouldBe "Code 422 - Validation Error (422)"
    }
}
