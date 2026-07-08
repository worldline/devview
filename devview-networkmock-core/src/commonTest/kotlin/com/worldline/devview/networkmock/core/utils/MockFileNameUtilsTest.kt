package com.worldline.devview.networkmock.core.utils

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class MockFileNameUtilsTest {

    // region Valid file names

    @Test
    fun `parses status code from simple endpoint id`() {
        "getUser-200.json".parseStatusCode() shouldBe 200
    }

    @Test
    fun `parses status code from endpoint id with suffix`() {
        "getUser-404-simple.json".parseStatusCode() shouldBe 404
    }

    @Test
    fun `parses status code from endpoint id with multiple suffixes`() {
        "getUser-404-not-found.json".parseStatusCode() shouldBe 404
    }

    @Test
    fun `parses status code from hyphenated endpoint id`() {
        "get-user-500.json".parseStatusCode() shouldBe 500
    }

    @Test
    fun `parses status code from deeply hyphenated endpoint id with suffix`() {
        "get-user-profile-201-created.json".parseStatusCode() shouldBe 201
    }

    @Test
    fun `parses 3xx status code`() {
        "getUser-301.json".parseStatusCode() shouldBe 301
    }

    @Test
    fun `parses 5xx status code`() {
        "createUser-503.json".parseStatusCode() shouldBe 503
    }

    @Test
    fun `parses status code from file name without json extension`() {
        // removeSuffix(".json") is a no-op when the extension is absent,
        // so the regex still finds the three-digit code at the end of the string.
        "getUser-200".parseStatusCode() shouldBe 200
    }

    // endregion

    // region Invalid or malformed file names

    @Test
    fun `returns null for file name with no status code`() {
        "getUser.json".parseStatusCode().shouldBeNull()
    }

    @Test
    fun `returns null for file name with non-numeric suffix after hyphen`() {
        "getUser-json.json".parseStatusCode().shouldBeNull()
    }

    @Test
    fun `returns null for file name with only two digits`() {
        "getUser-20.json".parseStatusCode().shouldBeNull()
    }

    @Test
    fun `returns null for file name with four digits`() {
        "getUser-2000.json".parseStatusCode().shouldBeNull()
    }

    @Test
    fun `returns null for empty string`() {
        "".parseStatusCode().shouldBeNull()
    }

    // endregion
}
