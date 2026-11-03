package com.worldline.devview.crashhistory.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.worldline.devview.crashhistory.core.CrashRecord
import com.worldline.devview.crashhistory.core.CrashRecordKind
import com.worldline.devview.crashhistory.kindLabel

/**
 * Small coloured label for a record's kind. Red = confirmed crash, tertiary = reported by the app
 * or the OS, neutral = the system killed the app (not necessarily a crash).
 */
@Composable
internal fun CrashKindChip(record: CrashRecord, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when {
        record.kind == CrashRecordKind.UNCAUGHT_EXCEPTION ||
            record.kind == CrashRecordKind.SYSTEM_REPORTED && record.isCrash ->
            colors.errorContainer to colors.onErrorContainer
        record.kind == CrashRecordKind.APP_REPORTED ->
            colors.tertiaryContainer to
                colors.onTertiaryContainer
        else -> colors.surfaceVariant to colors.onSurfaceVariant
    }
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = container,
        contentColor = content
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            text = record.kindLabel,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
