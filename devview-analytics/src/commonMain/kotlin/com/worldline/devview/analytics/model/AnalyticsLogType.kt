package com.worldline.devview.analytics.model

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Marker interface for individual analytics event types.
 *
 * All analytics log types implement this interface and are organised into
 * categories via [AnalyticsLogCategory]. Types should be accessed through
 * their category for clarity and discoverability:
 *
 * ```kotlin
 * AnalyticsLogCategory.Social.Comment
 * AnalyticsLogCategory.Action.Click
 * AnalyticsLogCategory.Session.Login
 * ```
 *
 * The [category] property provides a typed reference back to the parent
 * [AnalyticsLogCategory], enabling programmatic grouping and filtering
 * without relying on reflection or string comparisons:
 *
 * ```kotlin
 * fun log(type: AnalyticsLogType) {
 *     if (type.category is AnalyticsLogCategory.Social) { ... }
 * }
 * ```
 *
 * @property category The [AnalyticsLogCategory] this type belongs to
 * @property displayName Human-readable name for UI display
 * @property description Detailed description of when to use this type
 * @property icon Material icon representing this log type in the UI
 *
 * @see AnalyticsLogCategory
 */
public sealed interface AnalyticsLogType {
    public val category: AnalyticsLogCategory
    public val displayName: String
    public val description: String
    public val icon: ImageVector

    public companion object {
        /**
         * Returns all available [AnalyticsLogType] instances across all categories.
         *
         * Note: This list must be kept in sync when new types are added,
         * as Kotlin Multiplatform does not support reflection-based subclass discovery.
         */
        public fun allTypes(): List<AnalyticsLogType> = listOf<AnalyticsLogType>(
            AnalyticsLogCategory.Screen.View,
            AnalyticsLogCategory.Session.Start,
            AnalyticsLogCategory.Session.End,
            AnalyticsLogCategory.Session.Login,
            AnalyticsLogCategory.Session.Logout,
            AnalyticsLogCategory.Action.Click,
            AnalyticsLogCategory.Action.Swipe,
            AnalyticsLogCategory.Action.Gesture,
            AnalyticsLogCategory.Action.FormInput,
            AnalyticsLogCategory.Action.Scroll,
            AnalyticsLogCategory.Search.Query,
            AnalyticsLogCategory.Search.ResultClick,
            AnalyticsLogCategory.Performance.Metrics,
            AnalyticsLogCategory.Performance.Network,
            AnalyticsLogCategory.Performance.Error,
            AnalyticsLogCategory.Performance.Crash,
            AnalyticsLogCategory.Ecommerce.Purchase,
            AnalyticsLogCategory.Ecommerce.AddToCart,
            AnalyticsLogCategory.Ecommerce.RemoveFromCart,
            AnalyticsLogCategory.Ecommerce.Checkout,
            AnalyticsLogCategory.Ecommerce.Subscription,
            AnalyticsLogCategory.Media.VideoStart,
            AnalyticsLogCategory.Media.VideoEnd,
            AnalyticsLogCategory.Media.AudioPlay,
            AnalyticsLogCategory.Media.Download,
            AnalyticsLogCategory.Feature.Enabled,
            AnalyticsLogCategory.Feature.Disabled,
            AnalyticsLogCategory.Feature.Usage,
            AnalyticsLogCategory.Social.Share,
            AnalyticsLogCategory.Social.Like,
            AnalyticsLogCategory.Social.Comment,
            AnalyticsLogCategory.Custom.Event,
            AnalyticsLogCategory.Diagnostic.Debug,
            AnalyticsLogCategory.Diagnostic.Log
        )

        /**
         * Returns all [AnalyticsLogType] instances grouped by their [AnalyticsLogCategory].
         *
         * @return A map of [AnalyticsLogCategory] to the list of analytics types belonging to it
         */
        public fun typesByCategory(): Map<AnalyticsLogCategory, List<AnalyticsLogType>> =
            allTypes().groupBy { it.category }
    }
}
