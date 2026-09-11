package com.worldline.devview.networkmock.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.worldline.devview.networkmock.core.model.MockResponse
import com.worldline.devview.networkmock.core.model.OperationMockState
import com.worldline.devview.networkmock.preview.MockResponsePreviewParameterProvider
import com.worldline.devview.networkmock.utils.containerColor
import com.worldline.devview.networkmock.utils.containerColorForStatusCode
import com.worldline.devview.networkmock.utils.contentColor
import com.worldline.devview.networkmock.utils.contentColorForStatusCode
import com.worldline.devview.networkmock.utils.fake
import com.worldline.devview.networkmock.utils.icon
import com.worldline.devview.networkmock.utils.iconForStatusCode
import com.worldline.devview.utils.preview.BooleanPreviewParameterProvider

/**
 * A single selectable response variant row in the operation picker sheet.
 *
 * @param mockResponse The response variant this row represents
 * @param onClick Called when the row itself is tapped — activates this response and closes the sheet
 * @param isMarkedForPreview Whether this response is currently marked for the preview/compare page
 * @param onToggleMarkedForPreview Called when the trailing preview toggle is tapped
 * @param selected Whether this response is the operation's currently active mock
 * @param previewToggleTestTag Test tag for the trailing preview toggle button. Callers rendering
 * more than one [MockItem] must pass a tag unique per response — the default is shared.
 */
@Composable
internal fun MockItem(
    mockResponse: MockResponse,
    onClick: () -> Unit,
    isMarkedForPreview: Boolean,
    onToggleMarkedForPreview: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    previewToggleTestTag: String = "mock_item_preview_toggle"
) {
    MockItemContent(
        modifier = modifier,
        statusCode = mockResponse.statusCode,
        label = mockResponse.displayName,
        selected = selected,
        onClick = onClick,
        isMarkedForPreview = isMarkedForPreview,
        onToggleMarkedForPreview = onToggleMarkedForPreview,
        previewToggleTestTag = previewToggleTestTag
    )
}

/** The "no mock" row — routes the operation to the actual network. Has nothing to preview. */
@Composable
internal fun NetworkItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    MockItemContent(
        modifier = modifier,
        statusCode = null,
        label = OperationMockState.Network.displayName,
        selected = selected,
        isNetwork = true,
        onClick = onClick,
        isMarkedForPreview = false,
        onToggleMarkedForPreview = null,
        previewToggleTestTag = "mock_item_preview_toggle"
    )
}

@Composable
private fun MockItemContent(
    statusCode: Int?,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    isMarkedForPreview: Boolean,
    onToggleMarkedForPreview: (() -> Unit)?,
    previewToggleTestTag: String,
    modifier: Modifier = Modifier,
    isNetwork: Boolean = false
) {
    val (icon, contentColor, containerColor) = when (isNetwork) {
        true -> {
            val state = OperationMockState.Network
            Triple(
                first = state.icon,
                second = state.contentColor,
                third = state.containerColor
            )
        }

        false -> {
            requireNotNull(value = statusCode) {
                "Status code must not be null for non-network items"
            }
            Triple(
                first = iconForStatusCode(statusCode = statusCode),
                second = contentColorForStatusCode(statusCode = statusCode),
                third = containerColorForStatusCode(statusCode = statusCode)
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = true,
                onClick = {
                    if (!selected) {
                        onClick()
                    }
                }
            ).then(
                other = modifier
                    .minimumInteractiveComponentSize()
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 16.dp)
    ) {
        Icon(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .clip(shape = MaterialTheme.shapes.small)
                .background(color = containerColor)
                .padding(all = 4.dp),
            imageVector = icon,
            contentDescription = null,
            tint = contentColor
        )
        Text(
            modifier = Modifier
                .weight(weight = 1f),
            text = label,
            style = MaterialTheme.typography.bodyLargeEmphasized
        )
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null
            )
        }
        if (onToggleMarkedForPreview != null) {
            IconButton(
                modifier = Modifier.testTag(tag = previewToggleTestTag),
                onClick = onToggleMarkedForPreview
            ) {
                Icon(
                    imageVector = if (isMarkedForPreview) {
                        Icons.Rounded.Visibility
                    } else {
                        Icons.Rounded.VisibilityOff
                    },
                    contentDescription = if (isMarkedForPreview) {
                        "Remove from preview"
                    } else {
                        "Add to preview"
                    }
                )
            }
        }
    }
}

@Preview(locale = "en")
@Composable
private fun MockItemPreview(
    @PreviewParameter(MockResponsePreviewParameterProvider::class) mockResponse: MockResponse
) {
    MaterialTheme {
        Surface {
            MockItem(
                mockResponse = mockResponse,
                onClick = {},
                isMarkedForPreview = false,
                onToggleMarkedForPreview = {}
            )
        }
    }
}

@Preview(locale = "en")
@Composable
private fun NetworkItemPreview(
    @PreviewParameter(BooleanPreviewParameterProvider::class) selected: Boolean
) {
    MaterialTheme {
        Surface {
            NetworkItem(
                selected = selected,
                onClick = {}
            )
        }
    }
}

@Preview(locale = "en")
@Composable
private fun MockItemSelectedPreview(
    @PreviewParameter(BooleanPreviewParameterProvider::class) selected: Boolean
) {
    MaterialTheme {
        Surface {
            MockItem(
                mockResponse = MockResponse.fake().first(),
                selected = selected,
                onClick = {},
                isMarkedForPreview = false,
                onToggleMarkedForPreview = {}
            )
        }
    }
}

@Preview(locale = "en")
@Composable
private fun MockItemMarkedForPreviewPreview(
    @PreviewParameter(BooleanPreviewParameterProvider::class) isMarkedForPreview: Boolean
) {
    MaterialTheme {
        Surface {
            MockItem(
                mockResponse = MockResponse.fake().first(),
                selected = false,
                onClick = {},
                isMarkedForPreview = isMarkedForPreview,
                onToggleMarkedForPreview = {}
            )
        }
    }
}
