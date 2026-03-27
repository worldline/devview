# Troubleshooting & FAQ

Find solutions to common issues and answers to frequently asked questions when integrating or using DevView.

> _[Placeholder: Insert screenshot or diagram of a typical error message or troubleshooting UI. Use a device frame if relevant.]_

## General Troubleshooting

### DevView not appearing
- Ensure your composable tree includes `DevView`.
- Confirm the `isOpen` state is set to `true` when you expect DevView to be visible.
- Check that all required modules are initialised and included in `rememberModules`.
- For gesture-based opening, verify gesture detection is correctly configured.

### Modules not showing up
- Confirm modules are added to the `rememberModules` block.
- Check that conditional modules meet their requirements (e.g., DEBUG mode, feature flags).
- Verify module dependencies are present in your build.gradle.
- Ensure module initialisation happens before DevView is rendered.
- For Network Mock, ensure `isNetworkMockEnabled` is set and the module is included.
- For custom modules, verify your implementation matches the required interface.

### Build errors
- Make sure you've added the correct dependency for your platform.
- Verify that you're using compatible versions of Kotlin and the library.
- Check that Compose Multiplatform is properly configured.
- Clean and rebuild your project: `./gradlew clean build`

### Styling issues
- Ensure you're using Material3 components and theme.
- Check that custom colours are properly defined.
- Verify containerColour values are valid Colour objects.
- Test on both light and dark themes.

### Feature flags not working
- Verify FeatureFlip module is included in your dependencies.
- Check that feature flag keys match exactly (case-sensitive).
- Ensure remote config has been fetched if using remote sources.
- For testing, consider using local overrides.

## Frequently Asked Questions

### Can I use DevView in production?
DevView is intended for development and debugging purposes. It is not recommended to include DevView in production builds.

### How do I add a custom module?
> _[Placeholder: Add step-by-step guide for custom module integration. This section will be expanded in future updates.]_

### Is DevView compatible with all Compose Multiplatform targets?
DevView supports Android and iOS targets. Other platforms may require additional configuration or are not officially supported yet.

### How do I localise DevView?
Currently, DevView documentation and UI are in English. Localisation support may be added in future releases.

### Where can I find more examples?
See the [Examples section](../examples/index.md) for platform-specific and advanced usage examples.

---

If your issue is not listed here, please consult the [DevView repository](https://github.com/worldline-tech/devview) or open an issue for further assistance.

