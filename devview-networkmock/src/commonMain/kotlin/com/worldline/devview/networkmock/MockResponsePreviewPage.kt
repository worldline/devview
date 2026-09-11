package com.worldline.devview.networkmock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.worldline.devview.networkmock.components.EndpointStateChip
import com.worldline.devview.networkmock.components.InlineDiffContent
import com.worldline.devview.networkmock.components.SplitDiffContent
import com.worldline.devview.networkmock.core.model.OperationMockState
import com.worldline.devview.networkmock.preview.PreviewSheetStatePreviewParameterProvider

/**
 * Page 2 of the operation sheet ([NetworkMockOperationSheet]): shows the body of one marked
 * response, or a diff between two.
 *
 * When [previewSheetState] is [PreviewSheetState.Single], renders the single response's body
 * using a split-diff view with no right-hand side. When it is [PreviewSheetState.Compare], the
 * two responses are diffed against each other: an inline diff is shown when the contents are
 * similar enough (as determined by [PreviewSheetState.Compare.useInlineDiff]), otherwise a
 * side-by-side split diff is used.
 *
 * @param previewSheetState The current state of the preview page, holding either one or two
 * responses to display.
 * @param onBack Called when the user taps the back arrow, returning to the operation picker page.
 * @param modifier [Modifier] to be applied to the root [Column].
 */
@Composable
internal fun MockResponsePreviewPage(
    previewSheetState: PreviewSheetState.HasResponse,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        PreviewPageHeader(
            previewSheetState = previewSheetState,
            onBack = onBack
        )
        HorizontalDivider()

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
                    leftLabel = previewSheetState.first.displayName,
                    rightLabel = previewSheetState.second.displayName
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

/**
 * Header bar for [MockResponsePreviewPage]: a back arrow, followed by either one
 * [EndpointStateChip] or two separated by a "vs" label.
 *
 * @param previewSheetState The current preview state, determining which chips are rendered.
 * @param onBack Called when the user taps the back arrow.
 * @param modifier [Modifier] to be applied to the root [Row].
 */
@Composable
private fun PreviewPageHeader(
    previewSheetState: PreviewSheetState.HasResponse,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(end = 16.dp, top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            modifier = Modifier.testTag(tag = "preview_page_back_button"),
            onClick = onBack
        ) {
            Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (previewSheetState) {
                is PreviewSheetState.Single -> {
                    EndpointStateChip(
                        endpointMockState = OperationMockState.Mock(
                            statusCode = previewSheetState.response.statusCode,
                            exampleName = previewSheetState.response.exampleName
                        ),
                        label = previewSheetState.response.displayName
                    )
                }

                is PreviewSheetState.Compare -> {
                    EndpointStateChip(
                        endpointMockState = OperationMockState.Mock(
                            statusCode = previewSheetState.first.statusCode,
                            exampleName = previewSheetState.first.exampleName
                        ),
                        label = previewSheetState.first.displayName
                    )
                    Text(
                        text = "vs",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    EndpointStateChip(
                        endpointMockState = OperationMockState.Mock(
                            statusCode = previewSheetState.second.statusCode,
                            exampleName = previewSheetState.second.exampleName
                        ),
                        label = previewSheetState.second.displayName
                    )
                }
            }
        }
    }
}

@Preview(locale = "en")
@Composable
private fun MockResponsePreviewPagePreview(
    @PreviewParameter(
        PreviewSheetStatePreviewParameterProvider::class
    ) previewSheetState: PreviewSheetState.HasResponse
) {
    MaterialTheme {
        Surface {
            MockResponsePreviewPage(
                previewSheetState = previewSheetState,
                onBack = {}
            )
        }
    }
}
