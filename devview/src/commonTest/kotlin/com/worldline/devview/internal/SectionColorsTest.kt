package com.worldline.devview.internal

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.worldline.devview.core.Section
import com.worldline.devview.core.displayName
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertTrue

class SectionColorsTest {

    @Test
    fun hue_blend_takes_shortest_arc() {
        lerpHue(a = 350f, b = 10f, t = 0.5f) shouldBe 0f
        lerpHue(a = 10f, b = 350f, t = 0.5f) shouldBe 0f
    }

    @Test
    fun every_section_passes_icon_contrast_with_different_host_hues() {
        for (primary in listOf(Color.Gray, Color.Red, Color.Green, Color.Blue, Color.Yellow)) {
            for (section in Section.entries) {
                for (dark in listOf(false, true)) {
                    val (container, content) = sectionTileColors(section = section, primary = primary, dark = dark)
                    val contrast = (maxOf(container.luminance(), content.luminance()) + 0.05f) /
                        (minOf(container.luminance(), content.luminance()) + 0.05f)
                    assertTrue(actual = contrast >= 4.5f, message = "$section dark=$dark contrast=$contrast")
                }
            }
        }
    }

    @Test
    fun container_only_override_receives_a_contrasting_icon() {
        sectionTileContent(container = Color.Black, defaultContent = Color.Red) shouldBe Color.White
        sectionTileContent(container = Color.White, defaultContent = Color.Yellow) shouldBe Color.Black
        sectionTileContent(container = Color.Unspecified, defaultContent = Color.Red) shouldBe Color.Red
        for (container in listOf(Color.Gray, Color.Red, Color.Green, Color.Blue, Color.Yellow)) {
            val content = sectionTileContent(container = container, defaultContent = Color.Blue)
            val contrast = (maxOf(container.luminance(), content.luminance()) + 0.05f) /
                (minOf(container.luminance(), content.luminance()) + 0.05f)
            assertTrue(actual = contrast >= 4.5f, message = "Explicit container contrast=$contrast")
        }
    }

    @Test
    fun sections_have_human_readable_names() {
        Section.entries.map { it.displayName } shouldBe listOf("Settings", "Features", "Network", "Logging", "Custom")
    }
}
