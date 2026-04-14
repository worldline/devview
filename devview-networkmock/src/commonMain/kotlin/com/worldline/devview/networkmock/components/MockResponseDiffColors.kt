package com.worldline.devview.networkmock.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

/** Light blue-white — background for left-side diff lines. Legible in both light and dark. */
internal val DiffLeftContainer = Color(color = 0xFFD6E8FF)

/** Muted steel blue — text colour on [DiffLeftContainer]. */
internal val DiffOnLeftContainer = Color(color = 0xFF2D4A6E)

/** Light bisque — background for right-side diff lines. Maximally distinct from [DiffLeftContainer]. */
internal val DiffRightContainer = Color(color = 0xFFFFE4C4)

/** Muted amber/brown — text colour on [DiffRightContainer]. */
internal val DiffOnRightContainer = Color(color = 0xFF4A3320)

/**
 * Colours used by [InlineDiffContent], [SplitDiffContent], and [DiffLineRow] to render mock
 * response diffs.
 *
 * Create an instance via [MockResponseDiffDefaults.colors], which provides Material3 defaults
 * for every slot. Individual colours can be overridden for theming or branded customisation.
 *
 * @param surface Background colour for unchanged content rows.
 * @param onSurface Text colour for unchanged content rows.
 * @param gutterContainer Background colour of the line-number gutter columns.
 * @param onGutterContainer Text colour used for line numbers and secondary labels in the gutter.
 * @param collapsedContainer Background colour of the collapsed-lines placeholder row.
 * @param leftContainer Background colour for lines that belong to the left (first) response only.
 * @param onLeftContainer Text colour for lines highlighted with [leftContainer].
 * @param rightContainer Background colour for lines that belong to the right (second) response only.
 * @param onRightContainer Text colour for lines highlighted with [rightContainer].
 */
@Suppress("TooManyFunctions")
@Immutable
internal class MockResponseDiffColors(
    internal val surface: Color,
    internal val onSurface: Color,
    internal val gutterContainer: Color,
    internal val onGutterContainer: Color,
    internal val collapsedContainer: Color,
    internal val leftContainer: Color,
    internal val onLeftContainer: Color,
    internal val rightContainer: Color,
    internal val onRightContainer: Color
) {
    /**
     * Returns a copy of these colours with the given overrides applied.
     * All parameters default to the current value so only the slots you want to change
     * need to be specified.
     */
    fun copy(
        surface: Color = this.surface,
        onSurface: Color = this.onSurface,
        gutterContainer: Color = this.gutterContainer,
        onGutterContainer: Color = this.onGutterContainer,
        collapsedContainer: Color = this.collapsedContainer,
        leftContainer: Color = this.leftContainer,
        onLeftContainer: Color = this.onLeftContainer,
        rightContainer: Color = this.rightContainer,
        onRightContainer: Color = this.onRightContainer
    ): MockResponseDiffColors = MockResponseDiffColors(
        surface = surface,
        onSurface = onSurface,
        gutterContainer = gutterContainer,
        onGutterContainer = onGutterContainer,
        collapsedContainer = collapsedContainer,
        leftContainer = leftContainer,
        onLeftContainer = onLeftContainer,
        rightContainer = rightContainer,
        onRightContainer = onRightContainer
    )

    @Stable
    internal fun surface(): Color = surface

    @Stable
    internal fun onSurface(): Color = onSurface

    @Stable
    internal fun gutterContainer(): Color = gutterContainer

    @Stable
    internal fun onGutterContainer(): Color = onGutterContainer

    @Stable
    internal fun collapsedContainer(): Color = collapsedContainer

    @Stable
    internal fun leftContainer(): Color = leftContainer

    @Stable
    internal fun onLeftContainer(): Color = onLeftContainer

    @Stable
    internal fun rightContainer(): Color = rightContainer

    @Stable
    internal fun onRightContainer(): Color = onRightContainer

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MockResponseDiffColors) return false
        if (surface != other.surface) return false
        if (onSurface != other.onSurface) return false
        if (gutterContainer != other.gutterContainer) return false
        if (onGutterContainer != other.onGutterContainer) return false
        if (collapsedContainer != other.collapsedContainer) return false
        if (leftContainer != other.leftContainer) return false
        if (onLeftContainer != other.onLeftContainer) return false
        if (rightContainer != other.rightContainer) return false
        if (onRightContainer != other.onRightContainer) return false
        return true
    }

    override fun hashCode(): Int {
        var result = surface.hashCode()
        result = 31 * result + onSurface.hashCode()
        result = 31 * result + gutterContainer.hashCode()
        result = 31 * result + onGutterContainer.hashCode()
        result = 31 * result + collapsedContainer.hashCode()
        result = 31 * result + leftContainer.hashCode()
        result = 31 * result + onLeftContainer.hashCode()
        result = 31 * result + rightContainer.hashCode()
        result = 31 * result + onRightContainer.hashCode()
        return result
    }
}

/**
 * Default values for [MockResponseDiffColors].
 *
 * Use [colors] to obtain a fully populated instance backed by the current Material3 theme.
 */
internal object MockResponseDiffDefaults {
    /**
     * Creates a [MockResponseDiffColors] populated with Material3 colour-scheme defaults.
     * Override any slot to customise the appearance of the diff components.
     *
     * - [leftContainer] / [onLeftContainer] and [rightContainer] / [onRightContainer] are
     *   hardcoded to muted blue and amber/brown respectively. These are theme-agnostic values
     *   chosen to be clearly distinct from each other and legible in both light and dark modes,
     *   without implying additions or removals.
     */
    @Composable
    fun colors(
        surface: Color = MaterialTheme.colorScheme.surface,
        onSurface: Color = MaterialTheme.colorScheme.onSurface,
        gutterContainer: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
        onGutterContainer: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        collapsedContainer: Color = MaterialTheme.colorScheme.surfaceContainerLow,
        leftContainer: Color = DiffLeftContainer,
        onLeftContainer: Color = DiffOnLeftContainer,
        rightContainer: Color = DiffRightContainer,
        onRightContainer: Color = DiffOnRightContainer
    ): MockResponseDiffColors = MockResponseDiffColors(
        surface = surface,
        onSurface = onSurface,
        gutterContainer = gutterContainer,
        onGutterContainer = onGutterContainer,
        collapsedContainer = collapsedContainer,
        leftContainer = leftContainer,
        onLeftContainer = onLeftContainer,
        rightContainer = rightContainer,
        onRightContainer = onRightContainer
    )
}
