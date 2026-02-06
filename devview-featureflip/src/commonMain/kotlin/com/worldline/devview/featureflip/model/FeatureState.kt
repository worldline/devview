package com.worldline.devview.featureflip.model

/**
 * Represents the state of a remote feature flag.
 *
 * This enum is used to manage the current state of [Feature.RemoteFeature] instances,
 * allowing local overrides of remotely-configured feature flags. This is particularly
 * useful during development and testing when you need to force a feature on or off
 * regardless of what the remote configuration says.
 *
 * ## State Transitions
 * Features typically start in [REMOTE] state and can be changed to:
 * - [LOCAL_ON]: For testing when feature should be enabled
 * - [LOCAL_OFF]: For testing when feature should be disabled
 * - Back to [REMOTE]: To restore remote configuration control
 *
 * ## Persistence
 * When persisting the state to DataStore, use the [ordinal] property to store it
 * as an integer, and [fromOrdinal] to restore it:
 * ```kotlin
 * // Saving
 * preferences[key] = featureState.ordinal
 *
 * // Restoring
 * val restoredState = FeatureState.fromOrdinal(preferences[key] ?: 0)
 * ```
 *
 * ## Usage Example
 * ```kotlin
 * // Creating a feature with remote state
 * val feature = Feature.RemoteFeature(
 *     name = "new_feature",
 *     description = "Experimental new feature",
 *     defaultRemoteValue = remoteConfig.getBoolean("new_feature"),
 *     state = FeatureState.REMOTE
 * )
 *
 * // Override locally for testing
 * featureHandler.setFeatureState("new_feature", FeatureState.LOCAL_ON)
 *
 * // Restore remote control
 * featureHandler.setFeatureState("new_feature", FeatureState.REMOTE)
 * ```
 *
 * @see Feature.RemoteFeature
 * @see com.worldline.devview.featureflip.model.FeatureHandler
 */
public enum class FeatureState {
    /**
     * Use the default value from remote configuration.
     *
     * When a feature is in REMOTE state, its enabled status will match whatever
     * value is provided by the remote configuration service. This is the default
     * state for remote features and represents normal production behavior.
     *
     * Use this state when you want the feature to be controlled entirely by
     * your remote configuration system (e.g., Firebase Remote Config, LaunchDarkly).
     */
    REMOTE,

    /**
     * Local override to force the feature OFF, regardless of remote configuration.
     *
     * When a feature is in LOCAL_OFF state, it will always be disabled, even if
     * the remote configuration says it should be enabled. This is useful for:
     * - Testing how the app behaves with a feature disabled
     * - Temporarily disabling a broken feature locally
     * - QA testing of feature toggle functionality
     *
     * The local override persists across app restarts until changed back to
     * REMOTE or LOCAL_ON.
     */
    LOCAL_OFF,

    /**
     * Local override to force the feature ON, regardless of remote configuration.
     *
     * When a feature is in LOCAL_ON state, it will always be enabled, even if
     * the remote configuration says it should be disabled. This is useful for:
     * - Testing new features before they're rolled out
     * - Developer testing of incomplete features
     * - QA validation of feature behavior
     *
     * The local override persists across app restarts until changed back to
     * REMOTE or LOCAL_OFF.
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
