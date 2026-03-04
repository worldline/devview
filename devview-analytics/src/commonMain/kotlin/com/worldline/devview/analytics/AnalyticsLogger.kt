package com.worldline.devview.analytics

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import com.worldline.devview.analytics.model.AnalyticsLog

/**
 * Central logger for managing analytics events in the DevView Analytics module.
 *
 * This singleton object provides thread-safe, reactive storage for analytics logs using
 * Compose's snapshot state system. All logged events are stored in-memory and can be
 * observed reactively by UI components.
 *
 * ## Usage
 *
 * ### Logging Events
 * ```kotlin
 * AnalyticsLogger.log(
 *     AnalyticsLog(
 *         tag = "user_login",
 *         screenClass = "LoginScreen",
 *         timestamp = System.currentTimeMillis(),
 *         type = AnalyticsLogCategory.Session.Login
 *     )
 * )
 * ```
 *
 * ### Accessing Logs
 * ```kotlin
 * val allLogs = AnalyticsLogger.logs
 * val hasAnyLogs = AnalyticsLogger.hasLogs
 * ```
 *
 * ### Clearing Logs
 * ```kotlin
 * // Internal use only
 * AnalyticsLogger.clear()
 * ```
 *
 * @see AnalyticsLog
 * @see AnalyticsScreen
 */
public object AnalyticsLogger {
    private val analyticsLogs = mutableStateListOf<AnalyticsLog>()

    /**
     * Adds a new analytics log entry to the internal storage.
     *
     * This method is thread-safe and will trigger recomposition in any Compose
     * UI that observes the [logs] property.
     *
     * @param log The analytics log entry to add.
     *
     * @sample
     * ```kotlin
     * AnalyticsLogger.log(
     *     AnalyticsLog(
     *         tag = "HomeScreen",
     *         screenClass = "com.example.ui.HomeScreen",
     *         timestamp = System.currentTimeMillis(),
     *         type = AnalyticsLogCategory.Screen.View
     *     )
     * )
     * ```
     */
    public fun log(log: AnalyticsLog) {
        analyticsLogs.add(element = log)
    }

    /**
     * Clears all stored analytics logs.
     *
     * This method is intended for internal use by the DevView framework.
     * Use with caution as it will remove all historical log data.
     */
    internal fun clear() {
        analyticsLogs.clear()
    }

    /**
     * Returns a snapshot state list of all analytics logs.
     *
     * This property provides reactive access to the logs, meaning any Compose
     * UI observing this property will automatically recompose when logs are
     * added or removed.
     *
     * @return A [SnapshotStateList] containing all logged analytics events in chronological order.
     */
    public val logs: SnapshotStateList<AnalyticsLog>
        get() = analyticsLogs

    /**
     * Indicates whether any analytics logs have been recorded.
     *
     * @return `true` if at least one log entry exists, `false` otherwise.
     */
    public val hasLogs: Boolean
        get() = analyticsLogs.isNotEmpty()
}

/**
 * CompositionLocal for providing analytics logs to Compose UI components.
 *
 * This allows analytics logs to be injected into the Compose tree and accessed
 * by child composables without explicit parameter passing.
 *
 * ## Usage
 *
 * ### Providing Logs
 * ```kotlin
 * CompositionLocalProvider(LocalAnalytics provides AnalyticsLogger.logs) {
 *     AnalyticsScreen()
 * }
 * ```
 *
 * ### Consuming Logs
 * ```kotlin
 * @Composable
 * fun MyComponent() {
 *     val analytics = LocalAnalytics.current
 *     // Use analytics logs
 * }
 * ```
 *
 * @throws IllegalStateException if accessed without being provided in the Compose tree.
 *
 * @see AnalyticsScreen
 * @see AnalyticsLogger
 */
public val LocalAnalytics: ProvidableCompositionLocal<List<AnalyticsLog>> =
    staticCompositionLocalOf {
        error(message = "No analytics provided.")
    }
