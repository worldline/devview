# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- NetworkMock: closed the remaining test-coverage gaps tracked in #91 — real sample specs
  (`sample/network`'s `sample-api.json` and `jsonplaceholder.json`) now parse through the actual
  `MockConfigRepository` in a new `RealSampleSpecTest` (`devview-networkmock-core`,
  `androidHostTest`), guarding against the shipped sample silently drifting out of sync with what
  the parser accepts; query-parameter matching is now exercised end-to-end through the real Ktor
  plugin interception path (`NetworkMockPluginTest`); the operation sheet's sticky-header
  status-family grouping now has explicit coverage (`NetworkMockOperationSheetTest`); and the
  delay-precedence chain (`Operation.delayMs ?: ApiSpec.delayMs ?: null`) now covers its
  previously-untested third case. Ambiguous host-match precedence, the preview/diff bottom sheet,
  and response Content-Type/header assertions were already covered by prior work — verified, not
  duplicated.
- NetworkMock: operations can now declare OpenAPI `tags`, read verbatim into a new
  `Operation.tags: List<String>` (empty by default, display-only — no effect on request
  matching, same as `Operation.version`). The operation list gains a fourth per-tab filter chip
  row (`tag_filter_row`, multi-select, hidden when the current spec has no tagged operations)
  and a sort control — a new "Sort" toolbar dropdown, wired via the shared toolbar's new
  `DestinationMetadataBuilder.menu` action (`NetworkMock` exposes a `sortSharedFlow`,
  `NetworkMockScreen` collects it). Tapping it opens a menu with one entry per sort key: spec
  order (default), path (A-Z), method (`HttpMethod.DefaultMethods` order), and tag (an
  operation's first declared tag); picking an entry sets that sort directly. Sort
  selection is per-tab, plain client-side state in `NetworkMockScreen`'s `ContentState`, not
  the ViewModel — same convention as the existing filters. See
  `docs/modules/networkmock-core.md`'s new "Tags" section and `docs/modules/networkmock-ui.md`.
  (`devview`, `devview-networkmock-core`, `devview-networkmock`, #116, #117)
- DevView: the shared top app bar's contextual actions can now be a dropdown menu of discrete
  choices, not just a single-tap icon or a confirm/cancel popup. `ModuleDestinationAction` gains
  a `menuItems: PersistentList<ModuleDestinationActionMenuItem>?` property (new public class);
  `DestinationMetadataBuilder` gains a `menu(icon) { item(label) { ... } }` DSL alongside the
  existing `action`. Precedence when both `action` and `menu` could apply: `menuItems` wins,
  then `popup`, then the plain `action` lambda. (`devview`, #117)

- NetworkMock: an operation can now declare narrow request-body match constraints — required
  top-level fields and/or a discriminator field's value, read from its
  `requestBody.content.<mediaType>.schema` — to disambiguate operations that would otherwise
  collide on path, method, and query alone (e.g. two specs sharing a host, each declaring
  `POST /api/payments`, differing only by body shape). Not full JSON Schema validation; an
  operation declaring no `requestBody`, or one with neither a required field nor a usable
  discriminator, matches any body, same as before. `Operation` gains `requestBodyMatch:
  RequestBodyMatch?` (new public class); `RequestMatcher` gains `matchesRequestBody`;
  `MockConfigRepository.findMatchingMock` gains an optional `requestBody: String?` parameter.
  `devview-networkmock-ktor`'s plugin reads the request body only when it's already a fully
  in-memory `OutgoingContent.ByteArrayContent` — a pure, repeatable read that never consumes or
  mutates anything the real network call still needs to send. See
  `docs/modules/networkmock-core.md`'s new "Request body matching" section.
  (`devview-networkmock-core`, `devview-networkmock-ktor`, #83)
- NetworkMock: a status code with a declared `content.<mediaType>.schema` but no `examples`
  now synthesizes a placeholder response body instead of being unmockable — primitives, `enum`
  (first value), `object`/`array` (recursively, by declared `type` or by the mere presence of
  `properties`/`items`), `allOf` (properties merged; conflicting definitions across members
  throw a clear error), and `oneOf` (first declared variant; `discriminator` is parsed but
  doesn't yet steer variant selection). Deliberately narrow, not full JSON Schema conformance —
  see `docs/modules/networkmock-core.md`'s new "Schema-based response synthesis" section.
  `MockResponse` gains `isSynthesized: Boolean` (default `false`); the operation picker page
  shows a small "Generated" badge on a synthesized response's row.
  (`devview-networkmock-core`, `devview-networkmock`, #82, #84)
- NetworkMock: an operation can now be configured as a **sequence** — an ordered list of
  responses it advances through one step per matched request, sticking on the last step once
  exhausted rather than looping back to the start. Useful for polling flows (order status,
  upload progress, async job completion) where the interesting behavior is the transition
  across repeated calls. Build one from the operation sheet's new "SEQUENCE" section: tap
  "Build a Sequence", tap responses in the desired order, then "Save Sequence"; "Reset
  Position" restarts at step 1 without leaving the sequence. `OperationMockState` gains a
  `Sequence(responses: List<Mock>, currentIndex: Int)` variant (**breaking**: another new
  sealed subtype); the position is persisted as part of this same state, so resetting an
  operation to `Network` discards it with no special-casing needed.
  `NetworkMockViewModel` gains `setOperationSequenceState`/`resetOperationSequencePosition`.
  (`devview-networkmock-core`, `devview-networkmock-ktor`, `devview-networkmock`, #96)
- NetworkMock: an operation can now simulate a network failure instead of returning a response,
  two ways — **deterministically**, by selecting Timeout or Connection Refused in the operation
  sheet's picker page (a new "Simulate Failure" section, alongside the response variants); or
  **probabilistically**, via a new `x-devview.failureRate` (0.0–1.0) OpenAPI extension field,
  which independently rolls on every otherwise-mocked request to that operation (an operation
  left on `Network` passthrough is never affected). Each failure kind mirrors the exception a
  real Ktor engine throws for the equivalent condition (`HttpRequestTimeoutException` for
  Timeout, a connection-level `kotlinx.io.IOException` for Connection Refused), so existing app
  error handling exercises the same code path. `OperationMockState` gains a `Failure(kind:
  FailureKind)` variant (**breaking**: exhaustive `when` blocks over `OperationMockState` need a
  new branch); `Operation` gains `failureRate: Double?`; `NetworkMockConfig` gains an injectable
  `random: Random` for deterministic tests; `MockColorScheme` gains a `failure: StatusColors`
  slot (**breaking**: new required constructor parameter). (`devview-networkmock-core`,
  `devview-networkmock-ktor`, `devview-networkmock`, #88, #95)
- NetworkMock: mocked responses now serve the `Content-Type` derived from the spec's declared
  media type (`responses.<code>.content.<mediaType>`, previously always hardcoded to
  `application/json`) plus any additional headers declared on `responses.<code>.headers` — a
  header's literal `example` value is served as-is, mirroring how query-parameter matching
  already reads a parameter's `example`. `$ref`'d headers resolve against `components.headers`.
  `MockResponse` gains `contentType` (default `"application/json"`) and `headers` (default
  empty) properties. (`devview-networkmock-core`, `devview-networkmock-ktor`, #87)
- NetworkMock: a "Reload Config" toolbar action, and `MockConfigRepository.invalidate()` /
  `NetworkMockViewModel.reloadConfiguration()`, to re-read and re-parse the configured OpenAPI
  specs without restarting the app — previously the parsed config was cached forever after the
  first load, with no way to pick up an edited spec file short of a process restart. Operations
  added, removed, or renamed in the spec appear immediately after reloading; persisted
  per-operation mock selections are untouched. (`devview-networkmock-core`,
  `devview-networkmock`, #90)

### Changed
- **Breaking:** `MockHttpClientCall` is now `internal` instead of `public` — it was only public
  because Ktor's `HttpClientCall(client)` base constructor required it to be instantiable from
  the plugin's install code, not because integrators have a legitimate reason to construct it
  themselves. Its `rawContent` override already depends on the `@InternalAPI`-annotated Ktor
  API, so staying public compounded that instability onto this library's own tracked surface.
  (`devview-networkmock-ktor`, #89)

### Fixed
- NetworkMock: `OpenApiParser`'s `$ref` resolution now disambiguates by the full
  `components.<section>` a fragment names, not just its trailing name — a `$ref` whose fragment
  points at an unexpected section (e.g. `components/parameters/Foo` where a `components/responses`
  entry was expected) is rejected with a clear error instead of being silently resolved against
  whatever section the call site happened to expect, so a same-named entry in a different section
  can never be conflated with the one actually referenced. `$ref` chains — an entry that itself
  declares another `$ref` — are now followed until a non-ref entry is reached (previously only one
  level deep), guarded against cycles: a circular `$ref` chain now fails with a clear error instead
  of hanging. (`devview-networkmock-core`)
- NetworkMock: replaced ~35 unconditional `println` calls in `MockConfigRepository` and
  `NetworkMockPlugin` with gated [Kermit](https://github.com/touchlab/Kermit) logging
  (tag `DevViewNetworkMock`), consolidating the plugin's multi-line per-request trace into one
  line per intercepted request. No response body content is ever logged. A host app can adjust
  or silence this via `Logger.setMinSeverity(...)`; `devview-consolelogger`, if installed,
  captures it automatically. Detekt's `ForbiddenMethodCall` rule (`println`/`print`) is now
  enforced repo-wide. (`devview-networkmock-core`, `devview-networkmock-ktor`, #86)
- NetworkMock: the bottom bar's search field and expand-filter button were padded as a whole
  `Surface`, pushing every filter chip row above them down by the system navigation bar inset
  as well — the inset now only pads the search field and button themselves, matching
  `AnalyticsScreen`'s existing (correct) layout. (`devview-networkmock`)

## [0.2.0-alpha03] - 2026-09-11

### Added
- NetworkMock status-family chip colors (2xx green, 4xx/5xx red, etc.) are now configurable via
  a `LocalMockColorScheme` CompositionLocal, mirroring `devview-consolelogger`'s
  `LocalLogColorScheme`. Also fixes these colors rendering as washed-out light pastels on dark
  surfaces — `MockColorScheme.Dark` is a proper hand-tuned dark palette rather than the light
  values reused verbatim. (`devview-networkmock`)
- NetworkMock UI: a multi-select HTTP method filter chip row, alongside the existing version
  filter. No chip selected shows every method; selecting one or more narrows the list to
  operations using any of the selected methods, combined with the version filter and search
  via AND. (`devview-networkmock`)
- `HttpMethod`: a value class modeled after Ktor's own `HttpMethod` (open set, companion
  constants, `DefaultMethods` for canonical ordering), replacing the raw `String` on
  `Operation.method` — without adding a Ktor dependency to `devview-networkmock-core`.
  (`devview-networkmock-core`)
- NetworkMock UI: a "Mocked"/"Network" filter chip row, and a mocked-operation count
  (e.g. "3 of 47 mocked") on the global mocking toggle, computed across every spec — both
  answer "what have I left mocked" at a glance. (`devview-networkmock`)

### Changed
- **Breaking:** `Operation.method` is now `HttpMethod` instead of `String`.
  (`devview-networkmock-core`)
- **Breaking:** NetworkMock's operation detail screen is now a bottom sheet over the operation
  list instead of a second navigation destination — tapping an operation opens the response
  picker directly, without leaving the list, and picking a response dismisses the sheet.
  Removed: `NetworkMockDestination.Endpoint`, `NetworkMockEndpointViewModel`,
  `NetworkMockEndpointUiState`, and `NetworkMockScreen`'s `navigateToEndpointScreen` parameter.
  Added to `NetworkMockViewModel`: `sheetState: StateFlow<OperationSheetState>`,
  `openOperation(key)`, `closeSheet()` — response variant discovery (previously
  `NetworkMockEndpointViewModel`'s job) now happens here, once per sheet open. Marking a
  response for preview/compare is now an explicit eye-icon toggle on each row instead of a
  long-press, and the preview/diff view is a second page of the same sheet (reached via a
  "Preview"/"Compare 2 responses" button) rather than a separate bottom sheet stacked on top of
  the detail screen. (`devview-networkmock`)
- NetworkMock UI: the search field and version filter row moved from the top of the screen into
  a `Scaffold` bottom bar (alongside the new method filter), matching FeatureFlip, Analytics, and
  ConsoleLogger's existing layout for one-handed reach. Only the search field and an expand
  chevron are visible by default; the mock-state, version, and method filter rows collapse
  behind the chevron (matching Analytics' bottom bar). (`devview-networkmock`)
- NetworkMock UI: each operation row now shows a leading colour rail (the state chip's colour)
  and a colour-coded HTTP method badge; the row's separate state-summary line — which duplicated
  the status code already shown in the state chip — is gone (#115). The row's per-operation
  version badge is also gone: `Operation.version` is parsed from the very path segment shown
  right next to it, so the badge never showed anything the path wasn't already showing, and it
  crowded the state chip. The path itself no longer truncates — it wraps instead, so a long URL
  is never cut off. The operation sheet's picker-page header shares the same fix, since it shows
  the same operation identity — except the header drops the state chip entirely: the picker
  list right below already marks the active response with a checkmark, its full label, and a
  family icon/colour, so a bare-status-code chip in the header would only repeat that.
  (`devview-networkmock`)

## [0.2.0-alpha02] - 2026-09-11

### Fixed
- Fixed ViewModel persistence during navigation: the `rememberViewModelStoreNavEntryDecorator()` was 
  not applied to the `NavDisplay`'s `entryDecorators` causing `ViewModel`s to persist once instantiated.

## [0.2.0-alpha01] - 2026-09-10

### Added
- `Operation.version`: a display-only version tag extracted from a `/v{n}/` path segment
  (e.g. `/api/v2/x` → `"v2"`), shown as a chip on each operation in the NetworkMock UI. Purely
  a UI label — request matching is unaffected. (`devview-networkmock-core`, `devview-networkmock`)
- NetworkMock UI: a search field (filters by name, path, or operationId) and a per-tab version
  filter row, both client-side over already-loaded data. (`devview-networkmock`)
- A [migration guide](docs/guides/migrating-to-openapi.md) and a `scripts/mocks_json_to_openapi.py`
  conversion script for integrators upgrading from a pre-0.2.0 `mocks.json` config.
- `devview-consolelogger` module: displays the app's native console output (logcat on
  Android, a stdout/stderr redirect on iOS) inside the DevView overlay, with per-level
  filter chips, a text filter, and auto-follow. Capture works on an untethered device
  (no debugger required) on both platforms; an optional `DevViewLogWriter` routes
  Kermit-based logging into the same view, closing the one remaining gap
  (`NSLog`/`os_log` on iOS with no debugger attached). Log-level colors are configurable
  via a `LocalLogColorScheme` CompositionLocal. (`devview-consolelogger`)
- TimeCapsule rows now show a per-state change summary (e.g. `count 3 → 4`) derived from a
  `key=value`-shaped label, expandable to reveal the full label with changed values
  highlighted, plus a wall-clock timestamp alongside the existing delta pill. The screen now
  shows a header naming the recorded owner. (`devview-timecapsule`)

### Changed
- **Breaking:** `devview-networkmock-core` now parses OpenAPI 3.x documents (JSON, and YAML
  on a best-effort basis) instead of the bespoke `mocks.json` format. One spec file is one
  API group — the environment axis is gone entirely; a group's request-matching hosts come
  from the spec's `servers[]` list, and an app talking to two hosts for the same API is
  simply two operations with different paths in one document. `NetworkMock(configPath: String)`
  is now `NetworkMock(specPaths: List<String>)`. See the
  [migration guide](docs/guides/migrating-to-openapi.md). (`devview-networkmock-core`,
  `devview-networkmock`, `devview-networkmock-ktor`)
- Renamed to OpenAPI vocabulary throughout the networkmock modules: `ApiGroupConfig` →
  `ApiSpec`, `EndpointConfig` → `Operation`, `EndpointKey` → `OperationKey` (drops the
  `environmentId` component), `EndpointDescriptor` → `OperationDescriptor`,
  `EndpointMockState` → `OperationMockState`, `GroupEnvironmentUiModel` → `ApiSpecUiModel`,
  `EndpointUiModel` → `OperationUiModel`. `MockResponse` and `MockMatch` are deliberately
  **not** renamed — they model DevView's own runtime mocking behavior, not something OpenAPI
  describes. `EnvironmentConfig`, `EndpointOverride`, and `effectiveEndpoints` are deleted.
- Response variant discovery now reads the spec's declared
  `responses.<code>.content.*.examples` instead of probing status-code/suffix combinations
  against the filesystem. `OperationMockState.Mock` is now keyed by `(statusCode,
  exampleName)` instead of a response file name. (`devview-networkmock-core`)
- Response delay simulation is now declared via the `x-devview.delayMs` OpenAPI
  Specification Extension, at the document root (spec-wide default) and/or per operation
  (overrides the default) — replacing `ApiGroupConfig.defaultDelayMs` /
  `EndpointConfig.delayMs`. (`devview-networkmock-core`)
- DataStore entries written under the pre-0.2.0 key shape
  (`network_mock_endpoint_{groupId}-{environmentId}-{endpointId}`) are wiped once on first
  launch after upgrading — the key shape and the `Mock` payload shape both changed, so a
  translation wasn't attempted. The global mocking toggle is unaffected. (`devview-networkmock-core`)
- `OperationDescriptor` no longer carries `availableResponses` — the main operation list never
  read them, so `NetworkMockViewModel` no longer eagerly discovers and decodes every response
  body on app start. Response variants are now only loaded when an operation's detail screen
  actually opens (`NetworkMockEndpointUiState.Content.responses`). (`devview-networkmock-core`,
  `devview-networkmock`)
- `TimeCapsuleEffect` gains a `subtitle` parameter, inserted between `label` and `maxEntries`.
  Callers passing `maxEntries` positionally must switch to a named argument.
  (`devview-timecapsule`)

## [0.1.5] - 2026-09-08

### Added
- `devview-timecapsule` module: records the state history of the currently visible screen
  via `TimeCapsuleEffect`/`TimeCapsuleOwner`, and lets a developer restore any earlier
  state back into that screen from the DevView overlay. History resets automatically when
  the screen leaves composition.

## [0.1.4] - 2026-07-22

### Changed
- Applied logo-inspired color palette to the sample app: custom light/dark `ColorScheme` using violet/indigo/magenta tones from the DevView brand, including the full `surfaceContainer` tonal ramp. (`sample`)
- Module icon containers on the home screen now use per-section colors derived from the logo palette; icon shape changed from circle to squircle (`RoundedCornerShape(8.dp)`) and module names are now `SemiBold`. Chevron indicator removed — touch ripple is the navigation affordance. (`devview`)
- Section headers on the home screen now use `primary` color instead of `outline`, and the DevView chameleon icon appears as a subtle watermark behind the module list. (`devview`)
- Analytics log items now display a `3dp` leading color strip matching the event category, making the log stream scannable by category at a glance. (`devview-analytics`)
- Feature type labels ("Local" / "Remote") are now rendered as small pill badges instead of plain text. (`devview-featureflip`)
- HTTP method labels on endpoint cards and the endpoint detail header are now rendered as proper badges with a `primaryContainer` background, matching the visual language of API explorer tools. (`devview-networkmock`)
- NetworkMock empty, error, and loading state screens now use Material icons instead of an emoji, with consistent typography and `onSurfaceVariant` text colors. (`devview-networkmock`)
- Added empty state to `FeatureFlipScreen` when the feature list is empty or no features match the active filter. (`devview-featureflip`)
- The endpoint detail hint card now shows a `TouchApp` icon for visual clarity. (`devview-networkmock`)
- Top app bar titles across all DevView screens are now `SemiBold` weight. (`devview`)
- Unified surface backgrounds: the `surfaceContainer` explicit color has been removed from the Analytics highlighted-logs header and the NetworkMock global toggle wrapper — backgrounds now inherit from `MaterialTheme` uniformly. (`devview-analytics`, `devview-networkmock`)
- Diff colors shifted from generic blue to lavender (`#DDD8FF`) to align with the brand palette. (`devview-networkmock`)

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

[Unreleased]: https://github.com/worldline/DevView/compare/0.2.0-alpha03...HEAD
[0.2.0-alpha03]: https://github.com/worldline/DevView/compare/0.2.0-alpha02...0.2.0-alpha03
[0.2.0-alpha02]: https://github.com/worldline/DevView/compare/0.2.0-alpha01...0.2.0-alpha02
[0.2.0-alpha01]: https://github.com/worldline/DevView/compare/0.1.5...0.2.0-alpha01
[0.1.5]: https://github.com/worldline/DevView/compare/0.1.4...0.1.5
[0.1.4]: https://github.com/worldline/DevView/compare/0.1.3...0.1.4
[0.1.3]: https://github.com/worldline/DevView/compare/0.1.2...0.1.3
[0.1.2]: https://github.com/worldline/DevView/compare/0.1.1...0.1.2
[0.1.1]: https://github.com/worldline/DevView/compare/v0.1.0...0.1.1
[0.1.0]: https://github.com/worldline/DevView/releases/tag/v0.1.0
