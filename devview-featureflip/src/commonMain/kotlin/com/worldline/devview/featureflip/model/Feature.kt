package com.worldline.devview.featureflip.model

import kotlin.random.Random

/**
 * Sealed class representing a feature flag in the application.
 *
 * Feature flags (also known as feature toggles) allow you to enable or disable
 * features in your application without deploying new code. This sealed class
 * provides two implementations:
 * - [LocalFeature]: Simple device-local flags stored using DataStore
 * - [RemoteFeature]: Flags controlled by remote configuration with local override capability
 *
 * ## Usage
 *
 * ### Defining Features
 * ```kotlin
 * val features = listOf(
 *     Feature.LocalFeature(
 *         name = "dark_mode",
 *         description = "Enable dark theme",
 *         isEnabled = false
 *     ),
 *     Feature.RemoteFeature(
 *         name = "new_checkout",
 *         description = "New checkout flow",
 *         defaultRemoteValue = true,
 *         state = FeatureState.REMOTE
 *     )
 * )
 * ```
 *
 * ### Checking Feature State
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     val featureHandler = LocalFeatureHandler.current
 *     val isDarkModeEnabled by featureHandler.isFeatureEnabled("dark_mode")
 *
 *     if (isDarkModeEnabled) {
 *         DarkTheme { Content() }
 *     } else {
 *         LightTheme { Content() }
 *     }
 * }
 * ```
 *
 * @see LocalFeature
 * @see RemoteFeature
 * @see FeatureHandler
 * @see FeatureState
 */
public sealed class Feature {
    /**
     * The unique identifier name of the feature.
     *
     * This should be a stable identifier that doesn't change over time.
     * Common naming conventions include snake_case or camelCase.
     *
     * Examples: "dark_mode", "newCheckoutFlow", "feature_premium_tier"
     */
    public abstract val name: String

    /**
     * Optional description explaining what this feature does.
     *
     * This description is displayed in the DevView UI to help developers
     * and testers understand the purpose of the feature flag.
     *
     * Examples:
     * - "Enables dark theme throughout the application"
     * - "New checkout flow with improved UX"
     * - "Premium tier features for subscribed users"
     */
    public abstract val description: String?

    /**
     * Whether the feature is currently enabled.
     *
     * The actual value depends on the feature type and its current state:
     * - For [LocalFeature]: Returns the stored boolean value
     * - For [RemoteFeature]: Returns the effective enabled state based on
     *   the current [FeatureState] and [RemoteFeature.defaultRemoteValue]
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
     * A feature flag controlled by a remote configuration service with local override capability.
     *
     * Remote features allow you to control features from a remote configuration service
     * (like Firebase Remote Config, LaunchDarkly, etc.) while also allowing developers
     * and testers to override the remote value locally for testing purposes.
     *
     * ## Feature States
     * Remote features support three states:
     * - [FeatureState.REMOTE]: Use the [defaultRemoteValue] from remote configuration
     * - [FeatureState.LOCAL_ON]: Force the feature ON regardless of remote configuration
     * - [FeatureState.LOCAL_OFF]: Force the feature OFF regardless of remote configuration
     *
     * ## Effective Enabled State
     * The [isEnabled] property returns the effective enabled state based on:
     * - If state is REMOTE: returns [defaultRemoteValue]
     * - If state is LOCAL_ON: returns true
     * - If state is LOCAL_OFF: returns false
     *
     * ## Usage Example
     * ```kotlin
     * // Feature controlled by remote config
     * val newCheckout = Feature.RemoteFeature(
     *     name = "new_checkout_flow",
     *     description = "Enable the redesigned checkout experience",
     *     defaultRemoteValue = remoteConfig.getBoolean("new_checkout_flow"),
     *     state = FeatureState.REMOTE
     * )
     *
     * // In the UI, users can override to test locally
     * // - Set to LOCAL_ON to force enable for testing
     * // - Set to LOCAL_OFF to force disable
     * // - Set back to REMOTE to use server value
     * ```
     *
     * @property name The unique identifier name of the feature (e.g., "new_checkout_flow").
     * @property description Optional human-readable description explaining what this feature does.
     * @property defaultRemoteValue The default value provided by the remote configuration service.
     * @property state The current state controlling whether to use remote value or local override.
     *
     * @see FeatureState
     * @see LocalFeature
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
     * Local features are simple boolean flags that are stored and managed entirely
     * on the device using DataStore. They are ideal for features that should be
     * controlled on a per-device or per-user basis without remote configuration.
     *
     * Unlike [RemoteFeature], local features have a straightforward on/off state
     * without the concept of remote values or local overrides.
     *
     * ## Use Cases
     * - User preferences (e.g., dark mode, notifications)
     * - Device-specific settings
     * - Features in development that aren't ready for remote control
     * - A/B test variants assigned to specific devices
     *
     * ## Usage Example
     * ```kotlin
     * val darkMode = Feature.LocalFeature(
     *     name = "dark_mode",
     *     description = "Enable dark theme throughout the app",
     *     isEnabled = false
     * )
     *
     * // Check if enabled
     * @Composable
     * fun App() {
     *     val featureHandler = LocalFeatureHandler.current
     *     val isDarkModeEnabled by featureHandler.isFeatureEnabled("dark_mode")
     *
     *     MaterialTheme(
     *         colorScheme = if (isDarkModeEnabled) darkColorScheme() else lightColorScheme()
     *     ) {
     *         Content()
     *     }
     * }
     * ```
     *
     * @property name The unique identifier name of the feature (e.g., "dark_mode").
     * @property description Optional human-readable description explaining what this feature does.
     * @property isEnabled Whether the feature is currently enabled on this device.
     *
     * @see RemoteFeature
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
