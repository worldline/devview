package com.worldline.devview.featureflip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.worldline.devview.featureflip.components.FeatureItem
import com.worldline.devview.featureflip.model.Feature
import com.worldline.devview.featureflip.model.FeatureHandler
import com.worldline.devview.featureflip.model.FeatureState
import com.worldline.devview.featureflip.model.LocalFeatureHandler
import com.worldline.devview.ui.DevViewEmptyState
import com.worldline.devview.ui.DevViewFilterBar
import com.worldline.devview.ui.DevViewSectionHeader
import com.worldline.devview.utils.rememberDataStore
import kotlinx.coroutines.channels.Channel
import okio.IOException

/**
 * Searchable feature-flag list with local switches and Remote/Off/On override choices.
 * Changes are persisted through [FeatureHandler]; query, filters and panel visibility
 * survive saved-state restoration. Filters combine OR within a dimension and AND across dimensions.
 *
 * Rendered by [FeatureFlip] inside DevView; the host provides [LocalFeatureHandler].
 *
 * @param modifier Modifier applied to the screen.
 * @param bottomPadding Bottom system inset supplied by DevView's destination scaffold.
 * @throws IllegalStateException if [LocalFeatureHandler] is not provided.
 */
@Composable
internal fun FeatureFlipScreen(modifier: Modifier = Modifier, bottomPadding: Dp = 0.dp) {
    val featureHandler = LocalFeatureHandler.current
    val features = featureHandler.features.value

    val changes = remember(
        key1 = featureHandler
    ) { Channel<FeatureChange>(capacity = Channel.UNLIMITED) }
    val latestChanges = remember(key1 = featureHandler) { mutableMapOf<String, FeatureChange>() }
    var failedChanges by remember(key1 = featureHandler) {
        mutableStateOf(value = emptyMap<String, FeatureChange>())
    }
    val submitChange: (String, FeatureState) -> Unit = { name, state ->
        val revision = latestChanges[name]?.revision ?: 0L
        check(value = revision < Long.MAX_VALUE) { "Feature change revisions exhausted" }
        val change = FeatureChange(name = name, state = state, revision = revision + 1)
        latestChanges[name] = change
        failedChanges = failedChanges - name
        changes.trySend(element = change)
    }
    // One consumer preserves click order even when a DataStore write suspends.
    LaunchedEffect(key1 = changes) {
        for (change in changes) {
            try {
                featureHandler.setFeatureState(featureName = change.name, state = change.state)
                if (latestChanges[change.name] ==
                    change
                ) {
                    failedChanges = failedChanges - change.name
                }
            } catch (_: IOException) {
                if (latestChanges[change.name] == change) {
                    failedChanges = failedChanges + (change.name to change)
                }
            }
        }
    }
    var query by rememberSaveable { mutableStateOf(value = "") }
    var expanded by rememberSaveable { mutableStateOf(value = false) }
    var selectedFilters by rememberSaveable(
        stateSaver = listSaver<List<String>, String>(save = { it }, restore = { it })
    ) { mutableStateOf(value = emptyList<String>()) }
    val availableFilters = FeatureFilter.entries.filter {
        it in FeatureFilter.availableEntries(features = features) || it.name in selectedFilters
    }
    val activeFilters = availableFilters.filter { it.name in selectedFilters }
    val types = activeFilters.filter { it == FeatureFilter.LOCAL || it == FeatureFilter.REMOTE }
    val states = activeFilters.filter { it == FeatureFilter.ON || it == FeatureFilter.OFF }
    val filteredFeatures = features.filter { feature ->
        val matchesQuery = query.isBlank() || feature.name.contains(
            other = query,
            ignoreCase = true
        )
        val matchesType = types.isEmpty() || types.any {
            it == FeatureFilter.LOCAL && feature is Feature.LocalFeature ||
                it == FeatureFilter.REMOTE && feature is Feature.RemoteFeature
        }
        val matchesState = states.isEmpty() || states.any {
            it == FeatureFilter.ON && feature.isEnabled ||
                it == FeatureFilter.OFF && !feature.isEnabled
        }
        matchesQuery && matchesType && matchesState
    }

    Scaffold(
        modifier = modifier.fillMaxSize().imePadding(),
        bottomBar = {
            DevViewFilterBar(
                query = query,
                onQueryChange = { query = it },
                placeholder = "Filter features",
                activeFilterCount = activeFilters.size,
                expanded = expanded,
                onExpandedChange = { expanded = it },
                onClearFilters = { selectedFilters = emptyList() },
                bottomPadding = bottomPadding
            ) {
                listOf(
                    "Type" to
                        availableFilters.filter {
                            it == FeatureFilter.LOCAL ||
                                it == FeatureFilter.REMOTE
                        },
                    "State" to
                        availableFilters.filter {
                            it == FeatureFilter.ON || it == FeatureFilter.OFF
                        }
                ).forEach { (title, filters) ->
                    if (filters.isNotEmpty()) {
                        DevViewSectionHeader(text = title)
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
                        ) {
                            items(items = filters, key = { it.name }) { filter ->
                                val selected = filter in activeFilters
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        selectedFilters = if (selected) {
                                            selectedFilters - filter.name
                                        } else {
                                            selectedFilters + filter.name
                                        }
                                    },
                                    label = { Text(text = filter.label) },
                                    modifier = Modifier.testTag(
                                        tag = "feature_filter_chip_${filter.name}"
                                    ),
                                    leadingIcon = if (selected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Rounded.Done,
                                                contentDescription = null
                                            )
                                        }
                                    } else {
                                        null
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    paddingValues = paddingValues
                ).testTag(tag = "feature_list")
        ) {
            items(
                items = failedChanges.entries.toList(),
                key = { "failure:${it.key}" },
                contentType = { "failure" }
            ) { (name, change) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp
                        ).testTag(tag = "feature_failure_$name"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Could not save $name. The previous value is unchanged.",
                        modifier = Modifier.weight(weight = 1f),
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(onClick = {
                        if (failedChanges[name] == change && latestChanges[name] == change) {
                            failedChanges = failedChanges - name
                            changes.trySend(element = change)
                        }
                    }) { Text(text = "Retry") }
                }
            }
            if (filteredFeatures.isEmpty()) {
                item(key = "empty", contentType = "empty") {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        DevViewEmptyState(
                            icon = Icons.Rounded.Lightbulb,
                            title = if (features.isEmpty()) {
                                "No features registered"
                            } else {
                                "No features match your filters"
                            },
                            message = if (features.isEmpty()) {
                                "Register flags with FeatureHandler."
                            } else {
                                "Change your search or clear the filters."
                            },
                            action = if (features.isNotEmpty()) {
                                {
                                    TextButton(onClick = {
                                        query = ""
                                        selectedFilters = emptyList()
                                    }) {
                                        Text(text = "Clear search and filters")
                                    }
                                }
                            } else {
                                null
                            }
                        )
                    }
                }
            } else {
                itemsIndexed(
                    items = filteredFeatures,
                    key = { _, feature -> "feature:${feature.name}" },
                    contentType = { _, feature -> feature::class.simpleName }
                ) { index, feature ->
                    Column(
                        modifier = Modifier.animateItem().testTag(
                            tag = "feature_item_${feature.name}"
                        )
                    ) {
                        FeatureItem(
                            feature = feature,
                            onStateChange = { state ->
                                submitChange(feature.name, state)
                            }
                        )
                        if (index != filteredFeatures.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

private data class FeatureChange(val name: String, val state: FeatureState, val revision: Long)

private enum class FeatureFilter(val label: String) {
    LOCAL("Local"),
    REMOTE("Remote"),
    ON("On"),
    OFF("Off");

    companion object {
        fun availableEntries(features: List<Feature>): List<FeatureFilter> =
            if (features.all { it is Feature.RemoteFeature } ||
                features.all { it is Feature.LocalFeature }
            ) {
                listOf(ON, OFF)
            } else {
                entries
            }
    }
}

@Preview(locale = "en")
@Composable
private fun FeaturesScreenPreview() {
    val dataStore = rememberDataStore(dataStoreName = "preview_datastore")
    val handler = remember(key1 = dataStore) {
        FeatureHandler(dataStore = dataStore, initialFeatures = Feature.fake())
    }
    CompositionLocalProvider(value = LocalFeatureHandler provides handler) {
        MaterialTheme {
            FeatureFlipScreen()
        }
    }
}
