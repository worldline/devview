package com.worldline.devview.networkmock

import com.worldline.devview.networkmock.core.model.MockResponse
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class PreviewSheetStateTest {

    private val responseA = MockResponse(
        statusCode = 200,
        mediaType = "application/json",
        exampleName = "default",
        displayName = "Success (200)",
        content = "{}"
    )
    private val responseB = responseA.copy(statusCode = 404, displayName = "Not Found (404)")
    private val responseC = responseA.copy(statusCode = 500, displayName = "Server Error (500)")

    @Test
    fun oversizedFormattedComparisonFallsBackToSplit() {
        val body = (1..2000).joinToString(prefix = "[", postfix = "]") { it.toString() }
        val response = responseA.copy(content = body)
        val comparison = PreviewSheetState.Compare(first = response, second = response)
        comparison.useInlineDiff shouldBe false
        comparison.lineDiff.size shouldBe 0
    }

    @Test
    fun identicalCompactAndFormattedJsonUseInlineDiff() {
        val compact = responseA.copy(content = "{\"ok\":true}")
        val formatted = compact.copy(content = "{\n    \"ok\": true\n}")
        val comparison = PreviewSheetState.Compare(first = compact, second = formatted)
        comparison.useInlineDiff shouldBe true
        comparison.lineDiff.all { it is com.worldline.devview.networkmock.model.DiffLine.Unchanged } shouldBe true
    }

    @Test
    fun singleHoldsTheDirectlyOpenedResponse() {
        PreviewSheetState.Single(response = responseA).response shouldBe responseA
    }

    @Test
    fun changingComparisonKeepsThePrimaryResponse() {
        val initial = PreviewSheetState.Compare(first = responseA, second = responseB)
        val changed = initial.copy(second = responseC)

        changed.first shouldBe responseA
        changed.second shouldBe responseC
    }
}
