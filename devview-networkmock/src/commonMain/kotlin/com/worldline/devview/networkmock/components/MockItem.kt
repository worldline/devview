package com.worldline.devview.networkmock.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.worldline.devview.networkmock.core.model.EndpointMockState
import com.worldline.devview.networkmock.core.model.MockResponse
import com.worldline.devview.networkmock.preview.MockResponsePreviewParameterProvider
import com.worldline.devview.networkmock.utils.containerColor
import com.worldline.devview.networkmock.utils.containerColorForStatusCode
import com.worldline.devview.networkmock.utils.contentColor
import com.worldline.devview.networkmock.utils.contentColorForStatusCode
import com.worldline.devview.networkmock.utils.fake
import com.worldline.devview.networkmock.utils.icon
import com.worldline.devview.networkmock.utils.iconForStatusCode
import com.worldline.devview.utils.preview.BooleanPreviewParameterProvider
import kotlin.math.abs

@Composable
internal fun MockItem(
    mockResponse: MockResponse,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isInPreviewMode: Boolean,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    MockItemContent(
        modifier = modifier,
        statusCode = mockResponse.statusCode,
        label = mockResponse.fileName,
        selected = selected,
        onClick = onClick,
        onLongClick = onLongClick,
        isInPreviewMode = isInPreviewMode
    )
}

@Composable
internal fun NetworkItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    MockItemContent(
        modifier = modifier,
        statusCode = null,
        label = EndpointMockState.Network.displayName,
        selected = selected,
        isNetwork = true,
        onClick = onClick,
        onLongClick = null,
        isInPreviewMode = false
    )
}

@Composable
private fun MockItemContent(
    statusCode: Int?,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    isInPreviewMode: Boolean,
    modifier: Modifier = Modifier,
    isNetwork: Boolean = false
) {
    val (icon, contentColor, containerColor) = when (isNetwork) {
        true -> {
            val state = EndpointMockState.Network
            Triple(
                first = state.icon,
                second = state.contentColor,
                third = state.containerColor
            )
        }

        false -> {
            requireNotNull(value = statusCode) {
                "Status code must not be null for non-network items"
            }
            Triple(
                first = iconForStatusCode(statusCode = statusCode),
                second = contentColorForStatusCode(statusCode = statusCode),
                third = containerColorForStatusCode(statusCode = statusCode)
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                enabled = true,
                onClick = {
                    if (!selected) {
                        onClick()
                    }
                },
                onLongClick = onLongClick
            ).then(
                other = modifier
                    .minimumInteractiveComponentSize()
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 16.dp)
    ) {
        // Vertical-axis flip: animate from -1f (status icon side) to 1f (checkbox side).
        // abs(flipProgress) drives scaleX so the icon collapses to 0 at the midpoint then
        // expands back; the sign determines which icon is visible.
        val flipTransition = updateTransition(
            targetState = isInPreviewMode,
            label = "leadingIconFlip"
        )

        val flipProgress by flipTransition.animateFloat(
            transitionSpec = { tween(durationMillis = 300) },
            label = "flipProgress"
        ) { inPreview -> if (inPreview) 1f else -1f }

        val displayedIcon by remember(key1 = icon) {
            derivedStateOf { if (flipProgress >= 0f) Icons.Rounded.CheckBox else icon }
        }

        Icon(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .clip(shape = MaterialTheme.shapes.small)
                .background(color = containerColor)
                .padding(all = 4.dp)
                .graphicsLayer { scaleX = abs(x = flipProgress) },
            imageVector = displayedIcon,
            contentDescription = null,
            tint = contentColor
        )
        Text(
            modifier = Modifier
                .weight(weight = 1f),
            text = label,
            style = MaterialTheme.typography.bodyLargeEmphasized
        )
        AnimatedVisibility(
            visible = selected
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null
            )
        }
    }
}

@Preview(locale = "en")
@Composable
private fun MockItemPreview(
    @PreviewParameter(MockResponsePreviewParameterProvider::class) mockResponse: MockResponse
) {
    MaterialTheme {
        Surface {
            MockItem(
                mockResponse = mockResponse,
                onClick = {},
                onLongClick = {},
                isInPreviewMode = false
            )
        }
    }
}

@Preview(locale = "en")
@Composable
private fun NetworkItemPreview(
    @PreviewParameter(BooleanPreviewParameterProvider::class) selected: Boolean
) {
    MaterialTheme {
        Surface {
            NetworkItem(
                selected = selected,
                onClick = {}
            )
        }
    }
}

@Preview(locale = "en")
@Composable
private fun MockItemSelectedPreview(
    @PreviewParameter(BooleanPreviewParameterProvider::class) selected: Boolean
) {
    MaterialTheme {
        Surface {
            MockItem(
                mockResponse = MockResponse.fake().first(),
                selected = selected,
                onClick = {},
                onLongClick = {},
                isInPreviewMode = false
            )
        }
    }
}

@Preview(locale = "en")
@Composable
internal fun MockItemPreviewModePreview(
    @PreviewParameter(BooleanPreviewParameterProvider::class) isInPreviewMode: Boolean
) {
    MaterialTheme {
        Surface {
            MockItem(
                mockResponse = MockResponse.fake().first(),
                selected = false,
                onClick = {},
                onLongClick = {},
                isInPreviewMode = isInPreviewMode
            )
        }
    }
}
