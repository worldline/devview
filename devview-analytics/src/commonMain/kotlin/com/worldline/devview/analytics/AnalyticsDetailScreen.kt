package com.worldline.devview.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.worldline.devview.analytics.components.CategoryChip
import com.worldline.devview.analytics.model.AnalyticsLog
import com.worldline.devview.ui.DevViewEmptyState
import com.worldline.devview.ui.DevViewSectionHeader
import com.worldline.devview.ui.rememberCopyToClipboard

@Composable
internal fun AnalyticsDetailScreen(
    log: AnalyticsLog?,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
    onBack: (() -> Unit)? = null
) {
    val copy = rememberCopyToClipboard()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(state = rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp)
    ) {
        if (onBack != null) {
            TextButton(onClick = onBack) { Text(text = "Back to events") }
        }
        if (log == null) {
            DevViewEmptyState(
                icon = Icons.Rounded.Insights,
                title = "Event no longer available",
                message = "It may have been cleared from the event list."
            )
        } else {
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                    Text(text = log.tag, style = MaterialTheme.typography.titleLarge)
                    Text(text = log.screenClass, style = MaterialTheme.typography.bodyMedium)
                    Text(text = log.detailedTimestamp, style = MaterialTheme.typography.bodySmall)
                    CategoryChip(category = log.type.category)
                    DevViewSectionHeader(
                        text = "Parameters",
                        modifier = Modifier.testTag(tag = "event_parameters")
                    )
                    if (log.params.isEmpty()) {
                        Text(
                            text = "No parameters recorded",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    log.params.entries.sortedBy { it.key }.forEach { (key, value) ->
                        Text(text = key, style = MaterialTheme.typography.labelLarge)
                        Text(text = value, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            OutlinedButton(onClick = { copy(log.copyText) }) { Text(text = "Copy event") }
        }
    }
}

internal val AnalyticsLog.copyText: String
    get() = buildString {
        appendLine(value = tag)
        appendLine(value = screenClass)
        appendLine(value = detailedTimestamp)
        appendLine(value = type.displayName)
        params.entries.sortedBy { it.key }.forEach { (key, value) ->
            appendLine(
                value = "$key: $value"
            )
        }
    }.trimEnd()
