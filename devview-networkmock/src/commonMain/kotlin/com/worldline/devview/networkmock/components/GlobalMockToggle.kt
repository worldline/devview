package com.worldline.devview.networkmock.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Global mock toggle card component.
 *
 * Displays a prominent toggle switch for enabling/disabling all network mocking
 * globally. When disabled, all requests use actual network regardless of individual
 * endpoint settings.
 *
 * @param enabled Whether global mocking is currently enabled
 * @param onToggle Callback when the toggle is switched
 * @param modifier Optional modifier for the card
 */
@Composable
public fun GlobalMockToggle(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp),
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
                    text = if (enabled) {
                        "Mock responses enabled - network calls will be intercepted"
                    } else {
                        "Mocking disabled - all requests use actual network"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = enabled,
                onCheckedChange = onToggle
            )
        }
    }
}
