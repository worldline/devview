package com.worldline.devview.networkmock.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.worldline.devview.networkmock.core.model.EndpointMockState
import com.worldline.devview.networkmock.model.EndpointUiModel
import com.worldline.devview.networkmock.preview.EndpointUiModelPreviewParameterProvider
import com.worldline.devview.networkmock.utils.containerColor
import com.worldline.devview.networkmock.utils.contentColor
import com.worldline.devview.networkmock.utils.icon

@Composable
public fun EndpointStateChip(
    endpointMockState: EndpointMockState,
    modifier: Modifier = Modifier,
    label: String = when (endpointMockState) {
        is EndpointMockState.Mock -> endpointMockState.statusCode.toString()
        EndpointMockState.Network -> endpointMockState.displayName
    },
    chipTestTag: String = "endpoint_state_chip",
    labelTestTag: String = "endpoint_state_chip_label"
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
            modifier = Modifier.testTag(tag = "${labelTestTag}_$displayedLabel"),
            text = label,
            style = MaterialTheme.typography.bodySmallEmphasized,
            color = endpointMockState.contentColor
        )
    }
}

@Preview(locale = "en")
@Composable
private fun EndpointStateChipPreview(
    @PreviewParameter(
        EndpointUiModelPreviewParameterProvider::class
    ) endpoint: EndpointUiModel
) {
    MaterialTheme {
        Surface {
            EndpointStateChip(
                endpointMockState = endpoint.currentState
            )
        }
    }
}
