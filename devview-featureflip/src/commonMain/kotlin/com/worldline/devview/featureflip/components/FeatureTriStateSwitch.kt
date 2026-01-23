@file:Suppress("TooManyFunctions", "CommentOverPrivateFunction")

package com.worldline.devview.featureflip.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.LocalTonalElevationEnabled
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRowScope
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.MultiContentMeasurePolicy
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxBy
import com.worldline.devview.featureflip.model.Feature.RemoteFeature
import com.worldline.devview.featureflip.model.FeatureState
import com.worldline.devview.featureflip.preview.RemoteFeaturePreviewParameterProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * A tri-state segmented button switch for controlling remote feature flags.
 *
 * Displays three options:
 * - Remote (cloud icon): Use the default remote configuration value
 * - Off (cancel icon): Force the feature off locally
 * - On (check icon): Force the feature on locally
 *
 * The active button's color changes based on the feature's effective state.
 *
 * @param feature The remote feature to control
 * @param onStateChange Callback invoked when the state changes
 * @param modifier Modifier to apply to the switch
 * @param featureOnColor Color used when a feature is effectively enabled
 * @param featureOffColor Color used when a feature is effectively disabled
 */
@Composable
internal fun FeatureTriStateSwitch(
    feature: RemoteFeature,
    onStateChange: (FeatureState) -> Unit,
    modifier: Modifier = Modifier,
    featureOnColor: Color = MaterialTheme.colorScheme.primary,
    featureOffColor: Color = MaterialTheme.colorScheme.error
) {
    val selectedIndex = feature.state.ordinal
    FeatureSingleChoiceSegmentedButtonRow(
        modifier = modifier
    ) {
        FeatureState.entries.forEachIndexed { index, featureState ->
            val selected = index == selectedIndex
            FeatureSegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = FeatureState.entries.size,
                    baseShape = MaterialTheme.shapes.small
                ),
                onClick = {
                    onStateChange(featureState)
                },
                selected = selected,
                icon = {
                    Icon(
                        imageVector = when (featureState) {
                            FeatureState.REMOTE -> Icons.Outlined.Cloud
                            FeatureState.LOCAL_ON -> Icons.Outlined.CheckCircleOutline
                            FeatureState.LOCAL_OFF -> Icons.Outlined.Cancel
                        },
                        contentDescription = featureState.name
                    )
                },
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = when (featureState) {
                        FeatureState.REMOTE -> when (feature.defaultRemoteValue) {
                            true -> featureOnColor
                            false -> featureOffColor
                        }

                        FeatureState.LOCAL_OFF -> featureOffColor
                        FeatureState.LOCAL_ON -> featureOnColor
                    }
                )
            )
        }
    }
}

/**
 * A custom single-choice segmented button row for feature state selection.
 *
 * @param modifier Modifier to apply to the row
 * @param space Spacing between buttons (defaults to border width for overlap effect)
 * @param content The button content to display
 */
@Composable
private fun FeatureSingleChoiceSegmentedButtonRow(
    modifier: Modifier = Modifier,
    space: Dp = SegmentedButtonDefaults.BorderWidth,
    content: @Composable SingleChoiceSegmentedButtonRowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .defaultMinSize(
                minHeight = LocalMinimumInteractiveComponentSize.current
            ).selectableGroup()
            .width(intrinsicSize = IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(space = -space),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val scope = remember {
            SingleChoiceSegmentedButtonScopeWrapper(
                scope = this
            )
        }
        scope.content()
    }
}

/**
 * A single button within the feature segmented button row.
 *
 * @param selected Whether this button is currently selected
 * @param onClick Callback when the button is clicked
 * @param shape The shape to apply to this button (varies by position)
 * @param icon The icon content to display
 * @param modifier Modifier to apply to the button
 * @param enabled Whether the button is enabled for interaction
 * @param colors Color scheme for the button states
 * @param border Border stroke for the button
 * @param interactionSource Optional interaction source for tracking user interactions
 * @param padding Padding applied to the button content
 */
