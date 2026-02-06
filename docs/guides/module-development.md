# Module Development Guide
In-depth guide for creating DevView modules.
## Module Structure
A DevView module consists of:
1. **Destinations** - Navigation screens
2. **Serializers** - For type-safe navigation  
3. **Content** - Composable UI
4. **Metadata** - Name, icon, section
## Creating a Module
See [Creating Custom Modules](../modules/custom-modules.md) for complete guide.
## Architecture Best Practices
- Keep modules focused and single-purpose
- Use proper state management
- Document all public APIs
- Provide preview samples
- Test on both platforms
## Example
```kotlin
object MyModule : Module {
    override val section = Section.CUSTOM
    override val destinations = persistentListOf(MyDestination.Main)
    override val registerSerializers = { /* ... */ }
    override fun EntryProviderScope<NavKey>.registerContent(...) { /* ... */ }
}
```
