package com.worldline.devview.featureflip.model

/**
 * Represents the type of a feature flag.
 *
 * Feature types determine whether a feature is controlled remotely or locally.
 *
 * **Note:** When persisting the type, use the [ordinal] property to store it
 * as an integer, and [fromOrdinal] to restore it.
 */
public enum class FeatureType {
    /**
     * A remotely-controlled feature flag.
     * These features are configured via a remote service and can be locally overridden.
     */
    REMOTE,

    /**
     * A locally-controlled feature flag.
     * These features are managed entirely on the device.
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
