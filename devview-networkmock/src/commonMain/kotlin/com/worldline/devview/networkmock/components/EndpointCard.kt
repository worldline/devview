package com.worldline.devview.networkmock.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.worldline.devview.networkmock.model.EndpointUiModel
import com.worldline.devview.networkmock.preview.EndpointUiModelPreviewParameterProvider

/**
 * Card component for displaying and configuring a single API endpoint mock.
 *
 * Shows the endpoint details, a toggle for enabling/disabling the mock,
 * and a dropdown to select which mock response to return.
 *
 * @param endpoint The endpoint UI model pairing static config with live state
 * @param openEndpointDetails Callback invoked when the card is tapped
 * @param modifier Optional modifier
 */
@Composable
internal fun EndpointCard(
    endpoint: EndpointUiModel,
    openEndpointDetails: () -> Unit,
    modifier: Modifier = Modifier,
    showFileName: Boolean = false
) {
    val spacing by animateIntAsState(
        targetValue = if (showFileName) 2 else 0
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                enabled = true,
                onClick = openEndpointDetails
            ).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(weight = 1f),
            verticalArrangement = Arrangement.spacedBy(space = spacing.dp)
        ) {
            Text(
                modifier = Modifier.testTag(
                    tag = "endpoint_name_${endpoint.descriptor.endpointId}"
                ),
                text = endpoint.descriptor.config.name,
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.testTag(
                        tag = "endpoint_method_${endpoint.descriptor.endpointId}"
                    ),
                    text = endpoint.descriptor.config.method,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    modifier = Modifier.testTag(
                        tag = "endpoint_path_${endpoint.descriptor.endpointId}"
                    ),
                    text = endpoint.descriptor.config.path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }
            AnimatedVisibility(
                visible = showFileName
            ) {
                Text(
                    modifier = Modifier.testTag(
                        tag = "endpoint_state_${endpoint.descriptor.endpointId}"
                    ),
                    text = endpoint.currentState.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        EndpointStateChip(
            endpointMockState = endpoint.currentState,
            chipTestTag = "endpoint_state_chip_${endpoint.descriptor.endpointId}",
            labelTestTag = "endpoint_state_chip_label_${endpoint.descriptor.endpointId}"
        )
    }
}

@Preview(locale = "en")
@Composable
private fun EndpointCardPreview(
    @PreviewParameter(
        EndpointUiModelPreviewParameterProvider::class
    ) endpoint: EndpointUiModel
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

@Preview(locale = "en")
@Composable
private fun EndpointCardWithFileNamePreview(
    @PreviewParameter(
        EndpointUiModelPreviewParameterProvider::class
    ) endpoint: EndpointUiModel
) {
    MaterialTheme {
        Surface {
            EndpointCard(
                endpoint = endpoint,
                openEndpointDetails = {},
                showFileName = true
            )
        }
    }
}
