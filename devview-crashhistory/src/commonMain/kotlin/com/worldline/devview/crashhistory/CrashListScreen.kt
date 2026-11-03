package com.worldline.devview.crashhistory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.worldline.devview.crashhistory.components.CrashRecordItem
import com.worldline.devview.crashhistory.core.CrashRecord
import com.worldline.devview.crashhistory.core.toJson

private const val EXPORT_ALL_FILE_NAME = "devview-crashes.json"

/**
 * Lists the recorded crashes, newest first, with an "Export all" button.
 *
 * Stateless so it can be tested without the recorder singleton.
 *
 * @param records Records to show.
 * @param onOpen Called with a record's id when its row is tapped.
 * @param bottomPadding Extra space below the list so the last row clears the host's navigation.
 */
@Composable
internal fun CrashListScreen(
    records: List<CrashRecord>,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp
) {
    if (records.isEmpty()) {
        Text(
            modifier = modifier
                .fillMaxSize()
                .padding(all = 16.dp)
                .testTag(tag = "crash_empty_state"),
            text = "Crashes will appear here",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    val sharer = rememberCrashSharer()
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(space = 0.dp)
    ) {
        item(key = "export_all") {
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag(tag = "crash_export_all_button"),
                onClick = {
                    sharer.shareFile(fileName = EXPORT_ALL_FILE_NAME, content = records.toJson())
                }
            ) {
                Text(text = "Export all (${records.size})")
            }
        }
        items(items = records, key = { it.id }) { record ->
            CrashRecordItem(
                modifier = Modifier.testTag(tag = "crash_item_${record.id}"),
                record = record,
                onClick = { onOpen(record.id) }
            )
            HorizontalDivider()
        }
    }
}
