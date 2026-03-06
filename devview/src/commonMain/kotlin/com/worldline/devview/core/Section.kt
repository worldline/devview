package com.worldline.devview.core

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Defines the organizational sections for grouping DevView modules.
 *
 * Sections provide a way to categorize and group related modules in the DevView
 * home screen. Modules within the same section are visually grouped together,
 * making it easier for developers to find and access related functionality.
 *
 * ## Section Organization
 * - [SETTINGS]: Configuration and app information modules
 * - [FEATURES]: Feature flag and development tools
 * - [LOGGING]: Debugging, analytics, and logging tools
 * - [CUSTOM]: Application-specific custom modules
 *
 * ## Usage Example
 * ```kotlin
 * object MyModule : Module {
 *     override val section: Section = Section.FEATURES
 *
 *     // Other module implementation...
 * }
 * ```
 *
 * ## Default Icons
 * Each section has a default icon accessible via the [icon] extension property:
 * - SETTINGS → Settings icon (gear)
 * - FEATURES → DeveloperMode icon
 * - LOGGING → FormatListNumbered icon (list)
 * - CUSTOM → Extension icon (puzzle piece)
 *
 * @see Module
 * @see icon
 */
public enum class Section {
    /**
     * Section for settings and configuration modules.
     *
     * Use this section for modules that provide app configuration, settings,
     * version information, or general app metadata.
     *
     * **Typical modules**: AppInfo, Settings, Configuration
     */
    SETTINGS,

    /**
     * Section for feature flag and development tool modules.
     *
     * Use this section for modules that control feature toggles, A/B tests,
     * or provide development-time feature management.
     *
     * **Typical modules**: FeatureFlip, FeatureToggles, ABTesting
     */
    FEATURES,

    /**
     * Section for network-related modules.
     *
     * Use this section for modules that monitor, log, or debug network requests and responses.
     * This can include tools that display network traffic, analyze API calls, or simulate network conditions.
     *
     * **Typical modules**: NetworkMonitor, API Inspector, NetworkMock
     */
    NETWORK,

    /**
     * Section for logging, analytics, and debugging modules.
     *
     * Use this section for modules that display logs, track analytics events,
     * monitor network traffic, or provide debugging capabilities.
     *
     * **Typical modules**: Analytics, Console, NetworkMonitor, Crashlytics
     */
    LOGGING,

    /**
     * Section for application-specific custom modules.
     *
     * Use this section for modules unique to your application that don't
     * fit into the standard categories.
     *
     * **Typical modules**: App-specific tools, business logic debuggers, custom utilities
     */
    CUSTOM
}

/**
 * Extension property providing the default icon for each section.
 *
 * Returns a Material Icons Rounded icon appropriate for the section type.
 * Modules can override this by providing their own [Module.icon] implementation.
 *
 * ## Icon Mapping
 * - [Section.SETTINGS] → Settings (gear icon)
 * - [Section.FEATURES] → DeveloperMode icon
 * - [Section.LOGGING] → FormatListNumbered (list icon)
 * - [Section.CUSTOM] → Extension (puzzle piece icon)
 *
 * ## Usage
 * ```kotlin
 * val sectionIcon = Section.FEATURES.icon
 *
 * Icon(
 *     imageVector = sectionIcon,
 *     contentDescription = "Features section"
 * )
 * ```
 *
 * @return The [ImageVector] icon for this section.
 *
 * @see Section
 * @see Module.icon
 */
public val Section.icon: ImageVector
    get() = when (this) {
        Section.SETTINGS -> Icons.Rounded.Settings
        Section.FEATURES -> Icons.Rounded.DeveloperMode
        Section.NETWORK -> Icons.Rounded.NetworkCheck
        Section.LOGGING -> Icons.Rounded.FormatListNumbered
        Section.CUSTOM -> Icons.Rounded.Extension
    }
