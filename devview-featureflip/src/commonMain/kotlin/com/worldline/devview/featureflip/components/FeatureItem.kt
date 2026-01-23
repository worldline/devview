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
 * Adds feature items to a LazyList.
 *
 * Creates a list of feature cards with appropriate styling and dividers.
 * Each feature is rendered with animation support and proper item keys for efficient recomposition.
 *
 * @param features List of features to display
 * @param onStateChange Callback invoked when a feature's state changes.
 *        Parameters are the feature name and the new state.
 * @param modifier Modifier to apply to each feature item
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
 * Displays a single feature item card.
 *
 * Renders a feature with its name, description, and appropriate control:
 * - [Feature.LocalFeature] shows a simple on/off Switch
 * - [Feature.RemoteFeature] shows a tri-state switch (Remote/Off/On)
 *
 * The card shape adapts based on its position in a list to create a grouped appearance.
 *
 * @param feature The feature to display
 * @param totalFeatures Total number of features in the list (for shape calculations)
 * @param index The index of this feature in the list
 * @param isLastIndex Whether this is the last item in the list
 * @param onStateChange Callback invoked when the feature's state changes
 * @param modifier Modifier to apply to the card
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

@Preview
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
