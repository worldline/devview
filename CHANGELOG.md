# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed
- Fixed excessive recompositions and broken Switch animation on `FeatureFlipScreen`: item keys now use the stable `feature.name` instead of `hashCode()`, `FeatureHandler` caches its Flow to prevent `collectAsStateWithLifecycle` from restarting on every recomposition, and redundant explicit `remember` keys have been removed from `derivedStateOf` blocks. (`devview-featureflip`)
- Added `distinctUntilChanged()` to `FeatureHandler.isFeatureEnabledFlow` and `getFeatures` to suppress recompositions when DataStore emits structurally identical values. (`devview-featureflip`)
- Fixed `AnalyticsScreen` `LazyColumn` item key: `log.hashCode()` was replaced by `log.timestamp`, then `log.timestamp` caused a crash because multiple events can share the same millisecond; the key is now the log's original position in the append-only `AnalyticsLogger.logs` list via `withIndex()`. Redundant explicit keys also removed from all `derivedStateOf` blocks. (`devview-analytics`)
- Fixed `HomeScreen` `LazyColumn` using `module.hashCode()` as item key instead of the stable `module.moduleName`. (`devview`)
- Fixed `NetworkMockScreen` using `collectAsState()` instead of `collectAsStateWithLifecycle()`, causing unnecessary state collection when the screen is off-stack or the app is backgrounded. (`devview-networkmock`)
- Added `distinctUntilChanged()` to `MockStateRepository.observeState()` to suppress recompositions triggered by structurally equal `NetworkMockState` emissions from DataStore. (`devview-networkmock-core`)

### Documentation
- Added Compose List Keys rules to the contributing guide (`code-style.md`): LazyColumn/LazyRow keys must be unique, stable under state changes, and semantically meaningful. Added matching item to the PR checklist.

## [0.1.3] - 2026-07-21

### Added
- `NetworkMockResourceLoader` fun interface in `devview-networkmock-core`: provides a named type for DI frameworks (Koin, Hilt, etc.) to bind, eliminating the need for a custom bridge interface in multi-module KMP projects where mock resource files live in a different module than where `NetworkMock` is constructed. Both `devview-networkmock` and `devview-networkmock-ktor` now expose `devview-networkmock-core` as an `api` dependency so the type is available to all integrators.

### Fixed
- Fixed DevView overlay back navigation: the overlay's back handler now correctly yields priority to the host app when closed, preventing it from silently consuming back events.
- Fixed crash on Network Mock screen startup: `MissingResourceException` thrown by Compose Resources when probing absent response files was not caught by the `IllegalStateException` handler in `MockConfigRepository`, causing a fatal crash. The exception is now normalised at the `NetworkMock` boundary before reaching the core module.

## [0.1.2] - 2026-07-17


### Fixed
- Preserved Kotlin module metadata in Android packaging by replacing broad `META-INF/**` excludes with selective license/signature excludes.
- Fixed docs list rendering on Home (`What's New`) and License (`Third-Party Licences`) pages.

### Changed
- Updated sample `DevViewApp` feature setup to include a local feature entry fix and a remote feature example.

## [0.1.1] - 2026-07-16

### Changed
- Downgraded Kotlin from 2.4.10 to 2.3.21 for broader consumer compatibility.
- Removed `compose-stability-analyzer` plugin (KMP incompatible).

### Documentation
- Updated minimum Kotlin version requirement to 2.3.21 in installation guide.
- Added acknowledgment for chrisbanes/haze inspiration.
- Added GitHub issue templates for bugs and feature requests.

### Quality
- Added Metalava API tracking to all published modules.
- Improved CI infrastructure: split workflows to Linux/macOS, added snapshot deploy, replaced `release.sh` with platform-agnostic `release.py`.
- Enabled automatic Maven Central publishing.

## [0.1.0] - 2026-07-15

### Added
- `devview`: core DevView framework with module registry DSL, section-based home screen, type-safe Navigation3 integration, and per-destination top-app-bar metadata/actions.
- `devview-featureflip`: feature flag tooling with local and remote features, tri-state remote override support, DataStore-backed persistence, and a built-in Compose management screen. Feature type badge and filter support for granular feature browsing.
- `devview-analytics`: in-app analytics log capture with typed log events, real-time Compose UI, highlighted log categories, and clear-log action support.
- `devview-networkmock-core`: shared network mock engine with JSON configuration loading (`mocks.json`), endpoint/request matching (including path parameters), mock response discovery/loading, and persisted endpoint/global state.
- `devview-networkmock`: DevView UI module for network mocking with host tabs, global mock toggle, endpoint state controls, and reset-to-network behavior.
- `devview-networkmock-ktor`: Ktor client plugin that intercepts requests, resolves endpoint mock state, returns synthetic HTTP responses when configured, and falls back to real network calls on misses/errors.
- `devview-utils`: shared multiplatform utilities for DataStore setup (`createDataStore`), reusable `DataStoreDelegate`, and `RequiresDataStore` initialization contract.
- `devview-test`: shared multiplatform test utilities including `FakePreferencesDataStore` and Turbine-based Flow assertion helpers.
- Pre-commit hooks enforcing gitleaks secret scanning and Detekt static analysis on every commit.

### Changed
- Refactored module architecture to support destination-level metadata (screen titles and action buttons) across DevView modules.
- Standardized DataStore initialization flow in `rememberModules`, allowing modules and plugin-facing components to share persistent state safely.
- Improved module UIs (home, analytics, feature flags, and network mock) with cleaner layouts and more actionable controls.
- Removed dead endpoint selection state from NetworkMock.
- Updated Kotlin to 2.4.10 and Compose Multiplatform to 2.11.0; bulk transitive dependency upgrades via Renovate.
- Migrated documentation site from MkDocs to Zensical.

### Fixed
- Resolved Dokka unresolved link warnings and broken KDoc links across all modules.
- Fixed Detekt `MaxLineLength` violations in KDoc link references.
- Fixed documentation site rendering issues and stale content.
- Fixed CI configuration to correctly handle Renovate dependency update branches.

### Documentation
- Rewrote README as an integrator quick-start guide; removed TODO.md.
- Added CLAUDE.md guidance files for root and all sub-modules.
- Documented git workflow conventions: branch naming, PR title format, and gitmoji commit style.
- Expanded Dokka API documentation and module samples for all public APIs.
- Added Renovate custom manager to keep documentation version badges in sync with dependency updates.

### Quality
- Added and expanded unit test coverage for all primary `devview-*` modules.
- Added Konsist architecture enforcement tests and Kover coverage reporting across the module set.

[Unreleased]: https://github.com/worldline/DevView/compare/v0.1.3...HEAD
[0.1.3]: https://github.com/worldline/DevView/compare/v0.1.2...v0.1.3
[0.1.2]: https://github.com/worldline/DevView/compare/v0.1.1...v0.1.2
[0.1.1]: https://github.com/worldline/DevView/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/worldline/DevView/releases/tag/v0.1.0
