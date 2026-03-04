package com.worldline.devview.featureflip.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.worldline.devview.featureflip.model.Feature
import com.worldline.devview.featureflip.model.Feature.LocalFeature
import com.worldline.devview.featureflip.model.Feature.RemoteFeature
import com.worldline.devview.featureflip.model.FeatureState
import com.worldline.devview.featureflip.preview.FeaturePreviewParameterProvider

/**
 * Adds feature items to a LazyList with optimized performance.
 *
 * Creates a visually grouped list of feature cards with appropriate styling, dividers,
 * and animations. Each feature is rendered with:
 * - Stable item keys for efficient recomposition
 * - Content type hints for recycling optimization
 * - Item animations for smooth transitions
 * - Proper spacing and dividers
 *
 * The cards are styled to appear as a cohesive group with connected corners between
 * adjacent items (first item has bottom corners squared, middle items have all corners
 * squared, last item has top corners squared).
 *
 * ## Usage
 * ```kotlin
 * LazyColumn {
 *     featureItems(
 *         features = myFeatureList,
 *         onStateChange = { featureName, newState ->
 *             // Handle state change
 *             featureHandler.setFeatureState(featureName, newState)
 *         }
 *     )
 * }
 * ```
 *
 * @param features List of features to display in the list.
 * @param onStateChange Callback invoked when a feature's state changes.
 *        Receives the feature name and the new [FeatureState].
 * @param modifier Modifier to apply to each feature item card.
 *
 * @see FeatureItem
 * @see com.worldline.devview.featureflip.model.Feature
 * @see com.worldline.devview.featureflip.model.FeatureState
 */
internal fun LazyListScope.featureItems(
    features: List<Feature>,
    onStateChange: (String, FeatureState) -> Unit,
    modifier: Modifier = Modifier
) {
    features.forEachIndexed { index, feature ->
        item(
            key = feature.name,
            contentType = feature::class.simpleName
        ) {
            FeatureItem(
                modifier = modifier
                    .animateItem(),
                feature = feature,
                totalFeatures = features.size,
                index = index,
                isLastIndex = index == features.lastIndex,
                onStateChange = { state ->
                    onStateChange(feature.name, state)
                }
            )
        }
    }
    item {
        Spacer(
            modifier = Modifier.padding(all = 16.dp)
        )
    }
}

/**
 * Displays a single feature item card with interactive controls.
 *
 * Renders a Material Design card containing the feature's information and
 * appropriate control widget based on the feature type:
 * - [Feature.LocalFeature]: Displays a Material [Switch] for simple on/off toggling
 * - [Feature.RemoteFeature]: Displays a [FeatureTriStateSwitch] for Remote/Off/On selection
 *
 * The card's shape automatically adapts based on its position in a list to create
 * a visually connected group of items:
 * - First item: Rounded top corners, squared bottom corners
 * - Middle items: All corners squared
 * - Last item: Squared top corners, rounded bottom corners
 * - Single item: All corners rounded
 *
 * ## Layout Structure
 * ```
 * Card
 * └── Row
 *     ├── Column (feature info, weighted)
 *     │   ├── Text (feature name, bold)
 *     │   └── Text (description, optional)
 *     └── Control Widget
 *         ├── Switch (for LocalFeature)
 *         └── FeatureTriStateSwitch (for RemoteFeature)
 * └── HorizontalDivider (if not last item)
 * ```
 *
 * @param feature The feature to display.
 * @param totalFeatures Total number of features in the list (used for shape calculations).
 * @param index The zero-based index of this feature in the list.
 * @param isLastIndex Whether this is the last item in the list.
 * @param onStateChange Callback invoked when the feature's state changes via user interaction.
 * @param modifier Modifier to apply to the card container.
 *
 * @see Feature.LocalFeature
 * @see Feature.RemoteFeature
 * @see FeatureTriStateSwitch
 * @see com.worldline.devview.featureflip.model.FeatureState
 */
@Composable
private fun FeatureItem(
    feature: Feature,
    totalFeatures: Int,
    index: Int,
    isLastIndex: Boolean,
    onStateChange: (FeatureState) -> Unit,
    modifier: Modifier = Modifier
) {
    val baseShape = MaterialTheme.shapes.medium

    val shape = when (totalFeatures) {
        1 -> baseShape
        else -> when (index) {
            0 -> baseShape.copy(
                bottomStart = ZeroCornerSize,
                bottomEnd = ZeroCornerSize
            )

            totalFeatures - 1 -> baseShape.copy(
                topStart = ZeroCornerSize,
                topEnd = ZeroCornerSize
            )

            else -> RoundedCornerShape(percent = 0)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = shape
    ) {
        Row(
            modifier = Modifier
                .padding(all = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(weight = 1f),
                verticalArrangement = Arrangement.spacedBy(space = 4.dp)
            ) {
                Text(
                    text = feature.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                feature.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            when (feature) {
                is LocalFeature -> Switch(
                    checked = feature.isEnabled,
                    onCheckedChange = {
                        onStateChange(if (it) FeatureState.LOCAL_ON else FeatureState.LOCAL_OFF)
                    }
                )

                is RemoteFeature -> FeatureTriStateSwitch(
                    feature = feature,
                    onStateChange = onStateChange
                )
            }
        }
        if (!isLastIndex) {
            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
        }
    }
}

@Preview(locale = "en")
@Composable
private fun FeatureItemPreview(
    @PreviewParameter(provider = FeaturePreviewParameterProvider::class) feature: Feature
) {
    MaterialTheme {
        Surface {
            LazyColumn {
                featureItems(
                    features = listOf(element = feature),
                    onStateChange = { _, _ -> }
                )
            }
        }
    }
}
