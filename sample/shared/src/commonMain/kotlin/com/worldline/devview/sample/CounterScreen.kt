package com.worldline.devview.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.worldline.devview.sample.viewmodel.CounterViewModel
import com.worldline.devview.timecapsule.TimeCapsuleEffect

/**
 * Demonstrates the TimeCapsule module end to end: increment the counter, open DevView,
 * restore an earlier count, and see it reflected here immediately.
 *
 * Only composed while [App]'s "Show Counter Screen" toggle is on, so hiding it disposes
 * [CounterViewModel] and demonstrates that TimeCapsule's history resets when the screen
 * leaves composition.
 */
@Composable
internal fun CounterScreen(modifier: Modifier = Modifier) {
    val viewModel = remember { CounterViewModel() }
    val state by viewModel.state.collectAsStateWithLifecycle()

    TimeCapsuleEffect(owner = viewModel, label = { it.label })

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        Button(onClick = viewModel::decrement) { Text(text = "-") }
        Text(text = "Count: ${state.count}")
        Button(onClick = viewModel::increment) { Text(text = "+") }
    }
}
