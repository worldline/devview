package com.worldline.devview.networkmock.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.worldline.devview.networkmock.core.model.EndpointMockState
import com.worldline.devview.networkmock.core.model.MockResponse
import com.worldline.devview.networkmock.preview.MockResponsePreviewParameterProvider
import com.worldline.devview.networkmock.utils.containerColor
import com.worldline.devview.networkmock.utils.containerColorForStatusCode
import com.worldline.devview.networkmock.utils.contentColor
import com.worldline.devview.networkmock.utils.contentColorForStatusCode
import com.worldline.devview.networkmock.utils.fake
import com.worldline.devview.networkmock.utils.icon
import com.worldline.devview.networkmock.utils.iconForStatusCode

@Composable
internal fun MockItem(
    mockResponse: MockResponse,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    MockItemContent(
        modifier = modifier,
        statusCode = mockResponse.statusCode,
        label = mockResponse.fileName,
        selected = selected,
        onClick = onClick
    )
}

@Composable
internal fun NetworkItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    MockItemContent(
        modifier = modifier,
        statusCode = null,
        label = EndpointMockState.Network.displayName,
        selected = selected,
        isNetwork = true,
        onClick = onClick
    )
}

@Composable
private fun MockItemContent(
    statusCode: Int?,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isNetwork: Boolean = false
) {
    val (icon, contentColor, containerColor) = when (isNetwork) {
        true -> {
            val state = EndpointMockState.Network
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
                enabled = !selected,
                onClick = onClick
            ).then(
                other = modifier
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
        AnimatedVisibility(
            visible = selected
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null
            )
        }
    }
}

@Preview(locale = "en")
@Composable
internal fun MockItemPreview(
    @PreviewParameter(MockResponsePreviewParameterProvider::class) mockResponse: MockResponse
) {
    MaterialTheme {
        Surface {
            MockItem(
                mockResponse = mockResponse,
                onClick = {}
            )
        }
    }
}

@Preview(locale = "en")
@Composable
internal fun NetworkItemPreview() {
    MaterialTheme {
        Surface {
            NetworkItem(
                selected = true,
                onClick = {}
            )
        }
    }
}

@Preview(locale = "en")
@Composable
internal fun MockItemSelectedPreview() {
    MaterialTheme {
        Surface {
            MockItem(
                mockResponse = MockResponse.fake().first(),
                selected = true,
                onClick = {}
            )
        }
    }
}
