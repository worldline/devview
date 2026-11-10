package com.worldline.devview.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.worldline.devview.sample.viewmodel.CounterState
import com.worldline.devview.sample.viewmodel.CounterViewModel
import com.worldline.devview.timecapsule.TimeCapsuleEffect

/**
 * Demonstrates the TimeCapsule module end to end: increment the counter,
 * type a note, open DevView, and restore an earlier count, note, and selection.
 *
 * Only composed while [App]'s "Show Counter Screen" toggle is on, so hiding it disposes
 * [CounterViewModel] and demonstrates that TimeCapsule's history resets when the screen
 * leaves composition.
 */
@Composable
internal fun CounterScreen(modifier: Modifier = Modifier) {
    val viewModel = remember { CounterViewModel() }
    val state by viewModel.state.collectAsStateWithLifecycle()

    TimeCapsuleEffect(
        owner = viewModel,
        textFields = listOf(element = CounterState::note),
        label = { "CounterState(count=${it.count}, label=${it.label})" }
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(
                space = 8.dp,
                alignment = Alignment.CenterHorizontally
            ),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp),
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = viewModel::decrement,
                modifier = Modifier.semantics { contentDescription = "Decrease count" }
            ) { Text(text = "-") }
            Text(text = "Count: ${state.count}")
            FilledTonalButton(
                onClick = viewModel::increment,
                modifier = Modifier.semantics { contentDescription = "Increase count" }
            ) { Text(text = "+") }
        }
        OutlinedTextField(
            state = state.note,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = "Time Capsule note") }
        )
    }
}
