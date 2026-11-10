package com.worldline.devview.networkmock.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.worldline.devview.networkmock.core.model.FailureKind
import com.worldline.devview.networkmock.core.model.MockResponse
import com.worldline.devview.networkmock.core.model.OperationMockState

/** Response choice with a separate, labelled details action. Builders use plain add rows. */
@Composable
internal fun MockItem(
    mockResponse: MockResponse,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    previewToggleTestTag: String = "mock_item_preview_toggle",
    secondaryLabel: String? = null,
    onPreview: (() -> Unit)? = null,
    enabled: Boolean = true,
    selectionControl: Boolean = true
) {
    MockItemContent(
        label = mockResponse.displayName,
        status = OperationMockState.Mock(
            statusCode = mockResponse.statusCode,
            mediaType = mockResponse.mediaType,
            exampleName = mockResponse.exampleName
        ),
        secondaryLabel = secondaryLabel,
        isSynthesized = mockResponse.isSynthesized,
        selected = selected,
        onClick = onClick,
        onPreview = onPreview,
        previewToggleTestTag = previewToggleTestTag,
        enabled = enabled,
        selectionControl = selectionControl,
        modifier = modifier
    )
}

/** Routes the operation to the actual network. */
@Composable
internal fun NetworkItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true
) {
    MockItemContent(
        label = "Use actual network",
        secondaryLabel = "Bypass this operation's mock",
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
    )
}

/** Selects a deterministic transport failure. */
@Composable
internal fun FailureItem(
    kind: FailureKind,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true
) {
    MockItemContent(
        label = OperationMockState.Failure(kind = kind).displayName,
        status = OperationMockState.Failure(kind = kind),
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
    )
}

@Composable
private fun MockItemContent(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onPreview: (() -> Unit)? = null,
    previewToggleTestTag: String = "mock_item_preview_toggle",
    secondaryLabel: String? = null,
    status: OperationMockState? = null,
    isSynthesized: Boolean = false,
    selectionControl: Boolean = true
) {
    val interaction = if (selectionControl) {
        Modifier.selectable(
            selected = selected,
            enabled = enabled,
            role = Role.RadioButton,
            onClick = onClick
        )
    } else {
        Modifier.clickable(enabled = enabled, onClick = onClick)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(other = interaction)
            .minimumInteractiveComponentSize()
            .heightIn(min = 72.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp)
    ) {
        if (selectionControl) RadioButton(selected = selected, onClick = null, enabled = enabled)
        Column(
            modifier = Modifier.weight(weight = 1f),
            verticalArrangement = Arrangement.spacedBy(space = 4.dp)
        ) {
            if (status != null) {
                EndpointStateChip(
                    endpointMockState = status,
                    chipTestTag = "${previewToggleTestTag}_status",
                    label = if (status is OperationMockState.Mock) status.statusCode.toString() else "Transport failure"
                )
            }
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            if (!secondaryLabel.isNullOrBlank()) {
                Text(
                    text = secondaryLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSynthesized) {
                Text(text = "Generated", style = MaterialTheme.typography.labelSmall)
            }
        }
        if (onPreview != null) {
            TextButton(
                modifier = Modifier
                    .testTag(tag = previewToggleTestTag)
                    .semantics { contentDescription = "Open $label details" },
                enabled = enabled,
                onClick = onPreview
            ) { Text(text = "Details") }
        }
    }
}
