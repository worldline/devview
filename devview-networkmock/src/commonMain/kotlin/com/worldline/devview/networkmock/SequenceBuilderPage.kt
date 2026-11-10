package com.worldline.devview.networkmock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import com.worldline.devview.networkmock.components.MockItem
import com.worldline.devview.networkmock.core.model.MockResponse
import com.worldline.devview.networkmock.core.model.OperationMockState
import com.worldline.devview.networkmock.model.OperationUiModel
import com.worldline.devview.networkmock.utils.fake
import com.worldline.devview.networkmock.viewmodel.OperationSheetState
import kotlinx.collections.immutable.toPersistentList

/**
 * Builds an ordered sequence, initially populated from the operation's active sequence.
 * Tapping an available response appends a step (including repeats); each step can be removed.
 * Back and Cancel discard the draft and return to the picker without changing the active mock.
 *
 * @param content The operation and available response bodies used to resolve active sequence steps.
 * @param onBack Called by the back arrow or Cancel.
 * @param onCommitSequence Called by Save with the ordered draft, enabled only for at least two steps.
 * @param modifier [Modifier] applied to the root [Column].
 * @param showHeader Whether to show the standalone back/title header.
 * @param onResetSequencePosition Called by Reset for an active sequence.
 */
@Composable
internal fun SequenceBuilderPage(
    content: OperationSheetState.Content,
    onBack: () -> Unit,
    onCommitSequence: (List<MockResponse>) -> Unit,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    onResetSequencePosition: () -> Unit = {}
) {
    var steps by remember(key1 = content.operationUiModel.descriptor.key) {
        mutableStateOf(
            value = (content.operationUiModel.currentState as? OperationMockState.Sequence)
                ?.responses
                .orEmpty()
                .toPersistentList()
        )
    }
    val currentSteps = steps
    val resolvedSteps = currentSteps.map { step ->
        content.responses.find {
            it.statusCode == step.statusCode && it.mediaType == step.mediaType &&
                it.exampleName == step.exampleName
        }
    }
    val hasUnavailableSteps = resolvedSteps.any { it == null }
    val hasMultipleMediaTypes = content.responses
        .map { it.mediaType }
        .distinct()
        .size > 1

    Column(modifier = modifier.fillMaxSize()) {
        if (showHeader) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    modifier = Modifier.testTag(tag = "sequence_page_back_button"),
                    onClick = onBack
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Text(text = "Build a Sequence", style = MaterialTheme.typography.titleMedium)
            }
            HorizontalDivider()
        }
        val activeSequence = content.operationUiModel.currentState as? OperationMockState.Sequence
        if (activeSequence != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(weight = 1f).testTag(tag = "sequence_position"),
                    text = "Step ${activeSequence.currentIndex + 1} of ${activeSequence.responses.size}",
                    style = MaterialTheme.typography.bodySmall
                )
                TextButton(
                    modifier = Modifier.testTag(tag = "sequence_reset_button"),
                    onClick = onResetSequencePosition
                ) { Text(text = "Reset") }
            }
        }
        Text(
            modifier = Modifier
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ).testTag(tag = "sequence_build_hint"),
            text = if (hasUnavailableSteps) {
                "Remove unavailable steps before saving (${steps.size} added)."
            } else {
                "Tap responses below in order (${steps.size} added). The last step repeats."
            },
            style = MaterialTheme.typography.bodySmall
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(
                    weight = 1f
                ).testTag(tag = "sequence_builder_list")
        ) {
            item(key = "steps_header") {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    text = "STEPS",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            currentSteps.forEachIndexed { index, response ->
                val resolvedResponse = resolvedSteps[index]
                item(key = "sequence_step_$index") {
                    Row(
                        modifier = Modifier
                            .animateItem()
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag(tag = "sequence_step_$index"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
                    ) {
                        Text(text = "${index + 1}.", style = MaterialTheme.typography.labelLarge)
                        Column(modifier = Modifier.weight(weight = 1f)) {
                            Text(
                                text =
                                    resolvedResponse?.displayName
                                        ?: "${response.displayName} (unavailable)",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (hasMultipleMediaTypes && response.mediaType.isNotBlank()) {
                                Text(
                                    text = response.mediaType,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        IconButton(
                            modifier = Modifier.testTag(tag = "sequence_remove_step_$index"),
                            onClick = { steps = steps.removingAt(index = index) }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Remove step ${index + 1}"
                            )
                        }
                    }
                }
            }
            stickyHeader(key = "responses_header") {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    text = "AVAILABLE RESPONSES",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            content.responses.forEach { response ->
                val itemKey = "sequence_response_${response.statusCode}_${response.mediaType}_${response.exampleName}"
                item(key = itemKey) {
                    MockItem(
                        modifier = Modifier
                            .animateItem()
                            .padding(
                                horizontal = 16.dp
                            ).testTag(tag = itemKey),
                        mockResponse = response,
                        secondaryLabel = response.mediaType.takeIf {
                            hasMultipleMediaTypes &&
                                it.isNotBlank()
                        },
                        onClick = {
                            steps = steps.adding(
                                element = OperationMockState.Mock(
                                    statusCode = response.statusCode,
                                    mediaType = response.mediaType,
                                    exampleName = response.exampleName
                                )
                            )
                        }
                    )
                }
            }
        }
        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth().padding(all = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                modifier = Modifier.testTag(tag = "sequence_cancel_button"),
                onClick = onBack
            ) { Text(text = "Cancel") }
            Button(
                modifier = Modifier.weight(weight = 1f).testTag(tag = "sequence_save_button"),
                enabled = steps.size >= 2 && !hasUnavailableSteps,
                onClick = { onCommitSequence(resolvedSteps.filterNotNull()) }
            ) { Text(text = "Save Sequence (${steps.size})") }
        }
    }
}

@Preview(locale = "en")
@Composable
private fun SequenceBuilderPagePreview() {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            SequenceBuilderPage(
                content = OperationSheetState.Content(
                    operationUiModel = OperationUiModel.fake().first(),
                    responses = MockResponse.fake(amount = 6).toPersistentList()
                ),
                onBack = {},
                onCommitSequence = {}
            )
        }
    }
}
