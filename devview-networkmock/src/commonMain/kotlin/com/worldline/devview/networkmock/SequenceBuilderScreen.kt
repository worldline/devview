package com.worldline.devview.networkmock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.worldline.devview.networkmock.components.LoadingState
import com.worldline.devview.networkmock.components.MockItem
import com.worldline.devview.networkmock.core.model.MockResponse
import com.worldline.devview.networkmock.core.model.StatusCodeFamily
import com.worldline.devview.networkmock.viewmodel.OperationDetailViewModel
import com.worldline.devview.networkmock.viewmodel.OperationSheetState
import com.worldline.devview.networkmock.viewmodel.SequenceDraftStep
import com.worldline.devview.networkmock.viewmodel.asState
import com.worldline.devview.ui.DevViewGroupedDivider
import com.worldline.devview.ui.DevViewSectionHeader
import com.worldline.devview.ui.devViewGroupedItem

@Composable
internal fun SequenceBuilderScreen(
    viewModel: OperationDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val mutation by viewModel.mutations.state.collectAsStateWithLifecycle()
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
            is OperationSheetState.Content -> SequenceBuilderContent(
                responses = content.responses,
                steps = draft,
                enabled = !mutation.busy,
                onAdd = viewModel::add,
                onRemove = viewModel::remove,
                onMove = viewModel::move,
                onSave = viewModel::saveSequence,
                onBack = { if (viewModel.mutations.interactionsEnabled) onBack() },
                onReload = viewModel::reload
            )
        }
    }
}

@Composable
internal fun SequenceBuilderContent(
    responses: List<MockResponse>,
    steps: List<SequenceDraftStep>,
    enabled: Boolean,
    onAdd: (MockResponse) -> Unit,
    onRemove: (String) -> Unit,
    onMove: (String, Int) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onReload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unresolved = steps.any { step -> responses.none { it.asState() == step.response } }
    val editable = enabled && !unresolved
    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(
                    weight = 1f
                ).testTag(tag = "sequence_builder_list")
        ) {
            item(key = "explainer") {
                Text(
                    text = "Pick at least 2 responses in order. The last step repeats.",
                    modifier = Modifier.padding(all = 16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            item(key = "steps_header") { DevViewSectionHeader(text = "Steps") }
            if (steps.isEmpty()) {
                item(key = "empty_steps") {
                    Text(
                        text = "No steps yet. Choose a response below.",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
            itemsIndexed(items = steps, key = { _, step -> step.id }) { index, step ->
                val response = responses.firstOrNull { it.asState() == step.response }
                val label = response?.let { "${it.displayName} · ${it.mediaType}" }
                    ?: "${step.response.statusCode} · ${step.response.mediaType} · " +
                    "${step.response.exampleName} (unavailable)"
                Column(
                    modifier = Modifier
                        .padding(
                            horizontal = 16.dp
                        ).devViewGroupedItem(
                            index = index,
                            count = steps.size
                        ).testTag(tag = "sequence_step_${step.id}")
                ) {
                    Text(
                        text = "${index + 1}. $label",
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            enabled = editable && index > 0,
                            onClick = { onMove(step.id, -1) },
                            modifier = Modifier.testTag(tag = "sequence_up_${step.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowUpward,
                                contentDescription = "Move step ${index + 1} up"
                            )
                        }
                        IconButton(
                            enabled = editable && index < steps.lastIndex,
                            onClick = { onMove(step.id, 1) },
                            modifier = Modifier.testTag(tag = "sequence_down_${step.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowDownward,
                                contentDescription = "Move step ${index + 1} down"
                            )
                        }
                        IconButton(
                            enabled = editable,
                            onClick = { onRemove(step.id) },
                            modifier = Modifier.testTag(tag = "sequence_remove_${step.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Remove step ${index + 1}"
                            )
                        }
                    }
                    if (index != steps.lastIndex) DevViewGroupedDivider()
                }
            }
            if (unresolved) {
                item(key = "unresolved") {
                    Column(modifier = Modifier.padding(all = 16.dp)) {
                        Text(
                            text = "A saved step is unavailable. Your draft is kept unchanged. " +
                                "Reload the specification before editing or saving."
                        )
                        TextButton(
                            enabled = enabled,
                            onClick = onReload
                        ) { Text(text = "Reload specification") }
                    }
                }
            }
            responses
                .groupBy {
                    StatusCodeFamily.fromStatusCode(
                        statusCode = it.statusCode
                    )
                }.forEach { (family, variants) ->
                    item(
                        key = "header_${family.name}"
                    ) { DevViewSectionHeader(text = "${family.displayName} responses") }
                    variants.forEachIndexed { index, response ->
                        item(
                            key = "response_${response.statusCode}_${response.mediaType}_${response.exampleName}"
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(
                                        horizontal = 16.dp
                                    ).devViewGroupedItem(index = index, count = variants.size)
                            ) {
                                MockItem(
                                    mockResponse = response,
                                    secondaryLabel = response.mediaType,
                                    selected = false,
                                    enabled = editable,
                                    selectionControl = false,
                                    onClick = { onAdd(response) },
                                    modifier = Modifier.testTag(
                                        tag = "sequence_response_${response.statusCode}_" +
                                            "${response.mediaType}_${response.exampleName}"
                                    )
                                )
                                if (index != variants.lastIndex) {
                                    DevViewGroupedDivider(
                                        modifier = Modifier.padding(start = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
        }
        HorizontalDivider()
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            if (steps.size < 2) {
                val missing = 2 - steps.size
                Text(
                    text = "Add $missing more ${if (missing == 1) "step" else "steps"}",
                    modifier = Modifier.testTag(tag = "sequence_save_helper")
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                OutlinedButton(
                    enabled = editable && steps.size >= 2,
                    onClick = onSave,
                    modifier = Modifier.testTag(tag = "sequence_save_button")
                ) {
                    Text(text = "Save sequence (${steps.size})")
                }
                TextButton(
                    enabled = enabled,
                    onClick = onBack,
                    modifier = Modifier.testTag(tag = "sequence_cancel_button")
                ) { Text(text = "Cancel") }
            }
        }
    }
}
