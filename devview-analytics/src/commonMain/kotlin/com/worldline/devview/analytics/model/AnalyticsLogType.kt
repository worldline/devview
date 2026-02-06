package com.worldline.devview.analytics.model

/**
 * Defines the types of analytics events that can be logged and displayed.
 *
 * This enumeration categorizes analytics logs into different types to help
 * organize and filter events in the analytics UI.
 *
 * @see AnalyticsLog
 */
public enum class AnalyticsLogType {
    /**
     * Represents screen view or page view events.
     *
     * Use this type to track navigation and screen impressions in your application.
     * Typically logged when a user navigates to a new screen or page.
     *
     * Example use case: Tracking when users view the Home screen, Settings screen, etc.
     */
    SCREEN,

    /**
     * Represents general user interaction and application events.
     *
     * Use this type for standard analytics events such as button clicks,
     * form submissions, and other user interactions.
     *
     * Example use case: Tracking button clicks, user login, search queries, etc.
     */
    EVENT,

    /**
     * Represents custom or business-specific analytics events.
     *
     * Use this type for events that are unique to your application's business logic
     * or don't fit into the SCREEN or EVENT categories.
     *
     * Example use case: Tracking custom metrics like purchase completion,
     * subscription status changes, feature usage, etc.
     */
    CUSTOM
}
