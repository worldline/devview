package com.worldline.devview.featureflip.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.worldline.devview.featureflip.model.Feature
import com.worldline.devview.featureflip.model.Feature.LocalFeature
import com.worldline.devview.featureflip.model.Feature.RemoteFeature
import com.worldline.devview.featureflip.model.FeatureState

/**
 * Preview parameter provider for [Feature] instances.
 *
 * Provides various states of both [Feature.LocalFeature] and [Feature.RemoteFeature]
 * for Compose preview purposes.
 */
internal class FeaturePreviewParameterProvider : PreviewParameterProvider<Feature> {
    override val values: Sequence<Feature>
        get() = sequenceOf(
            RemoteFeature(
                name = "Remote Feature Enabled",
                description = "Remote feature with default value enabled",
                defaultRemoteValue = true,
                state = FeatureState.REMOTE
            ),
            RemoteFeature(
                name = "Remote Feature Disabled",
                description = "Remote feature with default value disabled",
                defaultRemoteValue = false,
                state = FeatureState.REMOTE
            ),
            RemoteFeature(
                name = "Remote Feature Local Off",
                description = "Remote feature overridden locally to OFF",
                defaultRemoteValue = false,
                state = FeatureState.LOCAL_OFF
            ),
            RemoteFeature(
                name = "Remote Feature Local On",
                description = "Remote feature overridden locally to ON",
                defaultRemoteValue = true,
                state = FeatureState.LOCAL_ON
            ),
            LocalFeature(
                name = "Local Feature Enabled",
                description = "Local feature that is enabled",
                isEnabled = true
            ),
            LocalFeature(
                name = "Local Feature Disabled",
                description = "Local feature that is disabled",
                isEnabled = false
            )
        )
}

/**
 * Preview parameter provider specifically for [Feature.RemoteFeature] instances.
 *
 * Provides remote features in all possible states (REMOTE, LOCAL_OFF, LOCAL_ON)
 * with both enabled and disabled default remote values.
 */
internal class RemoteFeaturePreviewParameterProvider : PreviewParameterProvider<RemoteFeature> {
    override val values: Sequence<RemoteFeature>
        get() = sequenceOf(
            RemoteFeature(
                name = "Remote Feature Enabled",
                description = "Remote feature with default value enabled",
                defaultRemoteValue = true,
                state = FeatureState.REMOTE
            ),
            RemoteFeature(
                name = "Remote Feature Disabled",
                description = "Remote feature with default value disabled",
                defaultRemoteValue = false,
                state = FeatureState.REMOTE
            ),
            RemoteFeature(
                name = "Remote Feature Local Off",
                description = "Remote feature overridden locally to OFF",
                defaultRemoteValue = false,
                state = FeatureState.LOCAL_OFF
            ),
            RemoteFeature(
                name = "Remote Feature Local On",
                description = "Remote feature overridden locally to ON",
                defaultRemoteValue = true,
                state = FeatureState.LOCAL_ON
            )
        )
}

/**
 * Preview parameter provider specifically for [Feature.LocalFeature] instances.
 *
 * Provides local features in both enabled and disabled states for previews.
 */
internal class LocalFeaturePreviewParameterProvider : PreviewParameterProvider<LocalFeature> {
    override val values: Sequence<LocalFeature>
        get() = sequenceOf(
            LocalFeature(
                name = "Local Feature Enabled",
                description = "Local feature that is enabled",
                isEnabled = true
            ),
            LocalFeature(
                name = "Local Feature Disabled",
                description = "Local feature that is disabled",
                isEnabled = false
            )
        )
}
