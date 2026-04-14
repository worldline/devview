# Changelog
All notable changes to this project are documented in this file.

## [0.0.1-SNAPSHOT] - 2026-03-27

### Added
- `devview`: core DevView framework with module registry DSL, section-based home screen, type-safe Navigation3 integration, and per-destination top-app-bar metadata/actions.
- `devview-featureflip`: feature flag tooling with local and remote features, tri-state remote override support, DataStore-backed persistence, and a built-in Compose management screen.
- `devview-analytics`: in-app analytics log capture with typed log events, real-time Compose UI, highlighted log categories, and clear-log action support.
- `devview-networkmock-core`: shared network mock engine with JSON configuration loading (`mocks.json`), endpoint/request matching (including path parameters), mock response discovery/loading, and persisted endpoint/global state.
- `devview-networkmock`: DevView UI module for network mocking with host tabs, global mock toggle, endpoint state controls, and reset-to-network behavior.
- `devview-networkmock-ktor`: Ktor client plugin that intercepts requests, resolves endpoint mock state, returns synthetic HTTP responses when configured, and falls back to real network calls on misses/errors.
- `devview-utils`: shared multiplatform utilities for DataStore setup (`createDataStore`), reusable `DataStoreDelegate`, and `RequiresDataStore` initialization contract.

### Changed
- Refactored module architecture to support destination-level metadata (screen titles and action buttons) across DevView modules.
- Standardized DataStore initialization flow in `rememberModules`, allowing modules and plugin-facing components to share persistent state safely.
- Improved module UIs (home, analytics, feature flags, and network mock) with cleaner layouts and more actionable controls.
- Expanded Dokka API documentation and module samples for public APIs.

### Quality
- Added and expanded unit test coverage for all primary devview-prefixed modules (`devview`, `devview-analytics`, `devview-featureflip`, `devview-networkmock`, `devview-networkmock-core`, and `devview-utils`).
- Added architecture/consistency checks and coverage wiring (`kover`) across the module set.

### Notes
- Current version in `gradle.properties` is `0.0.1-SNAPSHOT`.
- `devview-networkmock-ktor` currently has no dedicated `commonTest` source set; functional behavior is primarily validated via shared core logic and integration path usage.
