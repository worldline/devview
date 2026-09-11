package com.worldline.devview.networkmock.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.worldline.devview.utils.preview.BooleanPreviewParameterProvider

/**
 * Global mock toggle row, showing a switch for enabling/disabling all network mocking
 * alongside a count of currently mocked operations.
 *
 * When disabled, all requests use actual network regardless of individual endpoint
 * settings. [mockedCount] and [totalCount] are computed across every spec, not just the
 * one currently visible in the tab row — this is the at-a-glance answer to "what have I
 * left mocked".
 *
 * @param enabled Whether global mocking is currently enabled
 * @param mockedCount Number of operations currently in [com.worldline.devview.networkmock.core.model.OperationMockState.Mock]
 * @param totalCount Total number of operations across every spec
 * @param onToggle Callback when the toggle is switched
 * @param modifier Optional modifier for the row
 */
@Composable
internal fun GlobalMockToggle(
    enabled: Boolean,
    mockedCount: Int,
    totalCount: Int,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(weight = 1f)
        ) {
            Text(
                text = "Global Mocking",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                modifier = Modifier.testTag(tag = "global_mock_toggle_count"),
                text = "$mockedCount of $totalCount mocked",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            modifier = Modifier.testTag(tag = "global_mock_toggle_switch"),
            checked = enabled,
            onCheckedChange = onToggle
        )
    }
}

@Preview(locale = "en")
@Composable
private fun GlobalMockToggleEnabledPreview(
    @PreviewParameter(BooleanPreviewParameterProvider::class) enabled: Boolean
) {
    MaterialTheme {
        Surface {
            GlobalMockToggle(
                enabled = enabled,
                mockedCount = 3,
                totalCount = 47,
                onToggle = {}
            )
        }
    }
}
