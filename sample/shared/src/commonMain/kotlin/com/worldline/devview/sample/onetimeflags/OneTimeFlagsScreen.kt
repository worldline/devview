package com.worldline.devview.sample.onetimeflags

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.worldline.devview.ui.DevViewEmptyState
import com.worldline.devview.ui.DevViewSectionHeader
import com.worldline.devview.ui.devViewGroupedItem
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okio.IOException

@Composable
internal fun OneTimeFlagsContent(
    dataStore: DataStore<Preferences>,
    bottomPadding: Dp,
    resetError: String?,
    onResetError: (String?) -> Unit
) {
    var readError by remember(key1 = dataStore) { mutableStateOf(value = false) }
    var retry by remember { mutableIntStateOf(value = 0) }
    val flagsFlow = remember(key1 = dataStore, key2 = retry) {
        dataStore.data
            .map { preferences ->
                OneTimeFlag.entries.associateWith { flag -> preferences[flag.key] == true }
            }.catch { error ->
                if (error !is IOException) throw error
                readError = true
            }
    }
    val flags = key(dataStore, retry) { flagsFlow.collectAsState(initial = null).value }
    val scope = rememberCoroutineScope()

    when {
        readError -> DevViewEmptyState(
            icon = Icons.Default.Refresh,
            title = "Could not read flags",
            message = "No values have been changed.",
            action = {
                TextButton(onClick = {
                    readError = false
                    retry++
                }) { Text(text = "Retry") }
            }
        )
        flags == null -> Text(text = "Loading flags…", modifier = Modifier.padding(all = 16.dp))
        else -> OneTimeFlagsList(
            flags = flags,
            bottomPadding = bottomPadding,
            error = resetError,
            onReset = { flag ->
                scope.launch {
                    try {
                        dataStore.edit { preferences -> preferences.remove(key = flag.key) }
                        onResetError(null)
                    } catch (_: IOException) {
                        onResetError("Could not reset ${flag.description}. Try again.")
                    }
                }
            }
        )
    }
}

@Composable
private fun OneTimeFlagsList(
    flags: Map<OneTimeFlag, Boolean>,
    bottomPadding: Dp,
    error: String?,
    onReset: (OneTimeFlag) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 16.dp,
            end = 16.dp,
            bottom = bottomPadding + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(space = 4.dp)
    ) {
        item {
            Text(
                text = "Reset a flag to try its first-time experience again. Other app preferences stay untouched.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
        item { DevViewSectionHeader(text = "First-time experiences") }
        if (error != null) {
            item { Text(text = error, color = MaterialTheme.colorScheme.error) }
        }
        itemsIndexed(items = OneTimeFlag.entries, key = { _, flag -> flag.name }) { index, flag ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .devViewGroupedItem(
                        index = index,
                        count = OneTimeFlag.entries.size
                    ).padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(weight = 1f),
                    verticalArrangement = Arrangement.spacedBy(space = 4.dp)
                ) {
                    Text(text = flag.description, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (flags[flag] == true) "Seen" else "Not seen",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = flag.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = { onReset(flag) },
                    modifier = Modifier.semantics {
                        contentDescription = "Reset ${flag.description}"
                    }
                ) { Text(text = "Reset") }
            }
        }
    }
}
