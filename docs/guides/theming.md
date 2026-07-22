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
