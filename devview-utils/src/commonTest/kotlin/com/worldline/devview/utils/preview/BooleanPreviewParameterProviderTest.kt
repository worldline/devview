package com.worldline.devview.utils.preview

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class BooleanPreviewParameterProviderTest {

    @Test
    fun `values exposes true then false`() {
        val provider = BooleanPreviewParameterProvider()

        provider.values.toList().shouldContainExactly(true, false)
    }

    @Test
    fun `display names map to values and out of range returns null`() {
        val provider = BooleanPreviewParameterProvider()

        provider.getDisplayName(0) shouldBe "True"
        provider.getDisplayName(1) shouldBe "False"
        provider.getDisplayName(2) shouldBe null
    }
}

