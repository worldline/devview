# NetworkMock Ktor Plugin

The Ktor plugin module (`devview-networkmock-ktor`) enables network mocking for HTTP requests in Ktor clients.

## Overview
- Intercepts HTTP requests and returns mock responses based on configuration.
- Reads configuration/state from the core module.
- Supports flexible mock setup for development and testing.

## Features
- Ktor client plugin for HTTP interception
- Reads configuration/state from the core module
- Supports flexible mock setup for development and testing

## Usage
- Install the plugin in your Ktor client configuration.
- Ensure core module is initialized and accessible.
- Configure mock endpoints and responses via UI or programmatically.

### Example
```kotlin
val client = HttpClient {
    install(NetworkMockPlugin) {
        config = ... // Provide NetworkMockConfig
    }
}
```

## Best Practices
- Ensure the core module is initialized before using the plugin.
- Keep mock configuration up-to-date for accurate testing.
- Use the UI for easy management of mocks.

## Troubleshooting / FAQ
- **Why isn't my mock being applied?**
  - Ensure the plugin is installed and configuration matches the endpoint.
- **How do I reset all mocks?**
  - Use the UI reset option or reset state via the core repository.

## Related Modules
- [NetworkMock](networkmock.md): Overview and usage.
- [NetworkMock Core](networkmock-core.md): Shared configuration/state.
- [NetworkMock UI](networkmock-ui.md): UI for managing mocks.
- [FeatureFlip](featureflip.md): Combine with feature flags for advanced testing.

---

*API reference is available via Dokka or in your IDE.*
