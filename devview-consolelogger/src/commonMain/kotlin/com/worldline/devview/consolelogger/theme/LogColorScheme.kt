package com.worldline.devview.consolelogger.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.worldline.devview.consolelogger.model.LogLevel

/**
 * The set of colors used to render a single [LogLevel] in the console UI.
 *
 * @property label Color used for the log level's text/label, and for the message text of
 *           log lines at this level.
 * @property selectedContainer Background color of the level's filter chip when selected.
 * @property selectedContent Content (icon/label) color of the level's filter chip when selected.
 * @property unselectedContent Content (icon/label) color of the level's filter chip when
 *           not selected.
 *
 * @see LogColorScheme
 */
@Immutable
public data class LogLevelColors(
    public val label: Color,
    public val selectedContainer: Color,
    public val selectedContent: Color,
    public val unselectedContent: Color
)

/**
 * The complete set of per-[LogLevel] colors used by the Console module's UI.
 *
 * DevView renders inside the host app's `MaterialTheme`, but log-level colors (a debug
 * teal, a warning yellow, an error red, etc.) are intentionally not derived from
 * `MaterialTheme.colorScheme` — they need to stay visually distinct and consistent
 * regardless of the host's brand palette. [Light] and [Dark] are complete, hand-tuned
 * palettes tested for contrast in each theme; use [copy] to override individual levels.
 *
 * ## Usage
 *
 * Provide the scheme where your app already configures its `MaterialTheme`, via
 * [com.worldline.devview.consolelogger.theme.LocalLogColorScheme]:
 *
 * ```kotlin
 * MaterialTheme(colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()) {
 *     CompositionLocalProvider(
 *         LocalLogColorScheme provides if (darkTheme) LogColorScheme.Dark else LogColorScheme.Light
 *     ) {
 *         // ...
 *     }
 * }
 * ```
 *
 * ### Overriding individual levels
 * ```kotlin
 * LogColorScheme.Dark.copy(
 *     error = LogColorScheme.Dark.error.copy(label = Color.Magenta)
 * )
 * ```
 *
 * @property verbose Colors for [LogLevel.VERBOSE].
 * @property debug Colors for [LogLevel.DEBUG].
 * @property info Colors for [LogLevel.INFO].
 * @property warning Colors for [LogLevel.WARNING].
 * @property error Colors for [LogLevel.ERROR].
 * @property assert Colors for [LogLevel.ASSERT].
 * @property unknown Colors for [LogLevel.UNKNOWN].
 *
 * @see LogLevelColors
 * @see LocalLogColorScheme
 */
@Immutable
public data class LogColorScheme(
    public val verbose: LogLevelColors,
    public val debug: LogLevelColors,
    public val info: LogLevelColors,
    public val warning: LogLevelColors,
    public val error: LogLevelColors,
    public val assert: LogLevelColors,
    public val unknown: LogLevelColors
) {
    /**
     * Returns the [LogLevelColors] for the given [level].
     */
    public operator fun get(level: LogLevel): LogLevelColors = when (level) {
        LogLevel.VERBOSE -> verbose
        LogLevel.DEBUG -> debug
        LogLevel.INFO -> info
        LogLevel.WARNING -> warning
        LogLevel.ERROR -> error
        LogLevel.ASSERT -> assert
        LogLevel.UNKNOWN -> unknown
    }

    public companion object {
        /**
         * The default light-theme palette.
         */
        public val Light: LogColorScheme = LogColorScheme(
            verbose = LogLevelColors(
                label = Color(color = 0xFF616161),
                selectedContainer = Color(color = 0xFFEBEBEB),
                selectedContent = Color(color = 0xFF262626),
                unselectedContent = Color(color = 0xFF262626)
            ),
            debug = LogLevelColors(
                label = Color(color = 0xFF1C7474),
                selectedContainer = Color(color = 0xFFAFE9E9),
                selectedContent = Color(color = 0xFF0D3030),
                unselectedContent = Color(color = 0xFF0D3030)
            ),
            info = LogLevelColors(
                label = Color(color = 0xFF1D7524),
                selectedContainer = Color(color = 0xFFB7ECBA),
                selectedContent = Color(color = 0xFF103C13),
                unselectedContent = Color(color = 0xFF103C13)
            ),
            warning = LogLevelColors(
                label = Color(color = 0xFF716706),
                selectedContainer = Color(color = 0xFFEFEBC2),
                selectedContent = Color(color = 0xFF494304),
                unselectedContent = Color(color = 0xFF494304)
            ),
            error = LogLevelColors(
                label = Color(color = 0xFFBE4225),
                selectedContainer = Color(color = 0xFFF1CAC1),
                selectedContent = Color(color = 0xFF532013),
                unselectedContent = Color(color = 0xFF532013)
            ),
            assert = LogLevelColors(
                label = Color(color = 0xFF932999),
                selectedContainer = Color(color = 0xFFEDC3EF),
                selectedContent = Color(color = 0xFF432145),
                unselectedContent = Color(color = 0xFF432145)
            ),
            unknown = LogLevelColors(
                label = Color(color = 0xFF000000),
                selectedContainer = Color(color = 0xFFD9D9D9),
                selectedContent = Color(color = 0xFF000000),
                unselectedContent = Color(color = 0xFF000000)
            )
        )

        /**
         * The default dark-theme palette.
         */
        public val Dark: LogColorScheme = LogColorScheme(
            verbose = LogLevelColors(
                label = Color(color = 0xFFB3B3B3),
                selectedContainer = Color(color = 0xFFD1D1D1),
                selectedContent = Color(color = 0xFF262626),
                unselectedContent = Color(color = 0xFFD1D1D1)
            ),
            debug = LogLevelColors(
                label = Color(color = 0xFF36C9C9),
                selectedContainer = Color(color = 0xFF72D9D9),
                selectedContent = Color(color = 0xFF0D3030),
                unselectedContent = Color(color = 0xFF72D9D9)
            ),
            info = LogLevelColors(
                label = Color(color = 0xFF36C940),
                selectedContainer = Color(color = 0xFF72D979),
                selectedContent = Color(color = 0xFF103C13),
                unselectedContent = Color(color = 0xFF72D979)
            ),
            warning = LogLevelColors(
                label = Color(color = 0xFFF3E225),
                selectedContainer = Color(color = 0xFFF6E856),
                selectedContent = Color(color = 0xFF272402),
                unselectedContent = Color(color = 0xFFF6E856)
            ),
            error = LogLevelColors(
                label = Color(color = 0xFFDE846D),
                selectedContainer = Color(color = 0xFFDE846D),
                selectedContent = Color(color = 0xFF3E180E),
                unselectedContent = Color(color = 0xFFDE846D)
            ),
            assert = LogLevelColors(
                label = Color(color = 0xFFDA86DF),
                selectedContainer = Color(color = 0xFFDA86DF),
                selectedContent = Color(color = 0xFF432145),
                unselectedContent = Color(color = 0xFFDA86DF)
            ),
            unknown = LogLevelColors(
                label = Color(color = 0xFFFFFFFF),
                selectedContainer = Color(color = 0xFFFFFFFF),
                selectedContent = Color(color = 0xFF000000),
                unselectedContent = Color(color = 0xFFFFFFFF)
            )
        )
    }
}
