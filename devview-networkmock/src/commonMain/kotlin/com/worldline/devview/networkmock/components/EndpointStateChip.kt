package com.worldline.devview.networkmock.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.worldline.devview.networkmock.core.model.OperationMockState
import com.worldline.devview.networkmock.model.OperationUiModel
import com.worldline.devview.networkmock.preview.OperationUiModelPreviewParameterProvider
import com.worldline.devview.networkmock.utils.containerColor
import com.worldline.devview.networkmock.utils.contentColor
import com.worldline.devview.networkmock.utils.icon

@Composable
internal fun EndpointStateChip(
    endpointMockState: OperationMockState,
    modifier: Modifier = Modifier,
    label: String = endpointMockState.displayName,
    chipTestTag: String = "endpoint_state_chip",
    labelTestTag: String = "endpoint_state_chip_label"
) {
    Row(
        modifier = modifier
            .testTag(tag = chipTestTag)
            .clip(
                shape = MaterialTheme.shapes.small
            ).background(
                color = endpointMockState.containerColor
            ).padding(
                horizontal = 6.dp,
                vertical = 4.dp
            ),
        horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier
                .size(size = 16.dp),
            imageVector = endpointMockState.icon,
            contentDescription = null,
            tint = endpointMockState.contentColor
        )
        Text(
            modifier = Modifier
                .testTag(tag = "${labelTestTag}_$label")
                .widthIn(max = 160.dp),
            text = label,
            style = MaterialTheme.typography.bodySmallEmphasized,
            color = endpointMockState.contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(locale = "en")
@Composable
private fun EndpointStateChipPreview(
    @PreviewParameter(
        OperationUiModelPreviewParameterProvider::class
    ) endpoint: OperationUiModel
) {
    MaterialTheme {
        Surface {
            EndpointStateChip(
                endpointMockState = endpoint.currentState
            )
        }
    }
}
