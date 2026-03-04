package com.worldline.devview.core

import androidx.navigation3.runtime.NavKey

/**
 * Interface for navigation keys that have an associated title.
 *
 * This is used to provide a title for screens in the DevView navigation system.
 * Any NavKey that implements HasTitle can be displayed with a title in the UI.
 *
 * Example usage:
 * ```
 * @Serializable
 * data object MainScreen : HasTitle {
 *     override val title: String
 *        get() = "Main Screen"
 * }
 * ```
 *
 * This allows the navigation system to display "Main Screen" as the title when navigating to this destination.
 *
 * @see NavKey
 */
public interface HasTitle : NavKey {
    public val title: String
}
