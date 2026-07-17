# Changelog
All notable changes to this project are documented in this file.

## Unreleased

## [0.1.2] - 2026-07-17

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
