package com.worldline.devview.networkmock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.worldline.devview.networkmock.components.EndpointStateChip
import com.worldline.devview.networkmock.components.InlineDiffContent
import com.worldline.devview.networkmock.components.SplitDiffContent
import com.worldline.devview.networkmock.core.model.MockResponse
import com.worldline.devview.networkmock.core.model.OperationMockState
import com.worldline.devview.networkmock.preview.PreviewSheetStatePreviewParameterProvider
import com.worldline.devview.networkmock.utils.fake

/**
 * The preview page of [NetworkMockOperationSheet]: opens one response directly from its eye icon,
 * with a Compare with dropdown to choose a second response. Back returns to the picker.
 *
 * When [previewSheetState] is [PreviewSheetState.Single], renders the single response's body
 * using a split-diff view with no right-hand side. When it is [PreviewSheetState.Compare], the
 * two responses are diffed against each other: an inline diff is shown when the contents are
 * similar enough (as determined by [PreviewSheetState.Compare.useInlineDiff]), otherwise a
 * side-by-side split diff is used.
 *
 * @param previewSheetState The current state of the preview page, holding either one or two
 * responses to display.
 * @param otherResponses Available comparison responses, excluding the primary response.
 * @param onCompareWith Called when a comparison response is chosen.
 * @param onBack Called when the user taps the back arrow, returning to the operation picker page.
 * @param modifier [Modifier] to be applied to the root [Column].
 */
@Composable
internal fun MockResponsePreviewPage(
    previewSheetState: PreviewSheetState.HasResponse,
    otherResponses: List<MockResponse>,
    onCompareWith: (MockResponse) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var compareExpanded by remember { mutableStateOf(value = false) }
    val primary = when (previewSheetState) {
        is PreviewSheetState.Single -> previewSheetState.response
        is PreviewSheetState.Compare -> previewSheetState.first
    }
    val hasMultipleMediaTypes = (otherResponses + primary).map { it.mediaType }.distinct().size > 1
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        PreviewPageHeader(
            previewSheetState = previewSheetState,
            showMediaType = hasMultipleMediaTypes,
            onBack = onBack
        )
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            TextButton(
                modifier = Modifier.testTag(tag = "preview_compare_button"),
                enabled = otherResponses.isNotEmpty(),
                onClick = { compareExpanded = true }
            ) { Text(text = "Compare with…") }
            DropdownMenu(
                expanded = compareExpanded,
                onDismissRequest = { compareExpanded = false }
            ) {
                otherResponses.forEach { response ->
                    DropdownMenuItem(
                        modifier = Modifier.testTag(
                            tag = "preview_compare_${response.statusCode}_${response.mediaType}_${response.exampleName}"
                        ),
                        text = {
                            Text(
                                text =
                                    response.displayName +
                                        if (hasMultipleMediaTypes) " · ${response.mediaType}" else ""
                            )
                        },
                        onClick = {
                            compareExpanded = false
                            onCompareWith(response)
                        }
                    )
                }
            }
        }
        HorizontalDivider()
        Box(
            modifier = Modifier.fillMaxWidth().weight(weight = 1f).testTag(tag = "preview_content")
        ) {
            when (previewSheetState) {
                is PreviewSheetState.Single -> SplitDiffContent(
                    modifier = Modifier.fillMaxSize(),
                    first = previewSheetState.response,
                    second = null
                )
                is PreviewSheetState.Compare -> if (previewSheetState.useInlineDiff) {
                    InlineDiffContent(
                        modifier = Modifier.fillMaxSize(),
                        diff = previewSheetState.lineDiff,
                        leftLabel = previewSheetState.first.previewLabel(
                            showMediaType = hasMultipleMediaTypes
                        ),
                        rightLabel = previewSheetState.second.previewLabel(
                            showMediaType = hasMultipleMediaTypes
                        )
                    )
                } else {
                    SplitDiffContent(
                        modifier = Modifier.fillMaxSize(),
                        first = previewSheetState.first,
                        second = previewSheetState.second
                    )
                }
            }
        }
    }
}

/**
 * Header bar for [MockResponsePreviewPage]: a back arrow and stacked response chips,
 * separated by a "vs" label when comparing, so long names fit on narrow screens.
 *
 * @param previewSheetState The current preview state, determining which chips are rendered.
 * @param onBack Called when the user taps the back arrow.
 * @param modifier [Modifier] to be applied to the root [Row].
 */
@Composable
private fun PreviewPageHeader(
    previewSheetState: PreviewSheetState.HasResponse,
    showMediaType: Boolean,
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
        Column(
            modifier = Modifier.weight(weight = 1f),
            verticalArrangement = Arrangement.spacedBy(space = 4.dp)
        ) {
            when (previewSheetState) {
                is PreviewSheetState.Single -> {
                    EndpointStateChip(
                        endpointMockState = OperationMockState.Mock(
                            statusCode = previewSheetState.response.statusCode,
                            mediaType = previewSheetState.response.mediaType,
                            exampleName = previewSheetState.response.exampleName
                        ),
                        label = previewSheetState.response.previewLabel(
                            showMediaType = showMediaType
                        )
                    )
                }

                is PreviewSheetState.Compare -> {
                    EndpointStateChip(
                        endpointMockState = OperationMockState.Mock(
                            statusCode = previewSheetState.first.statusCode,
                            mediaType = previewSheetState.first.mediaType,
                            exampleName = previewSheetState.first.exampleName
                        ),
                        label = previewSheetState.first.previewLabel(showMediaType = showMediaType)
                    )
                    Text(
                        text = "vs",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    EndpointStateChip(
                        endpointMockState = OperationMockState.Mock(
                            statusCode = previewSheetState.second.statusCode,
                            mediaType = previewSheetState.second.mediaType,
                            exampleName = previewSheetState.second.exampleName
                        ),
                        label = previewSheetState.second.previewLabel(showMediaType = showMediaType)
                    )
                }
            }
        }
    }
}

private fun MockResponse.previewLabel(showMediaType: Boolean): String =
    displayName + if (showMediaType) " · $mediaType" else ""

@Preview(locale = "en")
@Composable
private fun MockResponsePreviewPagePreview(
    @PreviewParameter(
        PreviewSheetStatePreviewParameterProvider::class
    ) previewSheetState: PreviewSheetState.HasResponse
) {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            MockResponsePreviewPage(
                previewSheetState = previewSheetState,
                otherResponses = MockResponse.fake(amount = 6),
                onCompareWith = {},
                onBack = {}
            )
        }
    }
}
