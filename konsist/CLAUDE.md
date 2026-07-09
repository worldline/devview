# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Purpose

This module contains Konsist architecture-enforcement tests for the entire DevView project. Tests run against all source files discovered at test time — they are not unit tests for this module's own logic.

`outputs.upToDateWhen { false }` is set intentionally so Gradle never skips these tests as up-to-date, since the inputs are the full project source tree rather than this module's own sources.

## Enforced Rules

### Package Naming (`PackageNamingTest`)

- Every file in a `devview-*` module must use the package prefix derived from its module name: hyphens become dots, e.g. `devview-networkmock-core` → `com.worldline.devview.networkmock.core`.
- Package names must be all lowercase with no underscores.

### Module Dependencies (`ModuleDependencyTest`)

- `devview-<feature>` modules (direct feature modules, no second hyphen segment) must import from the `com.worldline.devview` base package.
- `devview-utils` must not import anything from `com.worldline.devview` (except its own `com.worldline.devview.utils` package).
- Any feature family that has sub-modules must have exactly one `-core` sub-module (e.g. `devview-networkmock-core`).
- `devview-<feature>` and `devview-<feature>-<identifier>` modules must import from `com.worldline.devview.<feature>` (the core sub-module's package).

### ViewModel Conventions (`ViewModelTest`)

- Classes whose name ends in `ViewModel` must live in a package ending with `.viewmodel`.
- Such classes must have `public` (or default) visibility.

### Compose Conventions (`ComposeTest`)

- `@Composable` functions in any `..components..` package must be `internal` (not public).
- `@Preview` composable functions must be `private`.
- Classes ending in `PreviewParameterProvider` (except those in `devview-utils`) must be `internal` and live in a package ending with `.preview`.

## Adding a New Rule

1. Create a new file in `src/test/kotlin/com/worldline/devview/konsist/`, e.g. `MyNewTest.kt`.
2. Extend `FunSpec` — the same Kotest style used by all existing tests.
3. Use `Konsist.scopeFromProject()` as the default scope, or `Konsist.scopeFromModule(name)` to target a specific module.
4. Use Konsist's `assertTrue` / `assertFalse` with `additionalMessage` to give readable failure output. For assertions that go through Kotest matchers directly (as in `ModuleDependencyTest`), wrap with `withClue { }` instead.
5. Shared module-naming helpers and constants live in `DevViewModules.kt` — use `devviewFeatureModuleNames()`, `featureNameOf()`, and `expectedPackagePrefixOf()` rather than duplicating the naming logic.

## Non-Obvious Patterns

- **Dynamic discovery:** Module names are not hard-coded; `devviewFeatureModuleNames()` queries `Konsist.scopeFromProject()` at runtime. Adding a new `devview-*` module to the project automatically includes it in dependency checks.
- **Reserved utility modules:** `devview-utils` and `devview-test` are excluded from feature-module dependency checks via `RESERVED_UTILITY_MODULES` in `DevViewModules.kt`.
- **`KotestUtils.kt`** exposes `TestScope.koTestName` for accessing the current test case name inside a `FunSpec` body — available if a new rule needs to include the test name in a diagnostic message.
- **Detekt runs here too:** The module applies Detekt with the same shared config used by the rest of the project (`config/quality/detekt/`). This lints the Konsist test code itself.
