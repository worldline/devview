# NetworkMock Workflows

This page covers common developer workflows for integrating and using the NetworkMock modules.

## Overview
NetworkMock supports a variety of workflows for mocking network requests, managing configuration, and integrating with UI and Ktor plugin.

## Workflows
### Adding a New Mock Endpoint
1. Define the endpoint in the core module's configuration.
2. Update the UI to display and manage the new endpoint.
3. Configure mock responses as needed.

### Enabling/Disabling Mocks
- Use the UI toggle to enable/disable global or per-endpoint mocking.
- Update state via repositories for programmatic control.

### Resetting Mocks
- Use the UI reset option to revert all endpoints to real network behavior.
- Alternatively, reset state via core repositories.

### Integrating UI and Ktor Plugin
- Ensure both modules reference the same DataStoreDelegate.
- UI changes are reflected in Ktor plugin behavior automatically.

## Best Practices
- Initialize core module before using UI or Ktor plugin.
- Keep mock configuration up-to-date for accurate testing.
- Use workflows to streamline development and testing.

## Troubleshooting / FAQ
- **Why aren't my workflow changes reflected in the app?**
  - Ensure all modules are referencing the same core and DataStoreDelegate.
- **How do I debug mock configuration issues?**
  - Use the UI to inspect and update configuration/state.

## Related Modules
- [NetworkMock](networkmock.md): Overview and usage.
- [NetworkMock Core](networkmock-core.md): Shared configuration/state.
- [NetworkMock UI](networkmock-ui.md): UI for managing mocks.
- [NetworkMock Ktor](networkmock-ktor.md): Ktor plugin for HTTP interception.
- [FeatureFlip](featureflip.md): Combine with feature flags for advanced testing.

---

*API reference is available via Dokka or in your IDE.*
