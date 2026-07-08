package com.worldline.devview.networkmock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.worldline.devview.networkmock.components.EndpointStateChip
import com.worldline.devview.networkmock.components.InlineDiffContent
import com.worldline.devview.networkmock.components.SplitDiffContent
import com.worldline.devview.networkmock.core.model.EndpointMockState
import com.worldline.devview.networkmock.preview.PreviewSheetStatePreviewParameterProvider
import kotlinx.coroutines.launch

/**
 * A modal bottom sheet that displays a preview of one or two [MockResponse] payloads for a
 * network mock endpoint.
 *
 * When [previewSheetState] is [PreviewSheetState.Single], the sheet renders the single response
 * content using a split-diff view with no right-hand side. When it is [PreviewSheetState.Compare],
 * the two responses are diffed against each other: an inline diff is shown when the contents are
 * similar enough (as determined by [PreviewSheetState.Compare.useInlineDiff]), otherwise a
 * side-by-side split diff is used.
 *
 * @param previewSheetState The current state of the preview sheet, holding either one or two
 * responses to display.
 * @param onDismissRequest Called when the sheet should be dismissed (e.g. the user swipes it down
 * or taps outside).
 * @param modifier [Modifier] to be applied to the [ModalBottomSheet].
 */
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
        ) {
            PreviewHeader(
                previewSheetState = previewSheetState,
                onClose = onClose
            )

            when (previewSheetState) {
                is PreviewSheetState.Single -> {
                    SplitDiffContent(
                        first = previewSheetState.response,
                        second = null
                    )
                }

                is PreviewSheetState.Compare -> if (previewSheetState.useInlineDiff) {
                    InlineDiffContent(
                        diff = previewSheetState.lineDiff,
                        leftLabel = previewSheetState.first.fileName,
                        rightLabel = previewSheetState.second.fileName
                    )
                } else {
                    SplitDiffContent(
                        first = previewSheetState.first,
                        second = previewSheetState.second
                    )
                }
            }
        }
    }
}

/**
 * Header bar displayed inside the bottom sheet for either a single preview or a comparison.
 *
 * When [previewSheetState] is [PreviewSheetState.Single], shows a single [EndpointStateChip] and
 * a close button. When it is [PreviewSheetState.Compare], shows two [EndpointStateChip]s separated
 * by a "vs" label, a close button, and a colour legend when
 * [PreviewSheetState.Compare.useInlineDiff] is `true`.
 *
 * @param previewSheetState The current preview state, determining which chips are rendered.
 * @param onClose Called when the user taps the close button.
 * @param modifier [Modifier] to be applied to the root layout.
 */
@Composable
private fun PreviewHeader(
    previewSheetState: PreviewSheetState.HasResponse,
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
                when (previewSheetState) {
                    is PreviewSheetState.Single -> {
                        EndpointStateChip(
                            endpointMockState = EndpointMockState.Mock(
                                responseFile = previewSheetState.response.fileName
                            ),
                            label = previewSheetState.response.displayName
                        )
                    }

                    is PreviewSheetState.Compare -> {
                        EndpointStateChip(
                            endpointMockState = EndpointMockState.Mock(
                                responseFile = previewSheetState.first.fileName
                            ),
                            label = previewSheetState.first.displayName
                        )
                        Text(
                            text = "vs",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        EndpointStateChip(
                            endpointMockState = EndpointMockState.Mock(
                                responseFile = previewSheetState.second.fileName
                            ),
                            label = previewSheetState.second.displayName
                        )
                    }
                }
            }
            IconButton(
                modifier = Modifier.align(alignment = Alignment.CenterEnd),
                onClick = onClose
            ) {
                Icon(imageVector = Icons.Rounded.Close, contentDescription = null)
            }
        }
    }
}

@Preview(locale = "en")
@Composable
private fun NetworkMockEndpointPreviewBottomSheetPreview(
    @PreviewParameter(
        PreviewSheetStatePreviewParameterProvider::class
    ) previewSheetState: PreviewSheetState.HasResponse
) {
    MaterialTheme {
        Surface {
            NetworkMockEndpointPreviewBottomSheet(
                previewSheetState = previewSheetState
            )
        }
    }
}
