# Theming Guide

Customising the appearance of DevView.

## Overview
DevView is designed to inherit your app's theme, using MaterialTheme and Compose best practices. This ensures a consistent look and feel across all modules and platforms.

## Step-by-Step: Adapting to App Theme
DevView uses MaterialTheme colour schemes and typography by default:
```kotlin
MaterialTheme.colorScheme.primary
MaterialTheme.typography.bodyLarge
```

## Customizing Colors
Override colors for modules or components as needed:
```kotlin
object MyModule : Module {
    override val containerColor: Color = Color(0xFFFF5722) // Deep Orange
    override val contentColor: Color = Color.White
    // ...other properties...
}
```

### Section hues and host-theme tones

Module `containerColor` and `contentColor` default to `Color.Unspecified` and resolve at render time.
Each section keeps a distinct hue, blended 25% toward the host primary hue along the shortest arc:
Settings 235°, Features 265°, Network 215°, Logging 175°, Custom 35°.
With HSL saturation 45%, light-theme tiles use container lightness 90% and icon lightness 20%;
dark-theme tiles use 28% and 88%. The current background luminance determines the mode,
not the device setting, so a feature-flag-driven theme also works.

Explicit module colour overrides still win. With only a container override, the unspecified icon
colour resolves to contrasting black or white. When both are explicit, check their contrast.
The old `Section.containerColor` and `Section.contentColor` extensions are removed in alpha08;
use module overrides rather than a fixed section palette.
Section headings use `displayName` and the host primary colour. Home tile container and icon
colours animate when their resolved values change; the surrounding host theme is not animated.

Layout, filter bars and empty states are shared across modules through the [UI kit](ui-components.md).

## Console Log Level Colors

Unlike most of DevView, the Console Logger module's per-[`LogLevel`](../modules/consolelogger.md)
colors (a debug teal, a warning yellow, an error red, etc.) are **not** derived from
`MaterialTheme.colorScheme` — they are two complete, hand-tuned palettes (`LogColorScheme.Light`
and `LogColorScheme.Dark`) chosen for contrast and consistency regardless of your app's brand
colors.

Provide the palette where you already configure your app's `MaterialTheme`, so it switches
alongside your light/dark theme:

```kotlin
MaterialTheme(colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()) {
    CompositionLocalProvider(
        LocalLogColorScheme provides if (darkTheme) LogColorScheme.Dark else LogColorScheme.Light
    ) {
        // ... DevView content ...
    }
}
```

Override individual levels with `copy()`:

```kotlin
LogColorScheme.Dark.copy(
    error = LogColorScheme.Dark.error.copy(label = Color.Magenta)
)
```

**If `LocalLogColorScheme` is never provided**, the console logs a one-time warning (tag
`DevViewConsole`) and falls back to `LogColorScheme.Light`/`LogColorScheme.Dark` guessed from
the ambient `MaterialTheme`'s surface luminance — usually correct, but the warning is your cue
to wire up `LocalLogColorScheme` explicitly. The fallback deliberately does not use
`isSystemInDarkTheme()`, since DevView's theme may be driven by something other than the
system setting (e.g. a feature flag), and the theme actually in effect is the only reliable
signal.

Console rows use neutral host-theme typography and surfaces; semantic colour is limited
to severity badges and filter dots. Both built-in palettes have label-contrast tests of
at least 4.5:1 against their reference surfaces. Custom colours and host surfaces still
need their own contrast checks. Warning labels are darker in light mode; light debug/error
and dark error/assert labels have also been adjusted.

## Network Mock Status Colors

Like the Console Logger's `LogColorScheme`, the Network Mock module's per-status-family colors
(2xx green, 4xx/5xx red, etc.), the network pass-through state, and simulated-failure state are
**not** derived from `MaterialTheme.colorScheme` — a mocked 2xx response needs to read as
"success" regardless of your app's brand colors. `MockColorScheme.Light`
and `MockColorScheme.Dark` are two complete, hand-tuned palettes chosen for contrast in each theme.
Dark status chips use deep containers and light labels. All eight container/label pairs in
both built-in palettes have tests for at least 4.5:1 text contrast; custom overrides need
their own checks. Network pass-through rows use quiet host-theme text instead of a colored
chip, while HTTP method badges keep their host-theme roles.

Provide the palette where you already configure your app's `MaterialTheme`, so it switches
alongside your light/dark theme:

```kotlin
MaterialTheme(colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()) {
    CompositionLocalProvider(
        LocalMockColorScheme provides if (darkTheme) MockColorScheme.Dark else MockColorScheme.Light
    ) {
        // ... DevView content ...
    }
}
```

Override individual status families with `copy()`:

```kotlin
MockColorScheme.Dark.copy(
    serverError = MockColorScheme.Dark.serverError.copy(content = Color.Magenta)
)
```

**If `LocalMockColorScheme` is never provided**, the module logs a one-time warning (tag
`DevViewNetworkMock`) and falls back to `MockColorScheme.Light`/`MockColorScheme.Dark` guessed
from the ambient `MaterialTheme`'s surface luminance — usually correct, but the warning is your
cue to wire up `LocalMockColorScheme` explicitly. As with the console, the fallback deliberately
does not use `isSystemInDarkTheme()`, since DevView's theme may be driven by something other
than the system setting.

### Response comparison colours

`MockResponseDiffColors()` selects light or deep-dark lavender and bisque pairs from the
ambient `MaterialTheme.colorScheme.surface` luminance, independently of the system theme.
Left and right sides keep their colour identity in both palettes. The default container/label
pairs meet 4.5:1 text contrast; callers can override each colour independently and should
check contrast against their own surfaces. Both inline and split comparisons share these
colours.

## Customising Typography
Use your app's typography settings in custom modules:
```kotlin
Text("DevView", style = MaterialTheme.typography.titleLarge)
```

## Customising Icons
Use Compose icons or your own vector assets:
```kotlin
override val icon = Icons.Default.Build
```

## Best Practices
- Use MaterialTheme for all custom modules
- Test on both light and dark themes
- Use accessible colour contrasts
- Preview modules in different theme modes

## Troubleshooting
- **Colours not matching app theme?** Ensure you use MaterialTheme properties and override them as needed.
- **Typography issues?** Use MaterialTheme.typography for consistency.
- **Icon rendering problems?** Check vector asset compatibility and Compose version.

## Next Steps
- See [Module Development](module-development.md) for more on customisation.
- Explore [Examples](../examples/index.md) for themed module samples.

## API Reference
> _[Dokka API Reference](../api/index.html)_
