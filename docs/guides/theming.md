# Theming Guide
Customizing the appearance of DevView.
## Module Customization
Customize your module's appearance:
```kotlin
object MyModule : Module {
    override val moduleName = "My Tool"
    override val icon = Icons.Default.Build
    override val containerColor = Color(0xFFFF5722)
    override val contentColor = Color.White
    override val subtitle = "Custom tool"
}
```
## Material Design 3
DevView uses Material Design 3. Your modules inherit the app's theme.
## Colors
- **containerColor**: Icon background
- **contentColor**: Icon color
- Use theme colors for consistency
## Icons
Use Material Icons or custom ImageVectors.
```kotlin
override val icon = Icons.Rounded.BugReport
```
