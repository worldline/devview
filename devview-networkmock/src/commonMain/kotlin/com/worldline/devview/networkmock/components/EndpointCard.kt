package com.worldline.devview.networkmock.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.worldline.devview.networkmock.core.model.OperationMockState
import com.worldline.devview.networkmock.model.OperationUiModel
import com.worldline.devview.networkmock.preview.OperationUiModelPreviewParameterProvider
import com.worldline.devview.networkmock.utils.badgeContainerColor
import com.worldline.devview.networkmock.utils.badgeContentColor
import com.worldline.devview.networkmock.utils.containerColor

/**
 * Row component for displaying and configuring a single API operation mock.
 *
 * Shows the operation name, method and path, a leading colour rail that reflects the current
 * mock state (matching the state chip's colour, transparent for [OperationMockState.Network]),
 * and the state chip itself. The path is never truncated — it wraps instead, since a cut-off
 * URL segment can hide the difference between two otherwise-similar operations.
 *
 * @param endpoint The operation UI model pairing static config with live state
 * @param openEndpointDetails Callback invoked when the card is tapped
 * @param modifier Optional modifier
 */
@Composable
internal fun EndpointCard(
    endpoint: OperationUiModel,
    openEndpointDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val railColor = when (val state = endpoint.currentState) {
        is OperationMockState.Mock -> state.containerColor
        OperationMockState.Network -> Color.Transparent
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(intrinsicSize = IntrinsicSize.Min)
            .clickable(
                enabled = true,
                onClick = openEndpointDetails
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(width = 3.dp)
                .fillMaxHeight()
                .background(color = railColor)
        )
        Row(
            modifier = Modifier
                .weight(weight = 1f)
                .padding(start = 13.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(weight = 1f)
            ) {
                Text(
                    modifier = Modifier.testTag(
                        tag = "endpoint_name_${endpoint.descriptor.operationId}"
                    ),
                    text = endpoint.descriptor.config.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = endpoint.descriptor.config.method.badgeContainerColor,
                                shape = RoundedCornerShape(size = 4.dp)
                            ).padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            modifier = Modifier.testTag(
                                tag = "endpoint_method_${endpoint.descriptor.operationId}"
                            ),
                            text = endpoint.descriptor.config.method.value,
                            style = MaterialTheme.typography.labelSmall,
                            color = endpoint.descriptor.config.method.badgeContentColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        modifier = Modifier.testTag(
                            tag = "endpoint_path_${endpoint.descriptor.operationId}"
                        ),
                        text = endpoint.descriptor.config.path,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            EndpointStateChip(
                endpointMockState = endpoint.currentState,
                chipTestTag = "endpoint_state_chip_${endpoint.descriptor.operationId}",
                labelTestTag = "endpoint_state_chip_label_${endpoint.descriptor.operationId}"
            )
        }
    }
}

@Preview(locale = "en")
@Composable
private fun EndpointCardPreview(
    @PreviewParameter(
        OperationUiModelPreviewParameterProvider::class
    ) endpoint: OperationUiModel
) {
    MaterialTheme {
        Surface {
            EndpointCard(
                endpoint = endpoint,
                openEndpointDetails = {}
            )
        }
    }
}
