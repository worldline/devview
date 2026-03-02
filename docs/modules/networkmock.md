# NetworkMock Module

A Kotlin Multiplatform library for mocking network requests and responses, with a built-in UI and Ktor plugin for flexible development and testing.

> _[Placeholder: Insert screenshot of NetworkMock UI. Use a device frame if relevant.]_

## Overview
NetworkMock enables developers to simulate API behaviour, test offline flows, and control network responses at runtime. It consists of three modules:
- **Core**: Shared configuration and state management
- **UI**: Compose-based interface for toggling and managing mocks
- **Ktor Plugin**: HTTP interception for Ktor clients

## Features
- Mock network requests and responses
- UI for toggling global and per-endpoint mocks
- Ktor plugin for HTTP interception
- Persistent configuration/state
- Multiplatform support (Android/iOS)

## Installation
Add to your `build.gradle.kts`:
```kotlin
dependencies {
    implementation(projects.devviewNetworkmock)
    implementation(projects.devviewNetworkmockKtor) // For Ktor plugin
}
```

## Integration Example
Add NetworkMock to your DevView modules list:
```kotlin
val modules = rememberModules {
    module(NetworkMock)
    // ...other modules...
}
```

## Quick Start
### Using the UI
```kotlin
// Add NetworkMockScreen to your Compose navigation
NetworkMockScreen(...)
```
### Using the Ktor Plugin
```kotlin
val client = HttpClient {
    install(NetworkMockPlugin) {
        config = ... // Provide NetworkMockConfig
    }
}
```

## Usage
- Use the UI to toggle mocks and select responses.
- Use the Ktor plugin to intercept requests in your client.
- Configuration/state is shared via the core module.

## Customisation
> _[Placeholder: Guide for customising NetworkMock UI and behaviour. This section will be expanded in future updates.]_

## Advanced Usage
> _[Placeholder: Advanced scenarios such as workflows, custom mock engines, and platform-specific customisation.]_

## Best Practices
1. Initialise the core module before using UI or Ktor plugin.
2. Keep mock configuration up to date for accurate testing.
3. Use workflows to streamline development and QA.

## Troubleshooting / FAQ
- **Why isn't my mock being applied?**
  - Ensure the Ktor plugin is installed and configuration matches the endpoint.
  - Check DataStore initialisation and permissions on both Android and iOS.
- **How do I reset all mocks?**
  - Use the UI reset option or reset state via the core repository.
- **Can I use this on iOS?**
  - Yes, Compose and Ktor Multiplatform are supported. Ensure platform-specific initialisation is correct.
- **Integration issues?**
  - Verify module is included in your DevView modules list and dependencies are present.

## Related Modules
- [FeatureFlip](featureflip.md): Combine with feature flags for advanced testing.
- [Analytics](analytics.md): Monitor analytics during mock scenarios.
- [Creating Custom Modules](custom-modules.md)

## API Reference
> _[API reference available via Dokka. Add direct link here when available.]_

---

*API reference is available via Dokka or in your IDE.*
