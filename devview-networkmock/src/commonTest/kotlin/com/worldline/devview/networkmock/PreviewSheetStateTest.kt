package com.worldline.devview.networkmock

import com.worldline.devview.networkmock.core.model.MockResponse
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

class PreviewSheetStateTest {

    private val responseA = MockResponse(
        statusCode = 200,
        fileName = "a-200.json",
        displayName = "Success (200)",
        content = """{"id":1}"""
    )
    private val responseB = MockResponse(
        statusCode = 404,
        fileName = "b-404.json",
        displayName = "Not Found (404)",
        content = """{"error":"not found"}"""
    )
    private val responseC = MockResponse(
        statusCode = 500,
        fileName = "c-500.json",
        displayName = "Server Error (500)",
        content = """{"error":"server error"}"""
    )

    // region transition

    @Test
    fun `transition from Hidden adds response as Single`() {
        val result = PreviewSheetState.Hidden.transition(response = responseA)

        result.shouldBeInstanceOf<PreviewSheetState.Single>().response shouldBe responseA
    }

    @Test
    fun `transition from Single with same response returns Hidden`() {
        val result = PreviewSheetState.Single(response = responseA).transition(response = responseA)

        result shouldBe PreviewSheetState.Hidden
    }

    @Test
    fun `transition from Single with different response becomes Compare`() {
        val result = PreviewSheetState.Single(response = responseA).transition(response = responseB)

        val compare = result.shouldBeInstanceOf<PreviewSheetState.Compare>()
        compare.first shouldBe responseA
        compare.second shouldBe responseB
    }

    @Test
    fun `transition from Compare deselecting first returns Single with second`() {
        val initial = PreviewSheetState.Compare(first = responseA, second = responseB)

        val result = initial.transition(response = responseA)

        result.shouldBeInstanceOf<PreviewSheetState.Single>().response shouldBe responseB
    }

    @Test
    fun `transition from Compare deselecting second returns Single with first`() {
        val initial = PreviewSheetState.Compare(first = responseA, second = responseB)

        val result = initial.transition(response = responseB)

        result.shouldBeInstanceOf<PreviewSheetState.Single>().response shouldBe responseA
    }

    @Test
    fun `transition from Compare with unrelated response is no-op`() {
        val initial = PreviewSheetState.Compare(first = responseA, second = responseB)

        val result = initial.transition(response = responseC)

        result shouldBe initial
    }

    // endregion

    // region isInPreviewMode

    @Test
    fun `isInPreviewMode returns false for Hidden`() {
        PreviewSheetState.Hidden.isInPreviewMode(response = responseA) shouldBe false
    }

    @Test
    fun `isInPreviewMode returns true for matching Single`() {
        PreviewSheetState.Single(response = responseA).isInPreviewMode(response = responseA) shouldBe true
    }

    @Test
    fun `isInPreviewMode returns false for non-matching Single`() {
        PreviewSheetState.Single(response = responseA).isInPreviewMode(response = responseB) shouldBe false
    }

    @Test
    fun `isInPreviewMode returns true for first response in Compare`() {
        PreviewSheetState.Compare(first = responseA, second = responseB)
            .isInPreviewMode(response = responseA) shouldBe true
    }

    @Test
    fun `isInPreviewMode returns true for second response in Compare`() {
        PreviewSheetState.Compare(first = responseA, second = responseB)
            .isInPreviewMode(response = responseB) shouldBe true
    }

    @Test
    fun `isInPreviewMode returns false for unrelated response in Compare`() {
        PreviewSheetState.Compare(first = responseA, second = responseB)
            .isInPreviewMode(response = responseC) shouldBe false
    }

    // endregion
}
