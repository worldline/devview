# NetworkMock UI

The UI module (`devview-networkmock`) provides Compose screens and controls for managing network mocks.

## Overview
- Toggle global mocking on/off
- Enable/disable individual endpoint mocks
- Select mock responses for each endpoint
- Reset all mocks to use actual network

## Features
- Compose UI for managing mocks
- Viewmodels and components for endpoint management
- Interacts with core repositories to update configuration/state

## Usage
- Add the NetworkMock screen to your Compose navigation.
- Use provided viewmodels and components for endpoint management.
- UI changes are reflected in the Ktor plugin via shared state.

## Best Practices
- Extend or modify UI components for custom mock management.
- Integrate with other DevView modules as needed.

## Troubleshooting / FAQ
- **Why aren't my UI changes reflected in the Ktor plugin?**
  - Ensure both modules use the same DataStoreDelegate and core initialization.
- **How do I reset all mocks?**
  - Use the UI reset option or clear state via the core repository.

## Related Modules
- [NetworkMock](networkmock.md): Overview and usage.
- [NetworkMock Core](networkmock-core.md): Shared configuration/state.
- [NetworkMock Ktor](networkmock-ktor.md): Ktor plugin for HTTP interception.
- [FeatureFlip](featureflip.md): Combine with feature flags for advanced testing.

---

*API reference is available via Dokka or in your IDE.*
