package com.worldline.devview.analytics.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.SwipeRight
import androidx.compose.material.icons.filled.SwipeVertical
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Defines the categories of analytics events that can be logged and displayed.
 *
 * This sealed interface groups [AnalyticsLogType] instances into named categories,
 * enabling structured discovery and type-safe category references.
 *
 * Each nested sealed interface represents a category and contains the [AnalyticsLogType]
 * instances that belong to it. Types can be accessed directly by category:
 *
 * ```kotlin
 * AnalyticsLogCategory.Social.Comment
 * AnalyticsLogCategory.Action.Click
 * AnalyticsLogCategory.Session.Login
 * ```
 *
 * @property displayName Human-readable name of the category for UI display
 * @property containerColor Background color for UI elements representing this category,
 * derived from the current [MaterialTheme] color scheme
 * @property contentColor Foreground color for UI elements representing this category,
 * derived from the current [MaterialTheme] color scheme
 * @property icon Material icon representing this category in the UI
 *
 * @see AnalyticsLogType
 */
@Immutable
public sealed interface AnalyticsLogCategory {
    public val displayName: String

    public val containerColor: Color
        @Composable @ReadOnlyComposable
        get

    public val contentColor: Color
        @Composable @ReadOnlyComposable
        get

    public val icon: ImageVector

    /**
     * Groups screen view and navigation events.
     *
     * Use types in this category to track screen impressions and navigation
     * flows in your application.
     */
    public sealed interface Screen : AnalyticsLogCategory {
        override val displayName: String get() = "Screen & Navigation"

        override val containerColor: Color
            @Composable @ReadOnlyComposable
            get() = MaterialTheme.colorScheme.primaryContainer

        override val contentColor: Color
            @Composable @ReadOnlyComposable
            get() = MaterialTheme.colorScheme.onPrimaryContainer

        override val icon: ImageVector
            get() = Icons.AutoMirrored.Filled.OpenInNew

        /**
         * Screen view or page view event.
         *
         * Typically logged when a user navigates to a new screen or page.
         *
         * Example use case: Tracking when users view the Home screen, Settings screen, etc.
         */
        public data object View : Screen, AnalyticsLogType {
            override val category: AnalyticsLogCategory get() = this
            override val displayName: String = "Screen View"
            override val description: String = "Screen or page view event"
            override val icon: ImageVector
                get() = Icons.AutoMirrored.Filled.OpenInNew
        }
    }

    /**
     * Groups user session lifecycle events.
     *
     * Use types in this category to track session boundaries and authentication
     * state changes in your application.
     */
    public sealed interface Session : AnalyticsLogCategory {
        override val displayName: String get() = "User & Session"

        override val containerColor: Color
            @Composable @ReadOnlyComposable
            get() = MaterialTheme.colorScheme.secondaryContainer

        override val contentColor: Color
            @Composable @ReadOnlyComposable
            get() = MaterialTheme.colorScheme.onSecondaryContainer

        override val icon: ImageVector
            get() = Icons.Filled.Person

        /**
         * User session initialization event.
         *
         * Logged when a user starts a new session in the application.
         */
        public data object Start : Session, AnalyticsLogType {
            override val category: AnalyticsLogCategory get() = this
            override val displayName: String = "Session Start"
            override val description: String = "User session initialization"
            override val icon: ImageVector
                get() = Icons.Filled.PlayCircle
        }

        /**
         * User session termination event.
         *
         * Logged when a user's session ends or the application is backgrounded.
         */
        public data object End : Session, AnalyticsLogType {
            override val category: AnalyticsLogCategory get() = this
            override val displayName: String = "Session End"
            override val description: String = "User session termination"
            override val icon: ImageVector
                get() = Icons.Filled.StopCircle
        }

        /**
         * User authentication/login event.
         *
         * Logged when a user successfully authenticates or logs into the application.
         */
        public data object Login : Session, AnalyticsLogType {
            override val category: AnalyticsLogCategory get() = this
            override val displayName: String = "User Login"
            override val description: String = "User authentication or login"
            override val icon: ImageVector
                get() = Icons.AutoMirrored.Filled.Login
        }

