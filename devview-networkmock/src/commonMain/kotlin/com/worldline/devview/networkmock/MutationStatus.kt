package com.worldline.devview.networkmock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.worldline.devview.networkmock.viewmodel.NetworkMockMutationState

@Composable
internal fun MutationStatus(
    state: NetworkMockMutationState,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.busy) return
    Column(modifier = modifier.fillMaxWidth().padding(all = 16.dp)) {
        if (state.failure == null) {
            Text(text = "Saving mock setting…", style = MaterialTheme.typography.bodyMedium)
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        } else {
            Text(text = "Mock setting was not saved", style = MaterialTheme.typography.titleSmall)
            Text(text = state.failure, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "Your selection is kept. Retry it or cancel this change.",
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                TextButton(modifier = Modifier.testTag(tag = "mutation_retry"), onClick = onRetry) {
                    Text(text = "Retry")
                }
                TextButton(
                    modifier = Modifier.testTag(tag = "mutation_cancel"),
                    onClick = onCancel
                ) {
                    Text(text = "Cancel change")
                }
            }
        }
    }
}
