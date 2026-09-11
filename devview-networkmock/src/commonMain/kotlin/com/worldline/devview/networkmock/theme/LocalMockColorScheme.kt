package com.worldline.devview.networkmock.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.luminance
import co.touchlab.kermit.Logger as KermitLogger

/**
 * CompositionLocal for providing the [MockColorScheme] used by the Network Mock module's UI.
 *
 * Provide this where your app already configures its `MaterialTheme`, so the status-family
 * palette switches alongside your light/dark theme:
 *
 * ```kotlin
 * MaterialTheme(colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()) {
 *     CompositionLocalProvider(
 *         LocalMockColorScheme provides if (darkTheme) MockColorScheme.Dark else MockColorScheme.Light
 *     ) {
 *         // ... DevView content ...
 *     }
 * }
 * ```
 *
 * If never provided, the module falls back to [MockColorScheme.Light]/[MockColorScheme.Dark]
 * chosen from the ambient `MaterialTheme`'s surface luminance, and logs a one-time warning
 * pointing at this CompositionLocal (see the "Theming" guide).
 *
 * @see MockColorScheme
 */
public val LocalMockColorScheme: ProvidableCompositionLocal<MockColorScheme?> =
    staticCompositionLocalOf { null }

// ponytail: plain top-level flag, not thread-safe under concurrent first compositions.
// Guards a diagnostic log line, not app state — a rare duplicate warning is harmless.
private var hasWarnedMissingMockColorScheme = false

/**
 * Resolves the [MockColorScheme] to use: [LocalMockColorScheme] if provided, otherwise a
 * best-effort guess derived from the ambient `MaterialTheme`'s surface luminance.
 *
 * The luminance-based guess intentionally does not use `isSystemInDarkTheme()` — DevView
 * renders inside the host app's `MaterialTheme`, which may be driven by something other
 * than the system setting (e.g. a feature flag), so the theme actually in effect is the
 * only reliable signal.
 */
@Composable
@ReadOnlyComposable
internal fun rememberMockColorScheme(): MockColorScheme {
    val provided = LocalMockColorScheme.current
    if (provided != null) return provided

    if (!hasWarnedMissingMockColorScheme) {
        hasWarnedMissingMockColorScheme = true
        KermitLogger.w(tag = "DevViewNetworkMock") {
            "LocalMockColorScheme was never provided; falling back to a palette guessed " +
                "from the current theme's surface luminance. Provide LocalMockColorScheme " +
                "at your app's MaterialTheme site to fix this — see the Theming guide."
        }
    }

    return if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
        MockColorScheme.Dark
    } else {
        MockColorScheme.Light
    }
}
