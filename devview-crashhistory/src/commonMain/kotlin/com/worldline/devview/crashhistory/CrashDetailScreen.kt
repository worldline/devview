package com.worldline.devview.crashhistory

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.worldline.devview.crashhistory.components.CrashKindChip
import com.worldline.devview.crashhistory.core.CrashRecord
import com.worldline.devview.crashhistory.core.toJson
import com.worldline.devview.crashhistory.core.toReportText
import kotlinx.collections.immutable.persistentListOf

/**
 * Full details of one crash with Copy, Share and Export buttons.
 *
 * The buttons live in the screen rather than the top bar because top-bar actions have no access to
 * the destination's id or to the platform context needed for sharing.
 *
 * @param record The record to show, or `null` if it was cleared in the meantime.
 * @param bottomPadding Extra space below the content so it clears the host's navigation.
 */
@Composable
internal fun CrashDetailScreen(
    record: CrashRecord?,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0
        .dp
) {
    if (record == null) {
        Text(
            modifier = modifier
                .fillMaxSize()
                .padding(all = 16.dp)
                .testTag(tag = "crash_detail_missing"),
            text = "This record no longer exists",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    val sharer = rememberCrashSharer()
    var shareFailed by remember { mutableStateOf(value = false) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(state = rememberScrollState())
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + bottomPadding),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            OutlinedButton(
                modifier = Modifier.testTag(tag = "crash_copy_button"),
                onClick = { sharer.copy(text = record.toReportText()) }
            ) { Text(text = "Copy") }
            OutlinedButton(
                modifier = Modifier.testTag(tag = "crash_share_button"),
                onClick = { shareFailed = !sharer.shareText(text = record.toReportText()) }
            ) { Text(text = "Share") }
            OutlinedButton(
                modifier = Modifier.testTag(tag = "crash_export_button"),
                onClick = {
                    shareFailed = !sharer.shareFile(
                        fileName = "devview-crash-${record.id}.json",
                        content = persistentListOf(record).toJson()
                    )
                }
            ) { Text(text = "Export") }
        }
        if (shareFailed) {
            Text(text = "Could not share crash report", color = MaterialTheme.colorScheme.error)
        }
        CrashKindChip(record = record)
        Text(
            text = record.exceptionType ?: "Unknown error",
            style = MaterialTheme.typography.titleMedium
        )
        record.message?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
        metadataRows(record = record).forEach { (label, value) ->
            Row(horizontalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                Text(
                    modifier = Modifier.padding(end = 4.dp),
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(text = value, style = MaterialTheme.typography.bodySmall)
            }
        }
        record.stackTrace?.let {
            SelectionContainer {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(state = rememberScrollState())
                        .testTag(tag = "crash_detail_stack_trace"),
                    text = it,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun metadataRows(record: CrashRecord): List<Pair<String, String>> = buildList {
    add(element = "Time" to formatCrashTime(timestampMillis = record.timestampMillis))
    if (!record.isCrash) add(element = "Note" to "Terminated by the system, not a crash")
    record.source?.let { add(element = "Source" to it) }
    record.threadName?.let { add(element = "Thread" to it) }
    record.processId?.let { add(element = "Process" to it.toString()) }
    add(element = "App" to "${record.metadata.appVersion} (${record.metadata.buildNumber})")
    add(
        element = "Device" to
            "${record.metadata.deviceModel}, ${record.metadata.platform} ${record.metadata.osVersion}"
    )
    if (record.truncated) add(element = "Note" to "Truncated to fit the history limits")
}
