package com.worldline.devview.featureflip.model

/**
 * Represents the state of a feature flag.
 *
 * This enum is used to manage the current state of remote features, allowing
 * local overrides of remotely-configured feature flags.
 *
 * **Note:** When persisting the state, use the [ordinal] property to store it
 * as an integer, and [fromOrdinal] to restore it.
 */
public enum class FeatureState {
    /**
     * Use the default value from remote configuration.
     * The feature's enabled state will match the remote server's setting.
     */
    REMOTE,

    /**
     * Local override to force the feature off, regardless of remote configuration.
     */
    LOCAL_OFF,

    /**
     * Local override to force the feature on, regardless of remote configuration.
     */
    LOCAL_ON;

    public companion object {
        /**
         * Converts an ordinal value back to a [FeatureState].
         *
         * @param ordinal The ordinal value to convert (0 = REMOTE, 1 = LOCAL_OFF, 2 = LOCAL_ON)
         * @return The corresponding [FeatureState]
         * @throws IllegalArgumentException if the ordinal is not valid
         */
        public fun fromOrdinal(ordinal: Int): FeatureState = when (ordinal) {
            0 -> REMOTE
            1 -> LOCAL_OFF
            2 -> LOCAL_ON
            else -> throw IllegalArgumentException("Unknown ordinal: $ordinal")
        }
    }
}
