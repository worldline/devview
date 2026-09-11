package com.worldline.devview.networkmock.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.worldline.devview.networkmock.core.model.StatusCodeFamily

/**
 * The container/content color pair used to render a single [StatusCodeFamily] (or the
 * [MockColorScheme.network] pass-through state) as a chip in the Network Mock UI.
 *
 * @property container Background color of the chip.
 * @property content Foreground color (icon/label) of the chip.
 *
 * @see MockColorScheme
 */
@Immutable
public data class StatusColors(public val container: Color, public val content: Color)

/**
 * The complete set of colors used to render HTTP status families and the network
 * pass-through state in the Network Mock module's UI.
 *
 * DevView renders inside the host app's `MaterialTheme`, but these colors are intentionally
 * not derived from `MaterialTheme.colorScheme` — a 2xx chip needs to read as "success" and a
 * 4xx/5xx chip as "error" regardless of the host's brand palette. [Light] and [Dark] are
 * complete, hand-tuned palettes tested for contrast in each theme; use [copy] to override
 * individual families.
 *
 * ## Usage
 *
 * Provide the scheme where your app already configures its `MaterialTheme`, via
 * [com.worldline.devview.networkmock.theme.LocalMockColorScheme]:
 *
 * ```kotlin
 * MaterialTheme(colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()) {
 *     CompositionLocalProvider(
 *         LocalMockColorScheme provides if (darkTheme) MockColorScheme.Dark else MockColorScheme.Light
 *     ) {
 *         // ...
 *     }
 * }
 * ```
 *
 * ### Overriding individual families
 * ```kotlin
 * MockColorScheme.Dark.copy(
 *     serverError = MockColorScheme.Dark.serverError.copy(content = Color.Magenta)
 * )
 * ```
 *
 * @property informational Colors for [StatusCodeFamily.INFORMATIONAL] (1xx).
 * @property successful Colors for [StatusCodeFamily.SUCCESSFUL] (2xx).
 * @property redirection Colors for [StatusCodeFamily.REDIRECTION] (3xx).
 * @property clientError Colors for [StatusCodeFamily.CLIENT_ERROR] (4xx).
 * @property serverError Colors for [StatusCodeFamily.SERVER_ERROR] (5xx).
 * @property unknown Colors for [StatusCodeFamily.UNKNOWN].
 * @property network Colors for the network pass-through state (no mock active).
 *
 * @see StatusColors
 * @see LocalMockColorScheme
 */
@Immutable
public data class MockColorScheme(
    public val informational: StatusColors,
    public val successful: StatusColors,
    public val redirection: StatusColors,
    public val clientError: StatusColors,
    public val serverError: StatusColors,
    public val unknown: StatusColors,
    public val network: StatusColors
) {
    /**
     * Returns the [StatusColors] for the given [family].
     */
    public operator fun get(family: StatusCodeFamily): StatusColors = when (family) {
        StatusCodeFamily.INFORMATIONAL -> informational
        StatusCodeFamily.SUCCESSFUL -> successful
        StatusCodeFamily.REDIRECTION -> redirection
        StatusCodeFamily.CLIENT_ERROR -> clientError
        StatusCodeFamily.SERVER_ERROR -> serverError
        StatusCodeFamily.UNKNOWN -> unknown
    }

    public companion object {
        /**
         * The default light-theme palette.
         */
        public val Light: MockColorScheme = MockColorScheme(
            informational = StatusColors(
                container = Color(color = 0xFFB7DCEC),
                content = Color(color = 0xFF184559)
            ),
            successful = StatusColors(
                container = Color(color = 0xFFB7ECBA),
                content = Color(color = 0xFF103C13)
            ),
            redirection = StatusColors(
                container = Color(color = 0xFFF0CAA7),
                content = Color(color = 0xFF603610)
            ),
            clientError = StatusColors(
                container = Color(color = 0xFFECB7B7),
                content = Color(color = 0xFF6F1111)
            ),
            serverError = StatusColors(
                container = Color(color = 0xFFECB7E6),
                content = Color(color = 0xFF611A59)
            ),
            unknown = StatusColors(
                container = Color(color = 0xFFD1D1D1),
                content = Color(color = 0xFF3D3D3D)
            ),
            network = StatusColors(
                container = Color(color = 0xFFABC4ED),
                content = Color(color = 0xFF0D1F3A)
            )
        )

        /**
         * The default dark-theme palette.
         */
        public val Dark: MockColorScheme = MockColorScheme(
            informational = StatusColors(
                container = Color(color = 0xFF70BAD9),
                content = Color(color = 0xFF184559)
            ),
            successful = StatusColors(
                container = Color(color = 0xFF70D976),
                content = Color(color = 0xFF103C13)
            ),
            redirection = StatusColors(
                container = Color(color = 0xFFE39C5B),
                content = Color(color = 0xFF603610)
            ),
            clientError = StatusColors(
                container = Color(color = 0xFFD97070),
                content = Color(color = 0xFF500C0C)
            ),
            serverError = StatusColors(
                container = Color(color = 0xFFD970CD),
                content = Color(color = 0xFF45123F)
            ),
            unknown = StatusColors(
                container = Color(color = 0xFFA4A4A4),
                content = Color(color = 0xFF3D3D3D)
            ),
            network = StatusColors(
                container = Color(color = 0xFF6290DD),
                content = Color(color = 0xFF0D1F3A)
            )
        )
    }
}
