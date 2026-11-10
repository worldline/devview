package com.worldline.devview.networkmock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.worldline.devview.networkmock.components.EndpointStateChip
import com.worldline.devview.networkmock.components.ErrorState
import com.worldline.devview.networkmock.components.InlineDiffContent
import com.worldline.devview.networkmock.components.LoadingState
import com.worldline.devview.networkmock.components.SplitDiffContent
import com.worldline.devview.networkmock.core.model.MockResponse
import com.worldline.devview.networkmock.core.model.OperationMockState
import com.worldline.devview.networkmock.viewmodel.MutationOutcome
import com.worldline.devview.networkmock.viewmodel.OperationDetailViewModel
import com.worldline.devview.networkmock.viewmodel.OperationSheetState
import com.worldline.devview.networkmock.viewmodel.asState
import com.worldline.devview.ui.rememberCopyToClipboard

@Composable
internal fun ResponseDetailScreen(
    destination: NetworkMockDestination.ResponseDetail,
    viewModel: OperationDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val mutation by viewModel.mutations.state.collectAsStateWithLifecycle()
    var compared by remember { mutableStateOf<MockResponse?>(value = null) }
    ReturnAfterSelection(viewModel = viewModel, onBack = onBack)
    Column(modifier = modifier.fillMaxSize().padding(bottom = bottomPadding)) {
        MutationStatus(
            state = mutation,
            onRetry = viewModel.mutations::retryFailed,
            onCancel = viewModel.mutations::cancelFailed
        )
        when (val content = state) {
            OperationSheetState.Hidden, OperationSheetState.Loading -> LoadingState()
            is OperationSheetState.Error -> OperationDetailError(
                message = content.message,
                viewModel = viewModel
            )
            is OperationSheetState.Content -> {
                val primary = content.responses.firstOrNull {
                    it.statusCode == destination.statusCode && it.mediaType == destination.mediaType &&
                        it.exampleName == destination.exampleName
                }
                if (primary == null) {
                    OperationDetailError(
                        message = "This response is no longer available. " +
                            "Reload the specification or return to the list.",
                        viewModel = viewModel
                    )
                } else {
                    val second = compared?.let { selected ->
                        content.responses.firstOrNull {
                            it.asState() ==
                                selected.asState()
                        }
                    }
                    val preview = rememberResponsePreview(primary = primary, second = second)
                    ResponseDetailContent(
                        preview = preview,
                        otherResponses = content.responses.filter {
                            it.asState() !=
                                primary.asState()
                        },
                        currentState = content.operationUiModel.currentState,
                        enabled = !mutation.busy,
                        onCompare = { if (viewModel.mutations.interactionsEnabled) compared = it },
                        onUse = {
                            if (viewModel.mutations.interactionsEnabled) {
                                viewModel.apply(
                                    response = primary
                                )
                            }
                        },
                        onBack = { if (viewModel.mutations.interactionsEnabled) onBack() }
                    )
                }
            }
        }
    }
}

