package com.worldline.devview.featureflip.model

import kotlin.random.Random

/**
 * Sealed class representing a feature flag in the application.
 *
 * Features can be either [RemoteFeature] (controlled by a remote configuration service)
 * or [LocalFeature] (stored and managed locally on the device).
 */
public sealed class Feature {
    /**
     * The unique identifier name of the feature.
     */
    public abstract val name: String

    /**
     * Optional description explaining what this feature does.
     */
    public abstract val description: String?

    /**
     * Whether the feature is currently enabled.
     * The actual value depends on the feature type and its current state.
     */
    public abstract val isEnabled: Boolean

    internal companion object {
        internal fun fake(amount: Int = 5) = List(size = amount) {
            when (Random.nextBoolean()) {
                true -> RemoteFeature.fake(index = it)
                false -> LocalFeature.fake(index = it)
            }
        }
    }

    /**
     * A feature flag controlled by a remote configuration service.
     *
     * Remote features support three states:
     * - [FeatureState.REMOTE]: Use the default remote value
     * - [FeatureState.LOCAL_ON]: Override to force the feature on
     * - [FeatureState.LOCAL_OFF]: Override to force the feature off
     *
     * @property name The unique identifier name of the feature
     * @property description Optional description of the feature's functionality
     * @property defaultRemoteValue The default value provided by the remote configuration
     * @property state The current state of the feature (remote or local override)
     */
    public data class RemoteFeature(
        override val name: String,
        override val description: String?,
        val defaultRemoteValue: Boolean,
        val state: FeatureState
    ) : Feature() {
        override val isEnabled: Boolean
            get() = when (state) {
                FeatureState.REMOTE -> defaultRemoteValue
                FeatureState.LOCAL_OFF -> false
                FeatureState.LOCAL_ON -> true
            }

        internal companion object {
            fun fakeList(amount: Int = 5) = List(size = amount) {
                RemoteFeature(
                    name = "Feature ${it + 1}",
                    description = "Description for feature ${it + 1}",
                    defaultRemoteValue = Random.nextBoolean(),
                    state = FeatureState.entries.random()
                )
            }

            fun fake(index: Int) = RemoteFeature(
                name = "Feature ${index + 5}",
                description = "Description for feature ${index + 5}",
                defaultRemoteValue = Random.nextBoolean(),
                state = FeatureState.REMOTE
            )
        }
    }

    /**
     * A locally-managed feature flag stored on the device.
     *
     * Local features have a simple on/off state without remote configuration.
     * They are useful for features that should be controlled per-device.
     *
     * @property name The unique identifier name of the feature
     * @property description Optional description of the feature's functionality
     * @property isEnabled Whether the feature is currently enabled
     */
    public data class LocalFeature(
        override val name: String,
        override val description: String?,
        override val isEnabled: Boolean
    ) : Feature() {
        internal companion object {
            fun fakeList(amount: Int = 5) = List(size = amount) {
                LocalFeature(
                    name = "Feature ${it + 1}",
                    description = "Description for feature ${it + 1}",
                    isEnabled = Random.nextBoolean()
                )
            }

            fun fake(index: Int) = LocalFeature(
                name = "Feature ${index + 1}",
                description = "Description for feature ${index + 1}",
                isEnabled = Random.nextBoolean()
            )
        }
    }
}
