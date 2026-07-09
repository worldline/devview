# Theming Guide

Customising the appearance of DevView.

> _[Placeholder: Insert screenshot of DevView UI in light and dark themes. Use a device frame if relevant.]_

## Overview
DevView is designed to inherit your app's theme, using MaterialTheme and Compose best practices. This ensures a consistent look and feel across all modules and platforms.

## Step-by-Step: Adapting to App Theme
DevView uses MaterialTheme colour schemes and typography by default:
```kotlin
MaterialTheme.colorScheme.primary
MaterialTheme.typography.bodyLarge
```

## Customising Colours
Override colours for modules or components as needed:
```kotlin
object MyModule : Module {
    override val containerColour = MaterialTheme.colorScheme.secondary
    override val contentColour = MaterialTheme.colorScheme.onSecondary
    // ...other properties...
}
```

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
> _[API reference available via Dokka. Add direct link here when available.]_
