package com.worldline.devview.consolelogger.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.worldline.devview.consolelogger.ConsoleNoiseChange
import com.worldline.devview.consolelogger.ConsoleNoiseOverrides
import com.worldline.devview.consolelogger.consoleNoiseKey
import com.worldline.devview.ui.DevViewSectionHeader

@Composable
internal fun ConsoleNoiseFilterPanel(
    defaults: List<String>,
    overrides: ConsoleNoiseOverrides,
    onChange: (ConsoleNoiseChange) -> Unit,
    hiddenEntries: Int,
    controlsEnabled: Boolean,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit
) {
    var draft by rememberSaveable { mutableStateOf(value = "") }
    val patterns = overrides.patterns(defaults = defaults)
    val duplicate = patterns.any { it.equals(other = draft.trim(), ignoreCase = true) }
    DevViewSectionHeader(text = "Noise filters")
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        Text(
            text = "Enabled rules discard incoming matches before buffering. " +
                "Turning a rule off only admits future entries.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$hiddenEntries earlier entries hidden. Matches tag or message, ignoring case.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (loading) {
            Text(text = "Loading saved noise filters…", style = MaterialTheme.typography.bodySmall)
        }
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            TextButton(onClick = onRetry) { Text(text = "Retry noise filters") }
        }
        patterns.forEach { pattern ->
            Row(
                modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = pattern.consoleNoiseKey() !in overrides.disabled,
                    enabled = controlsEnabled,
                    onCheckedChange = {
                        onChange(
                            ConsoleNoiseChange.Enable(pattern = pattern, enabled = it)
                        )
                    },
                    modifier = Modifier
                        .semantics { contentDescription = "Exclude entries matching $pattern" }
                        .testTag(tag = "console_noise_toggle_${pattern.consoleNoiseKey()}")
                )
                Text(
                    text = pattern,
                    modifier = Modifier.weight(weight = 1f),
                    style = MaterialTheme.typography.bodyMedium
                )
                IconButton(
                    enabled = controlsEnabled,
                    onClick = { onChange(ConsoleNoiseChange.Remove(pattern = pattern)) }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Remove exclusion: $pattern"
                    )
                }
            }
        }
        if (patterns.isEmpty() && !loading) {
            Text(text = "No noise exclusions", style = MaterialTheme.typography.bodySmall)
        }
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            label = { Text(text = "Exclude tag or message containing") },
            singleLine = true,
            enabled = controlsEnabled,
            isError = duplicate,
            supportingText = if (duplicate) {
                { Text(text = "This pattern already exists") }
            } else {
                null
            },
            modifier = Modifier.fillMaxWidth().testTag(tag = "console_noise_pattern_input")
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                enabled = controlsEnabled,
                onClick = { onChange(ConsoleNoiseChange.Reset) }
            ) { Text(text = "Reset to defaults") }
            TextButton(
                enabled = controlsEnabled && draft.isNotBlank() && !duplicate,
                onClick = {
                    onChange(ConsoleNoiseChange.Add(pattern = draft.trim()))
                    draft = ""
                }
            ) { Text(text = "Add exclusion") }
        }
    }
}
