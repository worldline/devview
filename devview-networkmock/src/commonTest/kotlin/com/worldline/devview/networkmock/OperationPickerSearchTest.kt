package com.worldline.devview.networkmock

import com.worldline.devview.networkmock.core.model.MockResponse
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class OperationPickerSearchTest {
    private val success = MockResponse(200, "application/json", "default", "Successful user", "{}")
    private val missing = MockResponse(404, "application/xml", "missing", "User not found", "<error/>")
    private val responses = listOf(success, missing)

    @Test
    fun searchesStatusNameExampleAndMediaTypeWithoutChangingOrderOrIdentity() {
        filterPickerResponses(responses, "200") shouldBe listOf(success)
        filterPickerResponses(responses, " NOT FOUND ") shouldBe listOf(missing)
        filterPickerResponses(responses, "MISSING") shouldBe listOf(missing)
        filterPickerResponses(responses, "application/XML") shouldBe listOf(missing)
        filterPickerResponses(responses, "user") shouldBe responses
    }

    @Test
    fun blankSearchReturnsAllAndUnmatchedOrEmptyCataloguesReturnNone() {
        filterPickerResponses(responses, "  ") shouldBe responses
        filterPickerResponses(responses, "503") shouldBe emptyList()
        filterPickerResponses(emptyList(), "200") shouldBe emptyList()
    }
}