@Composable
private fun SingleChoiceSegmentedButtonRowScope.FeatureSegmentedButton(
    selected: Boolean,
    onClick: () -> Unit,
    shape: Shape,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SegmentedButtonColors = SegmentedButtonDefaults.colors(),
    border: BorderStroke =
        SegmentedButtonDefaults.borderStroke(
            color = colors.borderColor(
                enabled = enabled,
                active = selected
            )
        ),
    interactionSource: MutableInteractionSource? = null,
    padding: PaddingValues = PaddingValues(all = 8.dp)
) {
    val mutableInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val containerColor = colors.containerColor(enabled = enabled, active = selected)
    val contentColor = colors.contentColor(enabled = enabled, checked = selected)
    val interactionCount = mutableInteractionSource.interactionCountAsState()

    val absoluteElevation = LocalAbsoluteTonalElevation.current
    CompositionLocalProvider(
        LocalContentColor provides contentColor,
        LocalAbsoluteTonalElevation provides absoluteElevation
    ) {
        Box(
            modifier = modifier
                .weight(weight = 1f)
                .interactionZIndex(checked = selected, interactionCount = interactionCount)
                .semantics { role = Role.RadioButton }
                .surface(
                    shape = shape,
                    backgroundColor =
                    surfaceColorAtElevation(
                        color = containerColor,
                        elevation = absoluteElevation
                    ),
                    border = border,
                    shadowElevation = 0f
                ).selectable(
                    selected = selected,
                    interactionSource = mutableInteractionSource,
                    indication = ripple(),
                    enabled = enabled,
                    onClick = onClick
                ),
            propagateMinConstraints = true
        ) {
            FeatureSegmentedButtonContent(icon = icon, padding = padding)
        }
    }
}

/**
 * Content layout for a feature segmented button.
 *
 * @param icon The icon to display in the button
 * @param padding Padding to apply around the content
 */
@Composable
private fun FeatureSegmentedButtonContent(icon: @Composable () -> Unit, padding: PaddingValues) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.padding(paddingValues = padding)
    ) {
        val scope = rememberCoroutineScope()
        val measurePolicy = remember {
            SegmentedButtonContentMeasurePolicy(scope = scope)
        }

        Layout(
            modifier = Modifier.height(intrinsicSize = IntrinsicSize.Min),
            contents = listOf(element = icon),
            measurePolicy = measurePolicy
        )
    }
}

// region SegmentedButton overrides

/**
 * Gets the appropriate border color based on enabled and active state.
 */
private fun SegmentedButtonColors.borderColor(enabled: Boolean, active: Boolean): Color = when {
    enabled && active -> activeBorderColor
    enabled && !active -> inactiveBorderColor
    !enabled && active -> disabledActiveBorderColor
    else -> disabledInactiveBorderColor
}

/**
 * Gets the appropriate container color based on enabled and active state.
 */
private fun SegmentedButtonColors.containerColor(enabled: Boolean, active: Boolean): Color = when {
    enabled && active -> activeContainerColor
    enabled && !active -> inactiveContainerColor
    !enabled && active -> disabledActiveContainerColor
    else -> disabledInactiveContainerColor
}

/**
 * Gets the appropriate content color based on enabled and checked state.
 */
private fun SegmentedButtonColors.contentColor(enabled: Boolean, checked: Boolean): Color = when {
    enabled && checked -> activeContentColor
    enabled && !checked -> inactiveContentColor
    !enabled && checked -> disabledActiveContentColor
    else -> disabledInactiveContentColor
}

/**
 * Custom measure policy for animating segmented button content transitions.
 *
 * @property scope Coroutine scope for launching animations
 */
