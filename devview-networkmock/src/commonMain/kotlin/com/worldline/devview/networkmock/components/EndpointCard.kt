package com.worldline.devview.networkmock.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.worldline.devview.networkmock.model.AvailableEndpointMock

/**
 * Card component for displaying and configuring a single API endpoint mock.
 *
 * Shows the endpoint details, a toggle for enabling/disabling the mock,
 * and a dropdown to select which mock response to return.
 *
 * @param endpoint The endpoint mock data
 * @param onToggleMock Callback when mock is enabled/disabled
 * @param onSelectResponse Callback when a response is selected
 * @param modifier Optional modifier
 */
@Composable
public fun EndpointCard(
    endpoint: AvailableEndpointMock,
    onToggleMock: (Boolean) -> Unit,
    onSelectResponse: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDropdown by remember { mutableStateOf(value = false) }

    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp)
        ) {
            // Endpoint name and method/path
            Text(
                text = endpoint.config.name,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(height = 4.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = endpoint.config.method,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = endpoint.config.path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(height = 12.dp))

            // Mock toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (endpoint.currentState.mockEnabled) "Use Mock" else "Use Network",
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = endpoint.currentState.mockEnabled,
                    onCheckedChange = onToggleMock
                )
            }

            // Response selector (only when mock is enabled)
            if (endpoint.currentState.mockEnabled && endpoint.availableResponses.isNotEmpty()) {
                Spacer(modifier = Modifier.height(height = 12.dp))

                Column {
                    Text(
                        text = "Mock Response:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(height = 4.dp))

                    OutlinedButton(
                        onClick = { showDropdown = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = endpoint.currentState.selectedResponseFile
                                    ?: "Select response...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select response"
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showDropdown,
                        onDismissRequest = { showDropdown = false }
                    ) {
                        endpoint.availableResponses.forEach { response ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = response.displayName,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = response.fileName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    onSelectResponse(response.fileName)
                                    showDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            // Show message if no responses available
            if (endpoint.currentState.mockEnabled && endpoint.availableResponses.isEmpty()) {
                Spacer(modifier = Modifier.height(height = 8.dp))
                Text(
                    text = "No mock responses available for this endpoint",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
