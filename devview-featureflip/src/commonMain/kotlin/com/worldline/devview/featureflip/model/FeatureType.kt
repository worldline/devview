package com.worldline.devview.featureflip.model

/**
 * Represents the type classification of a feature flag.
 *
 * Feature types distinguish between features controlled remotely via a configuration
 * service and features managed entirely on the local device. This classification helps
 * organize features in the UI and determines the available controls.
 *
 * ## Usage
 * The type is automatically determined from the [Feature] subclass:
 * - [Feature.RemoteFeature] → [REMOTE]
 * - [Feature.LocalFeature] → [LOCAL]
 *
 * ## Persistence
 * When persisting the type to storage, use the [ordinal] property to store it
 * as an integer, and [fromOrdinal] to restore it:
 * ```kotlin
 * // Saving
 * preferences[key] = featureType.ordinal
 *
 * // Restoring
 * val restoredType = FeatureType.fromOrdinal(preferences[key] ?: 0)
 * ```
 *
 * ## Filtering
 * The FeatureFlip UI allows filtering features by type, making it easy to
 * find specific categories of features during development and testing.
 *
 * @see Feature
 * @see Feature.RemoteFeature
 * @see Feature.LocalFeature
 */
public enum class FeatureType {
    /**
     * A remotely-controlled feature flag.
     *
     * Remote features are configured via a remote service (such as Firebase Remote Config,
     * LaunchDarkly, or similar) and can be toggled without deploying new app versions.
     * They support local overrides for testing purposes while maintaining the remote
     * configuration as the source of truth.
     *
     * In the UI, remote features are displayed with a tri-state control allowing
     * selection between Remote/Off/On states.
     *
     * @see Feature.RemoteFeature
     * @see FeatureState
     */
    REMOTE,

    /**
     * A locally-controlled feature flag.
     *
     * Local features are managed entirely on the device and stored using DataStore.
     * They are useful for user preferences, device-specific settings, or features
     * in development that don't require remote control.
     *
     * In the UI, local features are displayed with a simple on/off switch.
     *
     * @see Feature.LocalFeature
     */
    LOCAL;

    public companion object {
        /**
         * Converts an ordinal value back to a [FeatureType].
         *
         * @param ordinal The ordinal value to convert (0 = REMOTE, 1 = LOCAL)
         * @return The corresponding [FeatureType]
         * @throws IllegalArgumentException if the ordinal is not valid
         */
        public fun fromOrdinal(ordinal: Int): FeatureType = when (ordinal) {
            0 -> REMOTE
            1 -> LOCAL
            else -> throw IllegalArgumentException("Unknown ordinal: $ordinal")
        }
    }
}