internal class SegmentedButtonContentMeasurePolicy(private val scope: CoroutineScope) :
    MultiContentMeasurePolicy {
    private var animatable: Animatable<Int, AnimationVector1D>? = null
    private var initialOffset: Int? = null

    override fun MeasureScope.measure(
        measurables: List<List<Measurable>>,
        constraints: Constraints
    ): MeasureResult {
        val (contentMeasurables) = measurables
        val contentPlaceables = contentMeasurables.fastMap { it.measure(constraints = constraints) }
        val width = contentPlaceables.fastMaxBy { it.width }?.width ?: 0
        val height = contentPlaceables.fastMaxBy { it.height }?.height ?: 0
        val offsetX = 0

        if (initialOffset == null) {
            initialOffset = offsetX
        } else {
            val anim =
                animatable
                    ?: Animatable(
                        initialValue = initialOffset ?: offsetX,
                        typeConverter = Int.VectorConverter
                    ).also { animatable = it }
            if (anim.targetValue != offsetX) {
                scope.launch {
                    anim.animateTo(
                        targetValue = offsetX,
                        animationSpec = tween(durationMillis = 350)
                    )
                }
            }
        }

        return layout(width = width, height = height) {
            contentPlaceables.fastForEach { it.place(x = offsetX, y = (height - it.height) / 2) }
        }
    }
}

/**
 * Tracks the number of active interactions (press, focus) as a State.
 */
@Composable
private fun InteractionSource.interactionCountAsState(): State<Int> {
    val interactionCount = remember { mutableIntStateOf(value = 0) }
    LaunchedEffect(key1 = this) {
        this@interactionCountAsState.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press,
                is FocusInteraction.Focus -> {
                    interactionCount.intValue++
                }

                is PressInteraction.Release,
                is FocusInteraction.Unfocus,
                is PressInteraction.Cancel -> {
                    interactionCount.intValue--
                }
            }
        }
    }

    return interactionCount
}

/**
 * Applies z-index based on checked state and interaction count.
 * Ensures the selected button and interacted buttons appear above others.
 */
private fun Modifier.interactionZIndex(checked: Boolean, interactionCount: State<Int>) =
    this.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints = constraints)
        layout(width = placeable.width, height = placeable.height) {
            val zIndex = interactionCount.value + if (checked) CHECKED_Z_INDEX_FACTOR else 0f
            placeable.place(x = 0, y = 0, zIndex = zIndex)
        }
    }

private const val CHECKED_Z_INDEX_FACTOR = 5f

private class SingleChoiceSegmentedButtonScopeWrapper(scope: RowScope) :
    SingleChoiceSegmentedButtonRowScope,
    RowScope by scope

// endregion

// region Surface overrides

/**
 * Applies surface styling including shape, background color, border, and shadow elevation.
 */
@Stable
private fun Modifier.surface(
    shape: Shape,
    backgroundColor: Color,
    border: BorderStroke?,
    shadowElevation: Float
) = this
    .then(
        other = if (shadowElevation > 0f) {
            Modifier.graphicsLayer(
                shadowElevation = shadowElevation,
                shape = shape,
                clip = false
            )
        } else {
            Modifier
        }
    ).then(
        other = if (border != null) Modifier.border(border = border, shape = shape) else Modifier
    ).background(color = backgroundColor, shape = shape)
    .clip(shape = shape)

/**
 * Calculates the surface color at a given elevation, applying tonal elevation if appropriate.
 */
@Composable
private fun surfaceColorAtElevation(color: Color, elevation: Dp): Color =
    MaterialTheme.colorScheme.applyTonalElevation(backgroundColor = color, elevation = elevation)

/**
 * Applies tonal elevation to a background color if enabled and the color is a surface color.
 */
@Composable
@ReadOnlyComposable
internal fun ColorScheme.applyTonalElevation(backgroundColor: Color, elevation: Dp): Color {
    val tonalElevationEnabled = LocalTonalElevationEnabled.current
    return if (backgroundColor == surface && tonalElevationEnabled) {
        surfaceColorAtElevation(elevation = elevation)
    } else {
        backgroundColor
    }
}

// endregion

@Preview
@Composable
private fun FeatureTriStateSwitchPreview(
    @PreviewParameter(RemoteFeaturePreviewParameterProvider::class) feature: RemoteFeature
) {
    MaterialTheme {
        Surface {
            FeatureTriStateSwitch(
                feature = feature,
                onStateChange = { }
            )
        }
    }
}
