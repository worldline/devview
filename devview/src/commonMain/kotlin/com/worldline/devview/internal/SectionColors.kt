package com.worldline.devview.internal

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.worldline.devview.core.Section
import com.worldline.devview.core.hue
import kotlin.math.max
import kotlin.math.min

internal fun lerpHue(a: Float, b: Float, t: Float): Float {
    val delta = (b - a + 540f) % 360f - 180f
    return (a + delta * t + 360f) % 360f
}

internal fun sectionTileColors(
    section: Section,
    primary: Color,
    dark: Boolean
): Pair<Color, Color> {
    val red = primary.red
    val green = primary.green
    val blue = primary.blue
    val high = max(a = red, b = max(a = green, b = blue))
    val low = min(a = red, b = min(a = green, b = blue))
    val delta = high - low
    val primaryHue = when {
        delta == 0f -> section.hue
        high == red -> ((green - blue) / delta * 60f + 360f) % 360f
        high == green -> (blue - red) / delta * 60f + 120f
        else -> (red - green) / delta * 60f + 240f
    }
    val hue = lerpHue(a = section.hue, b = primaryHue, t = 0.25f)
    return Color.hsl(hue = hue, saturation = 0.45f, lightness = if (dark) 0.28f else 0.90f) to
        Color.hsl(hue = hue, saturation = 0.45f, lightness = if (dark) 0.88f else 0.20f)
}

internal fun sectionTileContent(container: Color, defaultContent: Color): Color = when {
    container == Color.Unspecified -> defaultContent
    // The black/white contrast crossover is (L + .05)^2 = .0525.
    container.luminance() > 0.179f -> Color.Black
    else -> Color.White
}

@Composable
internal fun sectionTileColors(section: Section): Pair<Color, Color> = sectionTileColors(
    section = section,
    primary = MaterialTheme.colorScheme.primary,
    dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
)
