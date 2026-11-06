package com.worldline.devview.consolelogger.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.worldline.devview.consolelogger.copyText
import com.worldline.devview.consolelogger.displayName
import com.worldline.devview.consolelogger.model.ConsoleLog
import com.worldline.devview.consolelogger.model.LogLevel
import com.worldline.devview.consolelogger.theme.LogColorScheme
import com.worldline.devview.ui.rememberCopyToClipboard

@Composable
internal fun ConsoleLogRow(
    log: ConsoleLog,
    colors: LogColorScheme,
    expanded: Boolean,
    onExpandedChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    val copy = rememberCopyToClipboard()
    val chevronRotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f)
    val toggleLabel = if (expanded) "Collapse log" else "Expand log"
    val toggle = Modifier
        .clickable(
            role = Role.Button,
            onClickLabel = toggleLabel,
            onClick = onExpandedChange
        ).semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                other = if (expanded) Modifier else toggle
            ).padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .then(other = if (expanded) toggle else Modifier),
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text(
                    text = if (log.level == LogLevel.UNKNOWN) "?" else log.level.name.take(n = 1),
                    color = colors[log.level].label,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .semantics { contentDescription = log.level.displayName }
                )
            }
            Text(
                text = log.formattedTimestamp,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = log.tag.ifEmpty { "Untagged" },
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(weight = 1f)
            )
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.rotate(degrees = chevronRotation)
            )
        }
        if (expanded) {
            SelectionContainer {
                Text(
                    text = log.message,
                    softWrap = false,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(state = rememberScrollState())
                        .testTag(tag = "console_expanded_message_${log.id}")
                )
            }
            TextButton(onClick = { copy(log.copyText()) }) { Text(text = "Copy log") }
        } else {
            Text(
                text = log.message
                    .lineSequence()
                    .firstOrNull()
                    .orEmpty(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