        /**
         * User sign-out event.
         *
         * Logged when a user logs out or signs out of the application.
         */
        public data object Logout : Session, AnalyticsLogType {
            override val category: AnalyticsLogCategory get() = this
            override val displayName: String = "User Logout"
            override val description: String = "User sign-out or logout"
            override val icon: ImageVector
                get() = Icons.AutoMirrored.Filled.Logout
        }
    }

    /**
     * Groups user interaction and gesture events.
     *
     * Use types in this category to track how users physically interact
     * with the UI in your application.
     */
    public sealed interface Action : AnalyticsLogCategory {
        override val displayName: String get() = "User Actions"

        override val containerColor: Color
            @Composable @ReadOnlyComposable
            get() = MaterialTheme.colorScheme.tertiaryContainer

        override val contentColor: Color
            @Composable @ReadOnlyComposable
            get() = MaterialTheme.colorScheme.onTertiaryContainer

        override val icon: ImageVector
            get() = Icons.Filled.TouchApp

        /**
         * UI element click event.
         *
         * Logged when a user taps or clicks on interactive UI elements.
         *
         * Example use case: Button clicks, link clicks, menu item selections.
         */
        public data object Click : Action, AnalyticsLogType {
            override val category: AnalyticsLogCategory get() = this
            override val displayName: String = "Click"
            override val description: String = "UI element click or tap"
            override val icon: ImageVector
                get() = Icons.Filled.TouchApp
        }

        /**
         * Gesture-based swipe interaction event.
         *
         * Logged when a user performs a swipe or other multitouch gesture.
         */
        public data object Swipe : Action, AnalyticsLogType {
            override val category: AnalyticsLogCategory get() = this
            override val displayName: String = "Swipe"
            override val description: String = "Swipe or multi-touch gesture"
            override val icon: ImageVector
                get() = Icons.Filled.SwipeRight
        }

        /**
         * General gesture recognition event.
         *
         * Logged for complex gesture patterns and interactions.
         */
        public data object Gesture : Action, AnalyticsLogType {
            override val category: AnalyticsLogCategory get() = this
            override val displayName: String = "Gesture"
            override val description: String = "Complex gesture pattern"
            override val icon: ImageVector
                get() = Icons.Filled.Gesture
        }

        /**
         * Form field change or form submission event.
         *
         * Logged when a user interacts with form fields or submits a form.
         */
        public data object FormInput : Action, AnalyticsLogType {
            override val category: AnalyticsLogCategory get() = this
            override val displayName: String = "Form Input"
            override val description: String = "Form field interaction or submission"
            override val icon: ImageVector
                get() = Icons.Filled.EditNote
        }

        /**
         * Scroll event.
         *
         * Logged when a user scrolls through content.
         */
        public data object Scroll : Action, AnalyticsLogType {
            override val category: AnalyticsLogCategory get() = this
            override val displayName: String = "Scroll"
            override val description: String = "Content scroll event"
            override val icon: ImageVector
                get() = Icons.Filled.SwipeVertical
        }
    }

    /**
     * Groups search and content discovery events.
     *
     * Use types in this category to track how users search and discover
     * content in your application.
     */
    public sealed interface Search : AnalyticsLogCategory {
        override val displayName: String get() = "Search & Discovery"

        override val containerColor: Color
            @Composable @ReadOnlyComposable
            get() = MaterialTheme.colorScheme.primaryContainer

        override val contentColor: Color
            @Composable @ReadOnlyComposable
            get() = MaterialTheme.colorScheme.onPrimaryContainer

        override val icon: ImageVector
            get() = Icons.Filled.Search

        /**
         * Search query event.
         *
         * Logged when a user performs a search operation.
         */
        public data object Query : Search, AnalyticsLogType {
            override val category: AnalyticsLogCategory get() = this
            override val displayName: String = "Search Query"
            override val description: String = "User search operation"
            override val icon: ImageVector
                get() = Icons.Filled.Search
        }

        /**
         * Search result click event.
         *
         * Logged when a user selects or clicks on a search result.
         */
        public data object ResultClick : Search, AnalyticsLogType {
            override val category: AnalyticsLogCategory get() = this
            override val displayName: String = "Search Result Click"
            override val description: String = "Search result selection"
            override val icon: ImageVector
                get() = Icons.AutoMirrored.Filled.ManageSearch
        }
    }

