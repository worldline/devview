package com.worldline.devview.networkmock.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.worldline.devview.networkmock.model.OperationUiModel
import com.worldline.devview.networkmock.preview.OperationUiModelPreviewParameterProvider

@Composable
internal fun EndpointHeaderCard(endpoint: OperationUiModel, modifier: Modifier = Modifier) {
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
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(size = 4.dp)
                            ).padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = endpoint.descriptor.config.method,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontFamily = FontFamily.Monospace
                        )
                    }
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
        OperationUiModelPreviewParameterProvider::class
    ) endpoint: OperationUiModel
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
