---
name: update-docs
description: Update Zensical and Dokka documentation when adding or changing a DevView module
---

You are updating the DevView documentation. The docs site is built with Zensical (an MkDocs fork) and API docs are generated with Dokka. Three files must stay synchronized — missing any one means the new module silently doesn't appear in the published site.

## The 3 coordinated changes

### 1. Create or update `docs/modules/<module>.md`

For a **new module**, create the file. Use `docs/modules/featureflip.md` or `docs/modules/analytics.md` as templates. The file should cover:

```markdown
# Module Name

Brief description of what the module does and when to use it.

## Setup

### Dependencies

```kotlin
// build.gradle.kts
implementation("com.worldline.devview:devview-modulename:VERSION")
```

### Registration

```kotlin
val modules = rememberModules {
    register(MyFeatureModule)
}
```

## Usage

How to use the module from the host app's perspective.

## Public API

Key public classes, objects, and functions. Link to API Reference for full details.

## Configuration

Any constructor parameters or configuration options.
```

For an **API change** to an existing module, update the corresponding `docs/modules/<module>.md` and check if `docs/guides/module-development.md` needs updating too.

**Known doc debt to fix if you encounter it:**
- `docs/guides/module-development.md` shows `persistentListOf` for `destinations` — this is wrong. The real API is `PersistentMap<KClass<out NavKey>, DestinationMetadata>` with `persistentMapOf()`.
- `docs/contributing/development.md` shows incorrect build commands. The actual host test command is `.\gradlew.bat cleanTestAndroidHostTest testAndroidHostTest -Pandroidx.baselineprofile.skipgeneration`.
- `README.md` is still KMP Wizard boilerplate — not customized for DevView.

### 2. Add nav entry in `zensical.toml`

The nav array in `zensical.toml` is explicit — new pages must be registered here or they won't appear in the site navigation (even if the file exists).

**For a simple module** (single page), add under the `Modules` section:
```toml
{ "Modules" = [
  "modules/index.md",
  { "FeatureFlip" = "modules/featureflip.md" },
  { "Analytics" = "modules/analytics.md" },
  { "MyFeature" = "modules/myfeature.md" },   # ← add here
  ...
]},
```

**For a multi-part module family** (like NetworkMock with Core/UI/Ktor), nest it:
```toml
{ "MyFeature" = [
  { "Overview" = "modules/myfeature.md" },
  { "Core" = "modules/myfeature-core.md" },
  { "Ktor Plugin" = "modules/myfeature-ktor.md" },
]},
```

### 3. Add `dokka()` dependency in `internal/dokka/build.gradle.kts`

Dokka only generates API docs for modules listed as dependencies here:

```kotlin
dependencies {
    dokka(projects.devview)
    dokka(projects.devviewAnalytics)
    dokka(projects.devviewFeatureflip)
    dokka(projects.devviewNetworkmock)
    dokka(projects.devviewNetworkmockCore)
    dokka(projects.devviewNetworkmockKtor)
    dokka(projects.devviewUtils)
    dokka(projects.devviewMyfeature)   // ← add here
}
```

The project accessor name follows Gradle's camelCase convention from the module's name:
- `devview-myfeature` → `projects.devviewMyfeature`
- `devview-myfeature-core` → `projects.devviewMyfeatureCore`

## Doc build pipeline

The full pipeline (from `scripts/build_docs.sh`):
1. `./gradlew :internal:dokka:dokkaGenerate` — generates API docs
2. Copies `internal/dokka/build/dokka/html/` → `docs/api/`
3. Copies `CHANGELOG.md` → `docs/changelog.md`
4. Runs `zensical build --clean`

To build docs locally (requires Python):
```shell
pip install zensical
./scripts/build_docs.sh build
```

The output is in `site/`. If you don't have Python/Zensical, at minimum verify:
- `docs/modules/<module>.md` exists and is well-formed Markdown
- `zensical.toml` nav entry is syntactically correct TOML
- `internal/dokka/build.gradle.kts` compiles (`.\gradlew.bat :internal:dokka:dokkaGenerate -Pandroidx.baselineprofile.skipgeneration`)

## When does this apply?

| Action | Doc changes needed |
|--------|--------------------|
| New module | All 3 changes |
| New public API in existing module | Update `docs/modules/<module>.md` |
| Changed Module interface contract | Update `docs/guides/module-development.md` |
| New Ktor plugin variant | New `docs/modules/<module>-ktor.md` + zensical.toml + dokka |
| Renamed or removed public API | Update docs + check for stale examples |
| New release | `CHANGELOG.md` auto-copies via build script |