    /**
     * Groups performance and technical diagnostic events.
     *
     * Use types in this category to track errors, crashes, network activity,
     * and performance metrics in your application.
     */
    public sealed interface Performance : AnalyticsLogCategory {
        override val displayName: String get() = "Performance & Technical"

        override val containerColor: Color
            @Composable @ReadOnlyComposable
            get() = MaterialTheme.colorScheme.errorContainer

        override val contentColor: Color
            @Composable @ReadOnlyComposable
            get() = MaterialTheme.colorScheme.onErrorContainer

        override val icon: ImageVector
            get() = Icons.Filled.Speed

        /**
         * Performance metrics event.
         *
         * Logged for performance data such as load times, frame rates, and API response times.
         */
        public data object Metrics : Performance, AnalyticsLogType {
            override val category: AnalyticsLogCategory get() = this
            override val displayName: String = "Performance Metrics"
            override val description: String = "Performance data (load times, frame rates, etc.)"
            override val icon: ImageVector
                get() = Icons.Filled.Speed
        }

        /**
         * Network-related event.
         *
         * Logged for network activities, connectivity changes, and API calls.
         */
        public data object Network : Performance, AnalyticsLogType {
            override val category: AnalyticsLogCategory get() = this
            override val displayName: String = "Network Event"
            override val description: String = "Network activity or connectivity change"
            override val icon: ImageVector
                get() = Icons.Filled.Wifi
        }

        /**
         * Error or exception event.
         *
         * Logged when an error occurs in the application.
         */
        public data object Error : Performance, AnalyticsLogType {
            override val category: AnalyticsLogCategory get() = this
            override val displayName: String = "Error"
            override val description: String = "Application error or exception"
            override val icon: ImageVector
                get() = Icons.Filled.ErrorOutline
        }

        /**
         * Application crash event.
         *
         * Logged when the application crashes or encounters a fatal error.
         */
        public data object Crash : Performance, AnalyticsLogType {
            override val category: AnalyticsLogCategory get() = this
            override val displayName: String = "Crash"
            override val description: String = "Application crash or fatal error"
            override val icon: ImageVector
                get() = Icons.Filled.BugReport
        }
    }

    /**
     * Groups e-commerce and transaction events.
     *
     * Use types in this category to track purchases, cart interactions,
     * and subscription events in your application.
     */
    public sealed interface Ecommerce : AnalyticsLogCategory {
        override val displayName: String get() = "E-Commerce"

        override val containerColor: Color
            @Composable @ReadOnlyComposable
            get() = MaterialTheme.colorScheme.secondaryContainer

        override val contentColor: Color
            @Composable @ReadOnlyComposable
            get() = MaterialTheme.colorScheme.onSecondaryContainer

        override val icon: ImageVector
            get() = Icons.Filled.ShoppingCart

        /**
         * Purchase or transaction event.
         *
         * Logged when a user completes a purchase or transaction.
         */
        public data object Purchase : Ecommerce, AnalyticsLogType {
            override val category: AnalyticsLogCategory get() = this
            override val displayName: String = "Purchase"
            override val description: String = "User purchase or transaction completion"
            override val icon: ImageVector
                get() = Icons.Filled.ShoppingCartCheckout
        }

        /**
         * Add to cart event.
         *
         * Logged when a user adds an item to their shopping cart.
         */
        public data object AddToCart : Ecommerce, AnalyticsLogType {
            override val category: AnalyticsLogCategory get() = this
            override val displayName: String = "Add to Cart"
            override val description: String = "Item added to shopping cart"
            override val icon: ImageVector
                get() = Icons.Filled.AddShoppingCart
        }

        /**
         * Remove from cart event.
         *
         * Logged when a user removes an item from their shopping cart.
         */
        public data object RemoveFromCart : Ecommerce, AnalyticsLogType {
            override val category: AnalyticsLogCategory get() = this
            override val displayName: String = "Remove from Cart"
            override val description: String = "Item removed from shopping cart"
            override val icon: ImageVector
                get() = Icons.Filled.RemoveShoppingCart
        }

        /**
         * Checkout process event.
         *
         * Logged for events related to the checkout flow.
         */
        public data object Checkout : Ecommerce, AnalyticsLogType {
            override val category: AnalyticsLogCategory get() = this
            override val displayName: String = "Checkout"
            override val description: String = "Checkout process event"
            override val icon: ImageVector
                get() = Icons.Filled.CreditCard
        }

