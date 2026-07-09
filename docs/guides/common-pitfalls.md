# Common Pitfalls in DevView

Avoid these common mistakes when integrating and extending DevView. This guide provides troubleshooting tips and best practices to help you build robust developer tools.

> _[Placeholder: Insert diagram or screenshot illustrating common mistakes and solutions. Use a device frame if relevant.]_

## Module Registration
- Forgetting to register modules in `rememberModules`.
- Registering duplicate modules or conflicting names.
- Not updating the modules list after adding new modules.

## Dependency Management
- Missing required dependencies in `build.gradle.kts`.
- Version mismatches between DevView modules and Compose/Kotlin.
- Not syncing Gradle after changes.

## State Management
- Not using Compose state correctly for DevView visibility.
- Failing to persist important state with DataStore.
- Not handling configuration changes (e.g., rotation).

## Navigation
- Incorrect or missing serialiser registration for destinations.
- Not wiring `onNavigateBack` or `onNavigate` callbacks.
- Navigation keys not implementing `NavKey`.

## Theming
- Not using MaterialTheme for colours and typography.
- Poor colour contrast or accessibility issues.
- Icons not rendering due to unsupported vector assets.

## Platform-Specific Issues
- Not testing on both Android and iOS.
- DataStore or Compose compatibility problems on iOS.
- Not handling platform-specific initialisation.

## Performance
- Heavy operations in UI composables.
- Not clearing analytics or network logs during long sessions.
- Large lists not using lazy loading.

## Security
- Exposing sensitive data in debug modules.
- Not restricting DevView to debug builds.

## Troubleshooting
- Refer to [Troubleshooting & FAQ](../getting-started/troubleshooting-faq.md) for solutions to common issues.
- Use platform-specific notes for Android/iOS differences.

## Next Steps
- Review [Best Practices](best-practices.md) for recommended patterns.
- Explore [Examples](../examples/index.md) for practical usage.

## API Reference
> _[API reference available via Dokka. Add direct link here when available.]_

