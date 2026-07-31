---
name: local-ci
description: Run the full CI check suite locally, mirroring the GitHub Actions build.yml pipeline
---

You are running the full local CI suite. This mirrors the 5 key jobs in `.github/workflows/build.yml`. Run each step sequentially and stop at the first failure — later steps are meaningless if earlier ones fail.

All commands use `.\gradlew.bat` on Windows (or `./gradlew` on Unix/macOS). All build/test commands require `-Pandroidx.baselineprofile.skipgeneration`.

## Step 1: Lint (fastest — catches style issues before wasting compile time)

```shell
.\gradlew.bat detektFull -Pandroidx.baselineprofile.skipgeneration
```

**What it checks:** Detekt static analysis + ktlint formatting + Compose-specific rules.
Key thresholds: MaxLineLength 120, LongMethod 60 lines (Composables excluded), ReturnCount 3, CyclomaticComplexMethod 20 (Composables excluded).

If this fails, fix lint issues before proceeding.

## Step 2: Architecture rules

```shell
.\gradlew.bat :konsist:test -Pandroidx.baselineprofile.skipgeneration
```

**What it checks:** Package naming, module dependency graph, ViewModel placement, Compose visibility rules. Always runs fresh (`upToDateWhen { false }`).

## Step 3: Unit and host tests

```shell
.\gradlew.bat cleanTestAndroidHostTest testAndroidHostTest -Pandroidx.baselineprofile.skipgeneration
```

**What it checks:** All `commonTest` and `androidHostTest` tests across every module. The `clean` prefix ensures stale results don't mask new failures.

To narrow to a specific module:
```shell
.\gradlew.bat :devview-myfeature:testAndroidHostTest -Pandroidx.baselineprofile.skipgeneration
```

To narrow to a single test class:
```shell
.\gradlew.bat :devview-myfeature:testAndroidHostTest --tests "com.worldline.devview.myfeature.MyTest" -Pandroidx.baselineprofile.skipgeneration
```

## Step 4: Full build (compilation + sample app)

```shell
.\gradlew.bat :sample:androidApp:assembleDebug -Pandroidx.baselineprofile.skipgeneration
```

**What it checks:** Full compilation of all library modules and the sample app. Catches serializer registration errors and KMP compatibility issues that unit tests don't reach.

## Step 5: Coverage (optional, run when checking coverage impact)

```shell
.\gradlew.bat :devview:koverXmlReport :devview:koverLog -Pandroidx.baselineprofile.skipgeneration
```

**What it checks:** Kover aggregates coverage across all library modules via the dependency in `devview/build.gradle.kts`.

## What's NOT included here

- **Device tests** (`connectedAndroidDeviceTest`) — require a running emulator. Run separately when modifying Compose UI or instrumented test code. CI runs these on API 26, 29, and 35 matrix.
- **Docs build** (`scripts/build_docs.sh build`) — requires Python + `pip install zensical`. Run separately when modifying documentation.
- **Publish** (`./gradlew publish`) — never run this locally without explicit intent; it pushes to Sonatype.

## Interpreting CI vs local differences

If CI fails but local passes:
1. Check if it's a device test (those are CI-only)
2. Check the baseline profile flag — CI always passes it, local might not
3. Check for environment differences (Java version via `java -version`)

If local fails but CI passes: you likely have stale caches. Run `.\gradlew.bat clean` first.
