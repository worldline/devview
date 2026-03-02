# NetworkMock Core

The core module (`devview-networkmock-core`) provides shared logic and persistent storage for network mocking.

## Overview
- Stores mock configuration and state using a process-level DataStoreDelegate.
- Exposes repositories for managing mock settings and responses.
- Ensures consistent access to configuration/state across UI and Ktor plugin modules.

## Features
- Singleton DataStore for storing preferences
- Singleton object for initializing and exposing repositories
- Repository pattern for configuration/state

## Usage
- Initialize the core module before using UI or Ktor plugin.
- Access repositories via `NetworkMockInitializer`.
- DataStoreDelegate ensures all modules operate on the same data.

## Best Practices
- Always initialize the core before using UI or Ktor plugin.
- Use the provided repositories for all configuration/state changes.

## Troubleshooting / FAQ
- **Why aren't my mocks persisting?**
  - Ensure you are using the shared DataStoreDelegate and not a separate instance.
- **How do I reset all state?**
  - Use repository methods to clear or reset configuration/state.

## Related Modules
- [NetworkMock](networkmock.md): Overview and usage.
- [NetworkMock UI](networkmock-ui.md): UI for managing mocks.
- [NetworkMock Ktor](networkmock-ktor.md): Ktor plugin for HTTP interception.

---

*API reference is available via Dokka or in your IDE.*
