package com.worldline.devview.networkmock.components

import androidx.compose.ui.graphics.luminance
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import kotlin.test.Test

class MockResponseDiffColorsTest {

    @Test
    fun `diff text contrast passes in both themes`() {
        val pairs = listOf(
            DiffLeftContainer to DiffOnLeftContainer,
            DiffRightContainer to DiffOnRightContainer,
            DiffDarkLeftContainer to DiffDarkOnLeftContainer,
            DiffDarkRightContainer to DiffDarkOnRightContainer
        )

        pairs.forEach { (container, content) ->
            val lighter = maxOf(container.luminance(), content.luminance()).toDouble()
            val darker = minOf(container.luminance(), content.luminance()).toDouble()
            (lighter + 0.05) / (darker + 0.05) shouldBeGreaterThanOrEqual 4.5
        }
    }
}
