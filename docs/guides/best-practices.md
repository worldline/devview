# Best Practices for DevView

Recommended patterns and practices for using DevView.

> _[Placeholder: Insert diagram or screenshot illustrating best practices in DevView usage. Use a device frame if relevant.]_

## General Principles
- Keep modules focused and single-purpose
- Use type-safe navigation and state management
- Document all public APIs and module interfaces
- Test modules on all supported platforms (Android/iOS)
- Use MaterialTheme for consistent theming
- Prefer complete code snippets in documentation

## Module Organisation
1. **Group by Section**: Organise modules logically
2. **Naming**: Use clear, descriptive names
3. **Documentation**: Document all modules

## Integration
- Register all modules in `rememberModules` before rendering DevView
- Use feature flags to control experimental or unstable features
- Integrate analytics and network mocking for comprehensive debugging
- Keep module registration conditional for debug/release builds

## State Management
- Use proper Compose state management
- Persist important state with DataStore
- Handle configuration changes

## Performance
- Load modules lazily when possible
- Avoid heavy operations in UI
- Use proper composable keys
- Clear analytics and network logs periodically during long sessions
- Use lazy loading for large lists or data sets

## Security
- Debug builds only
- Don't expose sensitive data
- Validate all inputs

## Testing
- Test feature flag states
- Verify analytics events
- Test on both platforms

## Code Quality
- Follow Kotlin conventions
- Use KDoc comments
- Run detekt and lint
- Keep module code well-organised and documented
- Use version control and code reviews for all changes
- Update documentation as modules evolve

## Theming
- Inherit app theme using MaterialTheme
- Customise colours, typography, and icons for clarity and accessibility
- Preview modules in different theme modes
- Use accessible colour contrasts

## Troubleshooting
- Refer to [Troubleshooting & FAQ](../getting-started/troubleshooting-faq.md) for common issues
- Use platform-specific notes for Android/iOS differences

## Next Steps
- See [Common Pitfalls](common-pitfalls.md) for mistakes to avoid
- Explore [Examples](../examples/index.md) for practical usage

## API Reference
> _[API reference available via Dokka. Add direct link here when available.]_
