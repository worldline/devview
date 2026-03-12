package com.worldline.devview.analytics.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents highlighted analytics log data for display in the analytics dashboard.
 *
 * This sealed interface defines the structure for analytics log summaries that can be
 * highlighted in the UI, such as total counts or counts by specific log types. It is
 * designed to be immutable for safe use in Compose and other concurrent contexts.
 *
 * ## Implementations
 * - [Total]: Represents the total count of all analytics logs.
 * - [Type]: Represents the count of analytics logs for a specific log type.
 *
 * @see AnalyticsLogType
 */
@Immutable
public sealed interface HighlightedAnalyticsLog {
    public val count: Int
    public val icon: ImageVector

    public val containerColor: Color
        @Composable @ReadOnlyComposable
        get

    public val contentColor: Color
        @Composable @ReadOnlyComposable
        get

    @Immutable
    public data class Total(override val count: Int) : HighlightedAnalyticsLog {
        override val icon: ImageVector
            get() = Icons.AutoMirrored.Rounded.List

        override val containerColor: Color
            @Composable @ReadOnlyComposable
            get() = MaterialTheme.colorScheme.surfaceVariant

        override val contentColor: Color
            @Composable @ReadOnlyComposable
            get() = MaterialTheme.colorScheme.onSurfaceVariant
    }

    @Immutable
    public data class Type(val type: AnalyticsLogType, override val count: Int) :
        HighlightedAnalyticsLog {
        override val icon: ImageVector
            get() = type.icon

        override val containerColor: Color
            @Composable @ReadOnlyComposable
            get() = type.category.containerColor

        override val contentColor: Color
            @Composable @ReadOnlyComposable
            get() = type.category.contentColor
    }
}
