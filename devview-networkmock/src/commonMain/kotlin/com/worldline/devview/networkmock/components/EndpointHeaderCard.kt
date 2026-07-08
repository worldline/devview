package com.worldline.devview.networkmock.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.worldline.devview.networkmock.model.EndpointUiModel
import com.worldline.devview.networkmock.preview.EndpointUiModelPreviewParameterProvider

@Composable
internal fun EndpointHeaderCard(endpoint: EndpointUiModel, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(weight = 1f)
            ) {
                Text(
                    text = endpoint.descriptor.config.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = endpoint.descriptor.config.method,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = endpoint.descriptor.config.path,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            EndpointStateChip(
                endpointMockState = endpoint.currentState
            )
        }
    }
}

@Preview(locale = "en")
@Composable
private fun EndpointHeaderCardPreview(
    @PreviewParameter(
        EndpointUiModelPreviewParameterProvider::class
    ) endpoint: EndpointUiModel
) {
    MaterialTheme {
        Surface {
            EndpointHeaderCard(
                endpoint = endpoint,
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
    }
}
