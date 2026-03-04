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
 * Displays a searchable, filterable list of feature flags with interactive controls
 * to modify their state. The screen provides a comprehensive interface for managing
 * both local and remote features during development and testing.
 *
 * ## Features
 * - **Search**: Filter features by name using the search bar
 * - **Filters**: Filter by type (Local/Remote) and state (On/Off)
 * - **Local Features**: Simple on/off switch control
 * - **Remote Features**: Tri-state control (Remote/Off/On)
 * - **Persistent State**: All changes are automatically saved using DataStore
 * - **Real-time Updates**: UI updates immediately when feature states change
 *
 * ## Usage
 *
 * ### Basic Usage with FeatureHandler
 * ```kotlin
 * val features = listOf(
 *     Feature.LocalFeature(
 *         name = "dark_mode",
 *         description = "Enable dark theme",
 *         isEnabled = false
 *     ),
 *     Feature.RemoteFeature(
 *         name = "new_feature",
 *         description = "New experimental feature",
 *         defaultRemoteValue = true,
 *         state = FeatureState.REMOTE
 *     )
 * )
 *
 * val featureHandler = rememberFeatureHandler(features)
 * CompositionLocalProvider(LocalFeatureHandler provides featureHandler) {
 *     FeatureFlipScreen()
 * }
 * ```
 *
 * ### Standalone Usage
 * ```kotlin
 * Scaffold { paddingValues ->
 *     FeatureFlipScreen(
 *         modifier = Modifier
 *             .fillMaxSize()
 *             .padding(paddingValues)
 *     )
 * }
 * ```
 *
 * ## UI Components
 * - Search bar with clear button
 * - Filter chips for Local/Remote/On/Off states
 * - Grouped feature cards with adaptive shapes
 * - Switches for local features
 * - Tri-state segmented buttons for remote features
 *
 * @param modifier Modifier to be applied to the root container.
 *
 * @throws IllegalStateException if [LocalFeatureHandler] is not provided in the composition.
 *
 * @see com.worldline.devview.featureflip.model.Feature
 * @see com.worldline.devview.featureflip.model.FeatureHandler
 * @see com.worldline.devview.featureflip.model.LocalFeatureHandler
 * @see com.worldline.devview.featureflip.model.rememberFeatureHandler
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
 *
 * These filters allow users to narrow down the feature list by type
 * (local vs remote) and state (enabled vs disabled).
 */
private enum class FeatureFilter {
    /** Filter to show only local features */
    LOCAL,

    /** Filter to show only remote features */
    REMOTE,

    /** Filter to show only enabled features */
    ON,

    /** Filter to show only disabled features */
    OFF;

    companion object {
        /**
         * Returns the available filter entries based on the feature list composition.
         *
         * If all features are remote features, only ON/OFF filters are shown
         * (since LOCAL/REMOTE filtering would be redundant). Otherwise, all
         * filter types are available.
         *
         * @param features The list of features to analyze
         * @return List of applicable filter options
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

@Preview(locale = "en")
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