// Kotlin function-type callbacks do not accept named arguments.
@Suppress("NamedArguments")
@Composable
internal fun ResponseDetailContent(
    preview: PreviewSheetState.HasResponse,
    otherResponses: List<MockResponse>,
    currentState: OperationMockState,
    enabled: Boolean,
    onCompare: (MockResponse) -> Unit,
    onUse: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(value = false) }
    val copy = rememberCopyToClipboard()
    val primary = when (preview) {
        is PreviewSheetState.Single -> preview.response
        is PreviewSheetState.Compare -> preview.first
    }
    val showMedia = (otherResponses + primary).map { it.mediaType }.distinct().size > 1
    val inUse = currentState == primary.asState()
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp)
        ) {
            EndpointStateChip(
                endpointMockState = primary.asState(),
                label = primary.detailLabel(showMedia = showMedia)
            )
            if (preview is PreviewSheetState.Compare) {
                Text(text = "vs", style = MaterialTheme.typography.labelSmall)
                EndpointStateChip(
                    endpointMockState = preview.second.asState(),
                    label = preview.second.detailLabel(showMedia = showMedia)
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                if (otherResponses.isNotEmpty()) {
                    Box {
                        OutlinedButton(
                            enabled = enabled,
                            onClick = { expanded = true },
                            modifier = Modifier.testTag(tag = "preview_compare_button")
                        ) {
                            Text(text = "Compare with…")
                        }
                        DropdownMenu(
                            expanded = expanded && enabled,
                            onDismissRequest = { expanded = false }
                        ) {
                            otherResponses.forEach { response ->
                                DropdownMenuItem(
                                    modifier = Modifier.testTag(
                                        tag = "preview_compare_${response.statusCode}_" +
                                            "${response.mediaType}_${response.exampleName}"
                                    ),
                                    text = {
                                        Text(
                                            text = response.detailLabel(showMedia = showMedia)
                                        )
                                    },
                                    onClick = {
                                        expanded = false
                                        onCompare(response)
                                    }
                                )
                            }
                        }
                    }
                }
                OutlinedButton(
                    onClick = { copy(primary.content) },
                    modifier = Modifier.testTag(tag = "response_copy_body")
                ) {
                    Text(text = "Copy body")
                }
            }
        }
        HorizontalDivider()
        SelectionContainer(
            modifier = Modifier.fillMaxWidth().weight(weight = 1f).testTag(tag = "preview_content")
        ) {
            when (preview) {
                is PreviewSheetState.Single -> SplitDiffContent(
                    modifier = Modifier.fillMaxSize(),
                    first = primary,
                    second = null
                )
                is PreviewSheetState.Compare -> if (preview.useInlineDiff) {
                    InlineDiffContent(
                        modifier = Modifier.fillMaxSize(),
                        diff = preview.lineDiff,
                        leftLabel = primary.detailLabel(showMedia = showMedia),
                        rightLabel = preview.second.detailLabel(showMedia = showMedia)
                    )
                } else {
                    SplitDiffContent(
                        modifier = Modifier.fillMaxSize(),
                        first = primary,
                        second = preview.second
                    )
                }
            }
        }
        HorizontalDivider()
        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
        ) {
            OutlinedButton(
                enabled = enabled && !inUse,
                onClick = onUse,
                modifier = Modifier.testTag(tag = "response_use")
            ) {
                Text(text = if (inUse) "In use" else "Use this response")
            }
            TextButton(
                enabled = enabled,
                onClick = onBack,
                modifier = Modifier.testTag(tag = "response_back")
            ) { Text(text = "Back to list") }
        }
    }
}

@Composable
internal fun ReturnAfterSelection(viewModel: OperationDetailViewModel, onBack: () -> Unit) {
    val ticket by viewModel.selection.collectAsStateWithLifecycle()
    val outcome = ticket?.outcome?.collectAsStateWithLifecycle()?.value
    val mutation by viewModel.mutations.state.collectAsStateWithLifecycle()
    val currentBack by rememberUpdatedState(newValue = onBack)
    LaunchedEffect(key1 = ticket?.id, key2 = outcome, key3 = mutation.busy) {
        val completed = ticket
        if (completed != null && outcome == MutationOutcome.Succeeded &&
            viewModel.acknowledgeSelection(ticketId = completed.id)
        ) {
            currentBack()
        }
    }
}

@Composable
internal fun OperationDetailError(message: String, viewModel: OperationDetailViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(all = 16.dp)) {
        ErrorState(message = message, modifier = Modifier.weight(weight = 1f))
        TextButton(
            enabled = viewModel.mutations.interactionsEnabled,
            onClick = viewModel::reload
        ) { Text(text = "Reload specification") }
    }
}

private fun MockResponse.detailLabel(showMedia: Boolean): String =
    displayName + if (showMedia) " · $mediaType" else ""
