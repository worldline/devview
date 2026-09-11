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

### Section-Derived Defaults (v0.1.4+)

Since v0.1.4, `containerColor` and `contentColor` on `Module` default to the
module's `Section` palette colors. Each section has its own brand-palette tint:

| Section | Container Color | Content Color |
|---------|----------------|---------------|
| `SETTINGS` | `#545AAE` | `#E8E9F7` |
| `FEATURES` | `#764DD0` | `#E8E9F7` |
| `NETWORK` | `#5571B2` | `#E8E9F7` |
| `LOGGING` | `#6970CA` | `#E8E9F7` |
| `CUSTOM` | `#A03CBC` | `#E8E9F7` |

You only need to override `containerColor`/`contentColor` if your module requires a color outside its section's default.

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

## Network Mock Status Colors

Like the Console Logger's `LogColorScheme`, the Network Mock module's per-status-family colors
(2xx green, 4xx/5xx red, etc.) are **not** derived from `MaterialTheme.colorScheme` — a mocked
2xx response needs to read as "success" regardless of your app's brand colors. `MockColorScheme.Light`
and `MockColorScheme.Dark` are two complete, hand-tuned palettes chosen for contrast in each theme.

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