        /**
         * Subscription-related event.
         *
         * Logged for subscription actions such as subscribe, unsubscribe, or renewal.
         */
        public data object Subscription : Ecommerce, AnalyticsLogType {
            override val category: AnalyticsLogCategory get() = this
            override val displayName: String = "Subscription"
            override val description: String =
                "Subscription action (subscribe, unsubscribe, renewal)"
            override val icon: ImageVector
                get() = Icons.Filled.Subscriptions
        }
    }

    /**
     * Groups content and media playback events.
     *
     * Use types in this category to track video, audio, and download
     * interactions in your application.
     */
    public sealed interface Media : AnalyticsLogCategory {
        override val displayName: String get() = "Content & Media"

        override val containerColor: Color
            @Composable @ReadOnlyComposable
            get() = MaterialTheme.colorScheme.tertiaryContainer

        override val contentColor: Color
            @Composable @ReadOnlyComposable
            get() = MaterialTheme.colorScheme.onTertiaryContainer

        override val icon: ImageVector
            get() = Icons.Filled.PlayArrow

        /**
         * Video playback started event.
         *
         * Logged when a user starts playing a video.
         */
        public data object VideoStart : Media, AnalyticsLogType {
            override val category: AnalyticsLogCategory get() = this
            override val displayName: String = "Video Start"
            override val description: String = "Video playback started"
            override val icon: ImageVector
                get() = Icons.Filled.PlayArrow
        }

        /**
         * Video playback completed event.
         *
         * Logged when a user finishes watching a video.
         */
        public data object VideoEnd : Media, AnalyticsLogType {
            override val category: AnalyticsLogCategory get() = this
            override val displayName: String = "Video End"
            override val description: String = "Video playback completed"
            override val icon: ImageVector
                get() = Icons.Filled.VideocamOff
        }

        /**
         * Audio playback event.
         *
         * Logged for audio playback actions.
         */
        public data object AudioPlay : Media, AnalyticsLogType {
            override val category: AnalyticsLogCategory get() = this
            override val displayName: String = "Audio Play"
            override val description: String = "Audio playback action"
            override val icon: ImageVector
                get() = Icons.Filled.Headphones
        }

        /**
         * Download event.
         *
         * Logged when a user downloads a file or content.
         */
        public data object Download : Media, AnalyticsLogType {
            override val category: AnalyticsLogCategory get() = this
            override val displayName: String = "Download"
            override val description: String = "File or content download"
            override val icon: ImageVector
                get() = Icons.Filled.Download
        }
    }

    /**
     * Groups feature flag and feature usage events.
     *
     * Use types in this category to track feature flag state changes
     * and general feature usage in your application.
     */
    public sealed interface Feature : AnalyticsLogCategory {
        override val displayName: String get() = "Feature Usage"

        override val containerColor: Color
            @Composable @ReadOnlyComposable
            get() = MaterialTheme.colorScheme.primaryContainer

        override val contentColor: Color
            @Composable @ReadOnlyComposable
            get() = MaterialTheme.colorScheme.onPrimaryContainer

        override val icon: ImageVector
            get() = Icons.Filled.Lightbulb

        /**
         * Feature enabled event.
         *
         * Logged when a feature flag or feature toggle is enabled for a user.
         */
        public data object Enabled : Feature, AnalyticsLogType {
            override val category: AnalyticsLogCategory get() = this
            override val displayName: String = "Feature Enabled"
            override val description: String = "Feature flag or toggle enabled"
            override val icon: ImageVector
                get() = Icons.Filled.ToggleOn
        }

        /**
         * Feature disabled event.
         *
         * Logged when a feature flag or feature toggle is disabled for a user.
         */
        public data object Disabled : Feature, AnalyticsLogType {
            override val category: AnalyticsLogCategory get() = this
            override val displayName: String = "Feature Disabled"
            override val description: String = "Feature flag or toggle disabled"
            override val icon: ImageVector
                get() = Icons.Filled.ToggleOff
        }

        /**
         * Feature usage event.
         *
         * Logged for general feature usage tracking.
         */
        public data object Usage : Feature, AnalyticsLogType {
            override val category: AnalyticsLogCategory get() = this
            override val displayName: String = "Feature Usage"
            override val description: String = "General feature usage tracking"
            override val icon: ImageVector
                get() = Icons.Filled.Lightbulb
        }
    }

