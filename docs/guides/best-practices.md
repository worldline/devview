# Best Practices
Recommended patterns and practices for using DevView.
## Module Organization
1. **Group by Section**: Organize modules logically
2. **Naming**: Use clear, descriptive names
3. **Documentation**: Document all modules
## State Management
- Use proper Compose state management
- Persist important state with DataStore
- Handle configuration changes
## Performance
- Load modules lazily when possible
- Avoid heavy operations in UI
- Use proper composable keys
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
