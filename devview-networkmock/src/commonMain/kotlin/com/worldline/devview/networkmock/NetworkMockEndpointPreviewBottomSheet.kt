package com.worldline.devview.networkmock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.worldline.devview.networkmock.components.EndpointStateChip
import com.worldline.devview.networkmock.components.InlineDiffContent
import com.worldline.devview.networkmock.components.SplitDiffContent
import com.worldline.devview.networkmock.components.computeLineDiff
import com.worldline.devview.networkmock.components.shouldUseInlineDiff
import com.worldline.devview.networkmock.model.EndpointMockState
import com.worldline.devview.networkmock.model.MockResponse
import kotlinx.coroutines.launch

@Composable
internal fun NetworkMockEndpointPreviewBottomSheet(
    previewSheetState: PreviewSheetState.HasResponse,
    onDismissRequest: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val scope = rememberCoroutineScope()

    val currentOnDismissRequest by rememberUpdatedState(newValue = onDismissRequest)

    val onClose: () -> Unit = {
        scope
            .launch {
                sheetState.hide()
            }.invokeOnCompletion {
                if (!sheetState.isVisible) currentOnDismissRequest()
            }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(fraction = 0.85f)
        ) {
            when (previewSheetState) {
                is PreviewSheetState.Single -> {
                    SinglePreviewHeader(
                        response = previewSheetState.response,
                        onClose = onClose
                    )
                    SplitDiffContent(
                        left = previewSheetState.response,
                        right = null,
                        modifier = Modifier.weight(weight = 1f)
                    )
                }

                is PreviewSheetState.Compare -> {
                    val first = previewSheetState.first
                    val second = previewSheetState.second
                    val useInlineDiff = remember(key1 = first.content, key2 = second.content) {
                        shouldUseInlineDiff(
                            contentLeft = first.content,
                            contentRight = second.content
                        )
                    }

                    ComparePreviewHeader(
                        first = first,
                        second = second,
                        showLegend = useInlineDiff,
                        onClose = onClose
                    )

                    if (useInlineDiff) {
                        val diff = remember(key1 = first.content, key2 = second.content) {
                            computeLineDiff(
                                contentLeft = first.content,
                                contentRight = second.content
                            )
                        }
                        InlineDiffContent(
                            diff = diff,
                            modifier = Modifier.weight(weight = 1f)
                        )
                    } else {
                        SplitDiffContent(
                            left = first,
                            right = second,
                            modifier = Modifier.weight(weight = 1f)
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Header composables
// ---------------------------------------------------------------------------

@Composable
private fun SinglePreviewHeader(
    response: MockResponse,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        EndpointStateChip(
            endpointMockState = EndpointMockState.Mock(responseFile = response.fileName),
            label = response.displayName
        )
        IconButton(
            modifier = Modifier.align(alignment = Alignment.CenterEnd),
            onClick = onClose
        ) {
            Icon(imageVector = Icons.Rounded.Close, contentDescription = null)
        }
    }
}

@Composable
private fun ComparePreviewHeader(
    first: MockResponse,
    second: MockResponse,
    showLegend: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(space = 6.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EndpointStateChip(
                    endpointMockState = EndpointMockState.Mock(responseFile = first.fileName),
                    label = first.displayName
                )
                Text(
                    text = "vs",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                EndpointStateChip(
                    endpointMockState = EndpointMockState.Mock(responseFile = second.fileName),
                    label = second.displayName
                )
            }
            IconButton(
                modifier = Modifier.align(alignment = Alignment.CenterEnd),
                onClick = onClose
            ) {
                Icon(imageVector = Icons.Rounded.Close, contentDescription = null)
            }
        }

        // Colour legend — only shown in inline diff mode
        if (showLegend) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    label = first.displayName
                )
                LegendDot(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    label = second.displayName
                )
            }
        }
    }
}

@Composable
private fun LegendDot(
    color: androidx.compose.ui.graphics.Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(size = 10.dp)
                .clip(shape = MaterialTheme.shapes.extraSmall)
                .background(color = color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

private val previewLeft = MockResponse(
    statusCode = 200,
    fileName = "endpoint-200.json",
    displayName = "Success (200)",
    content =
        """
        {
          "id": 1,
          "name": "Alice",
          "role": "admin"
        }
        """.trimIndent()
)

private val previewRightSimilar = MockResponse(
    statusCode = 200,
    fileName = "endpoint-200-alt.json",
    displayName = "Success Alt (200)",
    content =
        """
        {
          "id": 2,
          "name": "Bob",
          "role": "user"
        }
        """.trimIndent()
)

private val previewRightDissimilar = MockResponse(
    statusCode = 500,
    fileName = "endpoint-500.json",
    displayName = "Server Error (500)",
    content =
        """
        {
          "error": "InternalServerError",
          "message": "An unexpected error occurred.",
          "trace": "com.example.SomeService.doThing(SomeService.kt:42)"
        }
        """.trimIndent()
)

@Preview(locale = "en")
@Composable
private fun SinglePreviewSheetPreview() {
    MaterialTheme {
        Surface {
            NetworkMockEndpointPreviewBottomSheet(
                previewSheetState = PreviewSheetState.Single(response = previewLeft)
            )
        }
    }
}

@Preview(locale = "en")
@Composable
private fun CompareInlinePreviewSheetPreview() {
    MaterialTheme {
        Surface {
            NetworkMockEndpointPreviewBottomSheet(
                previewSheetState = PreviewSheetState.Compare(
                    first = previewLeft,
                    second = previewRightSimilar
                )
            )
        }
    }
}

@Preview(locale = "en")
@Composable
private fun CompareSplitPreviewSheetPreview() {
    MaterialTheme {
        Surface {
            NetworkMockEndpointPreviewBottomSheet(
                previewSheetState = PreviewSheetState.Compare(
                    first = previewLeft,
                    second = previewRightDissimilar
                )
            )
        }
    }
}
