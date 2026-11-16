package com.worldline.devview.featureflip.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.worldline.devview.featureflip.model.Feature
import com.worldline.devview.featureflip.model.FeatureState
import com.worldline.devview.featureflip.preview.FeaturePreviewParameterProvider

/** Displays a flag with a local switch or labelled remote override choices. */
@Composable
internal fun FeatureItem(
    feature: Feature,
    onStateChange: (FeatureState) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(weight = 1f),
                verticalArrangement = Arrangement.spacedBy(space = 4.dp)
            ) {
                Text(
                    text = feature.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.testTag(tag = "feature_name_${feature.name}")
                )
                feature.description?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag(tag = "feature_description_${feature.name}")
                    )
                }
            }
            if (feature is Feature.LocalFeature) {
                Switch(
                    checked = feature.isEnabled,
                    onCheckedChange = {
                        onStateChange(
                            if (it) FeatureState.LOCAL_ON else FeatureState.LOCAL_OFF
                        )
                    },
                    modifier = Modifier
                        .testTag(tag = "feature_switch_${feature.name}")
                        .semantics { contentDescription = feature.name }
                )
            }
        }
        if (feature is Feature.RemoteFeature) {
            Text(
                text = "Remote value: ${if (feature.defaultRemoteValue) "on" else "off"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                FeatureState.entries.forEachIndexed { index, state ->
                    val label = when (state) {
                        FeatureState.REMOTE -> "Remote"
                        FeatureState.LOCAL_OFF -> "Off"
                        FeatureState.LOCAL_ON -> "On"
                    }
                    SegmentedButton(
                        selected = feature.state == state,
                        onClick = { onStateChange(state) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = FeatureState.entries.size
                        ),
                        modifier = Modifier
                            .testTag(tag = "feature_segment_${feature.name}_${state.name}")
                            .semantics { contentDescription = "${feature.name}: $label" }
                    ) {
                        Text(text = label)
                    }
                }
            }
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
            FeatureItem(feature = feature, onStateChange = {})
        }
    }
}