    /**
     * Groups social and engagement events.
     *
     * Use types in this category to track social interactions such as
     * shares, likes, and comments in your application.
     */
    public sealed interface Social : AnalyticsLogCategory {
        override val displayName: String get() = "Social & Engagement"

        override val containerColor: Color
            @Composable @ReadOnlyComposable
            get() = MaterialTheme.colorScheme.secondaryContainer

        override val contentColor: Color
            @Composable @ReadOnlyComposable
            get() = MaterialTheme.colorScheme.onSecondaryContainer

        override val icon: ImageVector
            get() = Icons.Filled.Share

        /**
         * Content sharing event.
         *
         * Logged when a user shares content.
         */
        public data object Share : Social, AnalyticsLogType {
            override val category: AnalyticsLogCategory get() = this
            override val displayName: String = "Share"
            override val description: String = "Content sharing action"
            override val icon: ImageVector
                get() = Icons.Filled.Share
        }

        /**
         * Like or favorite event.
         *
         * Logged when a user likes or marks content as favorite.
         */
        public data object Like : Social, AnalyticsLogType {
            override val category: AnalyticsLogCategory get() = this
            override val displayName: String = "Like"
            override val description: String = "Like or favorite interaction"
            override val icon: ImageVector
                get() = Icons.Filled.Favorite
        }

        /**
         * Comment or feedback submission event.
         *
         * Logged when a user posts a comment or submits feedback.
         */
        public data object Comment : Social, AnalyticsLogType {
            override val category: AnalyticsLogCategory get() = this
            override val displayName: String = "Comment"
            override val description: String = "Comment or feedback submission"
            override val icon: ImageVector
                get() = Icons.AutoMirrored.Filled.Comment
        }
    }

    /**
     * Groups custom and business-specific events.
     *
     * Use types in this category for events unique to your application's
     * business logic that don't fit into other predefined categories.
     */
    public sealed interface Custom : AnalyticsLogCategory {
        override val displayName: String get() = "Custom"

        override val containerColor: Color
            @Composable @ReadOnlyComposable
            get() = MaterialTheme.colorScheme.surfaceVariant

        override val contentColor: Color
            @Composable @ReadOnlyComposable
            get() = MaterialTheme.colorScheme.onSurfaceVariant

        override val icon: ImageVector
            get() = Icons.Filled.Stars

        /**
         * Custom or business-specific analytics event.
         *
         * Use this type for events unique to your application's business logic
         * that don't fit into other predefined categories.
         *
         * Example use case: Tracking custom metrics like feature experiments,
         * business process milestones, or application-specific workflows.
         */
        public data object Event : Custom, AnalyticsLogType {
            override val category: AnalyticsLogCategory get() = this
            override val displayName: String = "Custom Event"
            override val description: String = "Custom or business-specific event"
            override val icon: ImageVector
                get() = Icons.Filled.Stars
        }
    }

    /**
     * Groups diagnostic and debugging events.
     *
     * Use types in this category for development-time diagnostic information
     * and general log entries.
     */
    public sealed interface Diagnostic : AnalyticsLogCategory {
        override val displayName: String get() = "Diagnostic"

        override val containerColor: Color
            @Composable @ReadOnlyComposable
            get() = MaterialTheme.colorScheme.errorContainer

        override val contentColor: Color
            @Composable @ReadOnlyComposable
            get() = MaterialTheme.colorScheme.onErrorContainer

        override val icon: ImageVector
            get() = Icons.Filled.Code

        /**
         * Debug information event.
         *
         * Logged for debug-level diagnostic information during development.
         */
        public data object Debug : Diagnostic, AnalyticsLogType {
            override val category: AnalyticsLogCategory get() = this
            override val displayName: String = "Debug"
            override val description: String = "Debug-level diagnostic information"
            override val icon: ImageVector
                get() = Icons.Filled.Code
        }

        /**
         * General logging event.
         *
         * Logged for general informational messages and log entries.
         */
        public data object Log : Diagnostic, AnalyticsLogType {
            override val category: AnalyticsLogCategory get() = this
            override val displayName: String = "Log"
            override val description: String = "General informational log entry"
            override val icon: ImageVector
                get() = Icons.AutoMirrored.Filled.Notes
        }
    }
}
