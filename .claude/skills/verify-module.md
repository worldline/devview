---
name: verify-module
description: Run a focused health check on a specific DevView module after making changes
---

You are running a focused verification on a single module. This is faster than the full local-ci suite and useful after changing a specific module.

## Step 0: Identify the module

If not already known, check recent changes:
```shell
git diff --name-only HEAD
```

The module is the top-level directory prefix of the changed files (e.g. `devview-analytics/...` → module is `:devview-analytics`).

## Step 1: Module-specific tests

```shell
.\gradlew.bat :<module>:testAndroidHostTest -Pandroidx.baselineprofile.skipgeneration
```

Example:
```shell
.\gradlew.bat :devview-analytics:testAndroidHostTest -Pandroidx.baselineprofile.skipgeneration
```

To run a single test class within the module:
```shell
.\gradlew.bat :<module>:testAndroidHostTest --tests "com.worldline.devview.<pkg>.<ClassName>" -Pandroidx.baselineprofile.skipgeneration
```

## Step 2: Architecture rules (project-wide, fast)

```shell
.\gradlew.bat :konsist:test -Pandroidx.baselineprofile.skipgeneration
```

Konsist always runs fresh and takes ~15 seconds. It catches: package name violations, dependency graph violations, Compose visibility violations, ViewModel placement violations.

## Step 3: Lint

```shell
.\gradlew.bat detektFull -Pandroidx.baselineprofile.skipgeneration
```

## Step 4: Compile check

```shell
.\gradlew.bat :sample:androidApp:assembleDebug -Pandroidx.baselineprofile.skipgeneration
```

This is the definitive compile check — it builds everything and links the sample app. Run this if you changed public API, added new destinations, or modified the module's `build.gradle.kts`.

## Step 5: Coverage delta (optional)

```shell
.\gradlew.bat :devview:koverLog -Pandroidx.baselineprofile.skipgeneration
```

Check the output for the module's coverage percentage. Compare against the value before your change to see if new code is covered.

## Quick reference: which steps to run

| Change type | Steps needed |
|-------------|-------------|
| Bug fix inside a single class | 1, 3 |
| New public function/class | 1, 2, 3, 4 |
| New Module or destination | 1, 2, 3, 4 |
| Renamed/moved file | 2, 3, 4 |
| `build.gradle.kts` change | 4 |
| Documentation only | None (check with `scripts/build_docs.sh build` if available) |
