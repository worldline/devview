---
name: add-konsist-rule
description: Add a Konsist architecture enforcement test to the konsist module
---

You are adding a new Konsist architecture test. Konsist tests run on every CI build and enforce structural constraints across the entire codebase. A new rule should target a real convention the project follows — not a hypothetical concern.

## 0. Understand the existing rules first

Read the existing test files to avoid duplicating or contradicting:
- `konsist/src/test/kotlin/com/worldline/devview/konsist/PackageNamingTest.kt` — package structure
- `konsist/src/test/kotlin/com/worldline/devview/konsist/ModuleDependencyTest.kt` — import restrictions
- `konsist/src/test/kotlin/com/worldline/devview/konsist/ComposeTest.kt` — Composable visibility
- `konsist/src/test/kotlin/com/worldline/devview/konsist/ViewModelTest.kt` — ViewModel placement

Shared helpers in `konsist/src/test/kotlin/com/worldline/devview/konsist/DevViewModules.kt`:
- `devviewFeatureModuleNames()` — names of all feature modules
- `featureNameOf(moduleName)` — extracts feature name from module name
- `expectedPackagePrefixOf(moduleName)` — converts module name to package prefix
- `RESERVED_UTILITY_MODULES` — set of non-feature modules to exclude from feature checks

## 1. Choose the right test file

Add to an existing file if the rule logically fits there. Create a new file only for a genuinely new category.

New file template:
```kotlin
package com.worldline.devview.konsist

import com.lemonappdev.konsist.api.Konsist
import io.kotest.core.spec.style.FunSpec

// Note: Konsist tests DO use FunSpec — this is the only place in the project
class MyNewRuleTest : FunSpec({

    test("all DataStore files have unique filenames across modules") {
        // ...
    }
})
```

## 2. Scoping

```kotlin
// Entire project:
val scope = Konsist.scopeFromProject()

// Single module:
val scope = Konsist.scopeFromModule("devview-analytics")

// Specific source sets:
val scope = Konsist.scopeFromModule("devview-featureflip", sourceSetName = "commonMain")
```

## 3. Common patterns

### Check class names / packages
```kotlin
test("ViewModels live in .viewmodel packages") {
    Konsist.scopeFromProject()
        .classes()
        .filter { it.name.endsWith("ViewModel") }
        .forEach { clazz ->
            clazz.assertTrue(
                additionalMessage = "ViewModel '${clazz.name}' must be in a '.viewmodel' package"
            ) {
                it.packageName?.endsWith(".viewmodel") == true
            }
        }
}
```

### Check visibility modifiers
```kotlin
test("@Composable functions in components packages are internal") {
    Konsist.scopeFromProject()
        .functions()
        .filter { it.hasAnnotationOf<Composable>() }
        .filter { "components" in (it.packageName ?: "") }
        .forEach { fn ->
            fn.assertTrue(
                additionalMessage = "@Composable '${fn.name}' in components must be internal"
            ) {
                it.hasInternalModifier
            }
        }
}
```

### Check import restrictions
```kotlin
test("devview-utils does not import from devview") {
    Konsist.scopeFromModule("devview-utils")
        .files()
        .forEach { file ->
            file.assertFalse(
                additionalMessage = "devview-utils must not depend on devview (found in ${file.path})"
            ) {
                it.imports.any { imp -> imp.name.startsWith("com.worldline.devview.core") }
            }
        }
}
```

### Iterate over feature modules
```kotlin
test("each feature module has exactly one core submodule or none") {
    devviewFeatureModuleNames().forEach { moduleName ->
        // ... check per module ...
    }
}
```

## 4. Key rules

- Use `assertTrue`/`assertFalse` with `additionalMessage` — it makes failures actionable
- Or use Kotest `withClue { clazz.name.shouldEndWith("ViewModel") }` — whichever reads more naturally
- Always verify your new test passes on the current codebase before committing (no false positives)
- Always verify it would catch the violation you're protecting against (no false negatives) — consider adding a comment explaining what exact bad pattern it guards against

## 5. The `outputs.upToDateWhen { false }` setting

Already configured in `konsist/build.gradle.kts` — Konsist tests always run fresh regardless of input changes. Do not add this per-test.

## 6. Verify

```shell
.\gradlew.bat :konsist:test -Pandroidx.baselineprofile.skipgeneration
```

Confirm:
1. The new test appears in output
2. It passes on the current codebase
3. If you can, temporarily introduce the violation locally to confirm it catches it, then revert
