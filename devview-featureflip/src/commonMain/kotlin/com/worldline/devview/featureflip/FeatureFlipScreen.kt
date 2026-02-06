package com.worldline.devview.featureflip

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.worldline.devview.featureflip.components.featureItems
import com.worldline.devview.featureflip.model.Feature
import com.worldline.devview.featureflip.model.LocalFeatureHandler
import com.worldline.devview.featureflip.model.rememberFeatureHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

/**
 * Main screen for managing feature flags.
 *
 * Displays a searchable, filterable list of feature flags with controls to modify their state.
 * Features can be filtered by type (local/remote) and state (on/off), and searched by name.
 *
 * @param modifier Modifier to be applied to the root container
 */
@Composable
public fun FeatureFlipScreen(modifier: Modifier = Modifier) {
    val featureHandler = LocalFeatureHandler.current
    val features by featureHandler.features

    @Suppress("InjectDispatcher")
    val coroutineScope = rememberCoroutineScope(getContext = { Dispatchers.IO })

    var query by remember { mutableStateOf(value = TextFieldValue()) }

    val selectedFilters = remember {
        FeatureFilter
            .availableEntries(features = features)
            .map { it to false }
            .toMutableStateMap()
    }

    val filteredFeatures by remember(key1 = query, key2 = selectedFilters, key3 = features) {
        derivedStateOf {
            when {
                query.text.isBlank() && selectedFilters.values.all { !it } -> features

                selectedFilters.values.all { !it } -> features.filter { feature ->
                    feature.name.contains(other = query.text, ignoreCase = true)
                }

                query.text.isBlank() -> features.filter { feature ->
                    selectedFilters.filter { it.value }.all { (state, selected) ->
                        when (state) {
                            FeatureFilter.LOCAL -> feature is Feature.LocalFeature
                            FeatureFilter.REMOTE -> feature is Feature.RemoteFeature
                            FeatureFilter.ON -> feature.isEnabled
                            FeatureFilter.OFF -> !feature.isEnabled
                        } && selected
                    }
                }

                else -> features.filter { feature ->
                    feature.name.contains(
                        other = query.text,
                        ignoreCase = true
                    ) && selectedFilters.filter { it.value }.all { (state, selected) ->
                        when (state) {
                            FeatureFilter.LOCAL -> feature is Feature.LocalFeature
                            FeatureFilter.REMOTE -> feature is Feature.RemoteFeature
                            FeatureFilter.ON -> feature.isEnabled
                            FeatureFilter.OFF -> !feature.isEnabled
                        } && selected
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = query,
            onValueChange = {
                query = it
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search"
                )
            },
            trailingIcon = {
                IconButton(
                    onClick = {
                        query = TextFieldValue()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Reset"
                    )
                }
            }
        )

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
        ) {
            selectedFilters
                .entries
                .sortedBy { it.key.ordinal } // .toSortedMap doesn't exist in KMP
                .forEach { (state, selected) ->
                    FilterChip(
                        selected = selected,
                        label = {
                            Text(
                                text = when (state) {
                                    FeatureFilter.LOCAL -> "Local"
                                    FeatureFilter.REMOTE -> "Remote"
                                    FeatureFilter.ON -> "On"
                                    FeatureFilter.OFF -> "Off"
                                }
                            )
                        },
                        onClick = {
                            selectedFilters[state] = !selected
                        },
                        leadingIcon = {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.Done,
                                    contentDescription = null
                                )
                            }
                        }
                    )
                }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            featureItems(
                features = filteredFeatures,
                onStateChange = { featureName, state ->
                    coroutineScope.launch {
                        featureHandler.setFeatureState(
                            featureName = featureName,
                            state = state
                        )
                    }
                }
            )
        }
    }
}

/**
 * Internal enum representing the available filter options for features.
 */
private enum class FeatureFilter {
    /** Filter for local features */
    LOCAL,

    /** Filter for remote features */
    REMOTE,

    /** Filter for enabled features */
    ON,

    /** Filter for disabled features */
    OFF;

    companion object {
        /**
         * Returns the available filter entries based on the feature list.
         * If all features are remote, only ON/OFF filters are shown.
         * Otherwise, all filter types are available.
         */
        fun availableEntries(features: List<Feature>): List<FeatureFilter> =
            if (features.filterIsInstance<Feature.RemoteFeature>().size == features.size) {
                listOf(
                    ON,
                    OFF
                )
            } else {
                entries
            }
    }
}

@Preview
@Composable
private fun FeaturesScreenPreview() {
    val featureHandler = rememberFeatureHandler(
        features = Feature.fake()
    )
    CompositionLocalProvider(value = LocalFeatureHandler provides featureHandler) {
        MaterialTheme {
            Scaffold {
                FeatureFlipScreen()
            }
        }
    }
}
