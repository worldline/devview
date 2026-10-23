package com.worldline.devview.networkmock.core.repository

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ContentTypeResolutionTest {

    @Test
    fun concreteDeclaredType_passesThroughUnchanged_regardlessOfBody() {
        resolveContentType(declared = "application/json", content = "hello") shouldBe "application/json"
        resolveContentType(declared = "application/vnd.api+json", content = "{}") shouldBe "application/vnd.api+json"
        resolveContentType(declared = "text/html", content = "<html></html>") shouldBe "text/html"
    }

    @Test
    fun blankDeclaredType_passesThroughUnchanged() {
        resolveContentType(declared = "", content = "") shouldBe ""
        resolveContentType(declared = "", content = "{}") shouldBe ""
    }

    @Test
    fun wildcardDeclaredType_withBlankContent_passesThroughUnchanged() {
        resolveContentType(declared = "*/*", content = "") shouldBe "*/*"
        resolveContentType(declared = "*/*", content = "   ") shouldBe "*/*"
        resolveContentType(declared = "application/*", content = "") shouldBe "application/*"
    }

    @Test
    fun starStar_withJsonContent_resolvesToApplicationJson() {
        resolveContentType(declared = "*/*", content = """{"ipagoId": "ipago-773201"}""") shouldBe "application/json"
        resolveContentType(declared = "*/*", content = "[1, 2, 3]") shouldBe "application/json"
        resolveContentType(declared = "*/*", content = "true") shouldBe "application/json"
    }

    @Test
    fun starStar_withNonJsonContent_resolvesToTextPlain() {
        resolveContentType(declared = "*/*", content = "plain text response") shouldBe "text/plain"
        resolveContentType(declared = "*/*", content = "not { valid json") shouldBe "text/plain"
    }

    @Test
    fun applicationStar_withJsonContent_resolvesToApplicationJson() {
        resolveContentType(declared = "application/*", content = """{"status": "ok"}""") shouldBe "application/json"
    }

    @Test
    fun applicationStar_withNonJsonContent_resolvesToApplicationOctetStream() {
        resolveContentType(declared = "application/*", content = "binary or arbitrary") shouldBe "application/octet-stream"
    }

    @Test
    fun textStar_alwaysResolvesToTextPlain() {
        resolveContentType(declared = "text/*", content = "plain text") shouldBe "text/plain"
        resolveContentType(declared = "text/*", content = """{"looksLike": "json"}""") shouldBe "text/plain"
    }

    @Test
    fun unhandledWildcardPrefix_passesThroughAsShortcut() {
        resolveContentType(declared = "image/*", content = "bytes") shouldBe "image/*"
        resolveContentType(declared = "audio/*", content = "bytes") shouldBe "audio/*"
    }
}
