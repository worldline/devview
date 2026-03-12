package com.worldline.devview.featureflip.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.worldline.devview.featureflip.model.Feature
import com.worldline.devview.featureflip.model.Feature.LocalFeature
import com.worldline.devview.featureflip.model.Feature.RemoteFeature
import com.worldline.devview.featureflip.model.FeatureState

/**
 * Preview parameter provider for [Feature] instances.
 *
 * Provides sample instances of both [Feature.LocalFeature] and [Feature.RemoteFeature]
 * in various states for Compose preview purposes. This allows developers to visualize
 * how different feature configurations appear in the UI during development.
 *
 * ## Provided Samples
 * - Remote features with enabled/disabled default values
 * - Remote features with local overrides (ON/OFF)
 * - Local features in enabled/disabled states
 *
 * ## Usage
 * ```kotlin
 * @Preview
 * @Composable
 * fun FeatureItemPreview(
 *     @PreviewParameter(FeaturePreviewParameterProvider::class) feature: Feature
 * ) {
 *     FeatureItem(
 *         feature = feature,
 *         onStateChange = { }
 *     )
 * }
 * ```
 *
 * @see Feature
 * @see Feature.LocalFeature
 * @see Feature.RemoteFeature
 * @see com.worldline.devview.featureflip.model.FeatureState
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

    override fun getDisplayName(index: Int): String? =
        when (val feature = values.elementAtOrNull(index = index)) {
            is RemoteFeature,
            is LocalFeature -> feature.name
            else -> null
        }
}

/**
 * Preview parameter provider specifically for [Feature.RemoteFeature] instances.
 *
 * Provides remote features in all possible [FeatureState] combinations with both
 * enabled and disabled default remote values. This comprehensive set allows developers
 * to preview all variations of remote feature states.
 *
 * ## Provided Samples
 * - REMOTE state with enabled default value
 * - REMOTE state with disabled default value
 * - LOCAL_OFF state override
 * - LOCAL_ON state override
 *
 * ## Usage
 * ```kotlin
 * @Preview
 * @Composable
 * fun RemoteFeatureSwitchPreview(
 *     @PreviewParameter(RemoteFeaturePreviewParameterProvider::class) feature: RemoteFeature
 * ) {
 *     FeatureTriStateSwitch(
 *         feature = feature,
 *         onStateChange = { }
 *     )
 * }
 * ```
 *
 * @see Feature.RemoteFeature
 * @see com.worldline.devview.featureflip.model.FeatureState
 * @see com.worldline.devview.featureflip.components.FeatureTriStateSwitch
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

    override fun getDisplayName(index: Int): String? =
        when (val feature = values.elementAtOrNull(index = index)) {
            is RemoteFeature -> feature.name
            else -> null
        }
}

/**
 * Preview parameter provider specifically for [Feature.LocalFeature] instances.
 *
 * Provides local features in both enabled and disabled states to facilitate
 * previewing of local feature UI components.
 *
 * ## Provided Samples
 * - Enabled local feature
 * - Disabled local feature
 *
 * ## Usage
 * ```kotlin
 * @Preview
 * @Composable
 * fun LocalFeatureItemPreview(
 *     @PreviewParameter(LocalFeaturePreviewParameterProvider::class) feature: LocalFeature
 * ) {
 *     FeatureItem(
 *         feature = feature,
 *         onStateChange = { }
 *     )
 * }
 * ```
 *
 * @see Feature.LocalFeature
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

    override fun getDisplayName(index: Int): String? =
        when (val feature = values.elementAtOrNull(index = index)) {
            is LocalFeature -> feature.name
            else -> null
        }
}
