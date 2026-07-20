# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Project Is

DevView is a Kotlin Multiplatform (Android + iOS) developer tool library. When integrated into a host app, it provides an in-app overlay developer menu exposing pluggable modules for debugging: feature flags, analytics inspection, and network mocking.

## Commands

On Windows, use `.\gradlew.bat` instead of `./gradlew`. Always append `-Pandroidx.baselineprofile.skipgeneration` to Android build/test tasks. In PowerShell, quote the flag to prevent it being parsed as a switch parameter: `"-Pandroidx.baselineprofile.skipgeneration"`.

**Build:**
```shell
./gradlew :sample:androidApp:assembleDebug -Pandroidx.baselineprofile.skipgeneration
```

**Unit/host tests (JVM, no device):**
```shell
./gradlew cleanTestAndroidHostTest testAndroidHostTest -Pandroidx.baselineprofile.skipgeneration
```

**Single test class:**
```shell
./gradlew :<module>:testAndroidHostTest --tests "com.worldline.devview.<package>.<ClassName>" -Pandroidx.baselineprofile.skipgeneration
```

**Device/instrumented tests (requires emulator or device):**
```shell
./gradlew connectedAndroidDeviceTest -Pandroidx.baselineprofile.skipgeneration
```

**Lint (Detekt + ktlint):**
```shell
./gradlew detektFull
```

**Architecture tests (Konsist):**
```shell
./gradlew :konsist:test
```

**Code coverage (Kover):**
```shell
./gradlew :devview:koverXmlReport :devview:koverLog
```

**Docs (local preview):**
```shell
pip install zensical==0.0.50
zensical serve
```

**Docs (full build including Dokka API docs):**
```shell
./scripts/build_docs.sh build
```

## Architecture

### Module Graph

```
devview-utils          (DataStore contracts, platform createDataStore)
    ↑
devview                (core: Module interface, ModuleRegistry DSL, DevView composable, Navigation3 host)
    ├── devview-analytics        (analytics log capture + Compose UI)
    ├── devview-featureflip      (feature flag management + Compose UI)
    └── devview-networkmock      (network mock UI)
            ↑
devview-networkmock-core  (mock engine: JSON config, request matching, DataStore state)
            ↑
devview-networkmock-ktor  (Ktor client plugin intercepting requests via networkmock-core)

devview-test           (shared test utilities: FakePreferencesDataStore, Turbine wrappers)
konsist/               (Konsist architecture enforcement tests)
sample/                (sample Android app showing full integration)
internal/dokka         (Dokka aggregation for published API docs)
```

### Key Concepts

**`Module` interface** (`devview`): Each feature module implements `Module`, declares `@Serializable` Navigation3 destination keys, and registers Compose content via `registerContent`. The `DevView` composable hosts all modules in a Navigation3 back-stack.

**`ModuleRegistry` DSL** (`devview`): Host apps use `buildModules { }` / `rememberModules { }` to assemble modules into a registry. Modules are grouped by `Section` on the home screen.

**`RequiresDataStore`** (`devview-utils`): Modules needing DataStore persistence implement this interface. `rememberModules` calls `initDataStore()` before `initModule()` automatically.

**Network mock engine** (`devview-networkmock-core`): Reads `mocks.json` from resources, matches requests, persists enabled/disabled state via DataStore. The Ktor plugin (`devview-networkmock-ktor`) intercepts requests client-side.

**Architecture enforcement** (`konsist`): Tests enforce dependency rules — feature modules must depend on `devview` core, each feature family must have exactly one `-core` sub-module, `devview-utils` must not import from `devview`.

### Tech Stack

- **Language:** Kotlin 2.x, targeting Android (minSdk 26) and iOS
- **UI:** Compose Multiplatform + Material3
- **Navigation:** JetBrains Navigation3 (type-safe with kotlinx.serialization)
- **Networking:** Ktor 3.x
- **Persistence:** AndroidX DataStore preferences
- **Testing:** Kotest, Turbine (Flow), MockK, Mokkery
- **Lint:** Detekt with ktlint and compose rules (config in `config/quality/detekt/`)
- **Build:** Gradle convention plugins in `gradle/build-logic/convention/`

## Convention Plugins

All library modules apply convention plugins from `gradle/build-logic/convention/` rather than configuring Kotlin/Compose/test targets directly. Key plugins: `MultiplatformLibraryConventionPlugin`, `ComposeMultiplatformConventionPlugin`, `UnitTestConventionPlugin`, `DeviceTestConventionPlugin`, `KoverConventionPlugin`.

## Git Workflow

### Branch naming

Use `<type>/<description>` where `type` is one of the semantic commit types enforced by CI:

```
feat  fix  docs  chore  refactor  test  ci  perf  build  revert
```

Examples: `feat/networkmock-endpoint-preview`, `fix/datastore-init-crash`, `chore/update-dependencies`.

### PR titles

PR titles **must** follow `type: description` format (e.g. `refactor: remove dead endpoint selection state`). The `semantic-title` job in `.github/workflows/pr-hygiene.yml` enforces this using `amannn/action-semantic-pull-request`. GitHub auto-suggests a title from the branch name — always verify it matches the `type: description` pattern before opening the PR.

### Commit messages

Individual commits use **gitmoji** format: `:emoji: message` (e.g. `:fire: Remove dead code`, `:memo: Update docs`). This is separate from the PR title convention — PRs are squash-merged using the PR title as the commit message.

## Publishing

Library group: `com.worldline.devview`. Version and Sonatype config are in `gradle.properties`. Publishing is handled by the `publish.yml` GitHub Actions workflow and `scripts/release.sh`.

## Changelog

`CHANGELOG.md` at the repo root is the single source of truth. `docs/changelog.md` is built from it automatically — only edit the root file.
