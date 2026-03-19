package com.worldline.devview.konsist

import com.lemonappdev.konsist.api.Konsist
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Verifies inter-module dependency rules for the DevView module family.
 *
 * Module naming convention:
 *  - `devview`                          → core base module
 *  - `devview-utils`                    → independent utility module (no devview dependencies)
 *  - `devview-<feature>`                → feature module, must depend on `devview` core
 *  - `devview-<feature>-<identifier>`   → feature sub-module, must depend on `devview-<feature>-core`
 *
 * Rules for feature families (modules sharing the same `<feature>` name):
 *  - There must be exactly ONE `-core` sub-module per feature family
 *  - `devview-<feature>` must depend on `devview-<feature>-core`
 *  - `devview-<feature>-<identifier>` must depend on `devview-<feature>-core`
 */
class ModuleDependencyTest : FunSpec(body = {
    test(name = "DevView feature modules depend on DevView core module") {
        val featureModules = devviewFeatureModuleNames()
            .filter { !it.removePrefix(DEVVIEW_MODULE_PREFIX).contains("-") }

        featureModules.forEach { moduleName ->
            val hasImportFromCore = Konsist.scopeFromModule(moduleName)
                .files
                .any { file ->
                    file.imports.any { import ->
                        import.name.startsWith(BASE_PACKAGE) &&
                            !import.name.startsWith(UTILS_PACKAGE)
                    }
                }
            withClue("'$moduleName' has no file importing from the core '$BASE_PACKAGE' package") {
                hasImportFromCore.shouldBeTrue()
            }
        }
    }

    test(name = "Each DevView feature family has exactly one -core sub-module") {
        val allModules = devviewFeatureModuleNames()
        val subModules = allModules.filter { it.removePrefix(DEVVIEW_MODULE_PREFIX).contains("-") }

        subModules
            .mapNotNull { featureNameOf(it) }
            .distinct()
            .forEach { featureName ->
                val coreModule = "$DEVVIEW_MODULE_PREFIX$featureName-$CORE_IDENTIFIER"
                allModules shouldContain coreModule
                subModules.count { it == coreModule } shouldBe 1
            }
    }

    test(name = "DevView feature modules depend on their -core sub-module") {
        val allModules = devviewFeatureModuleNames()
        val featureModules = allModules.filter { !it.removePrefix(DEVVIEW_MODULE_PREFIX).contains("-") }

        featureModules.forEach { moduleName ->
            val featureName = featureNameOf(moduleName) ?: return@forEach
            val coreModule = "$DEVVIEW_MODULE_PREFIX$featureName-$CORE_IDENTIFIER"
            if (coreModule !in allModules) return@forEach

            val corePackage = "$BASE_PACKAGE.$featureName"
            val hasImportFromCoreModule = Konsist.scopeFromModule(moduleName)
                .files
                .any { file -> file.imports.any { import -> import.name.startsWith(corePackage) } }
            withClue("'$moduleName' has no file importing from core sub-module package '$corePackage'") {
                hasImportFromCoreModule.shouldBeTrue()
            }
        }
    }

    test(name = "DevView feature sub-modules depend on their -core sub-module") {
        val allModules = devviewFeatureModuleNames()
        val subModules = allModules.filter { it.removePrefix(DEVVIEW_MODULE_PREFIX).contains("-") }

        subModules
            .filter { !it.endsWith("-$CORE_IDENTIFIER") }
            .forEach { moduleName ->
                val featureName = featureNameOf(moduleName) ?: return@forEach
                val coreModule = "$DEVVIEW_MODULE_PREFIX$featureName-$CORE_IDENTIFIER"
                if (coreModule !in allModules) return@forEach

                val corePackage = "$BASE_PACKAGE.$featureName"
                val hasImportFromCoreModule = Konsist.scopeFromModule(moduleName)
                    .files
                    .any { file -> file.imports.any { import -> import.name.startsWith(corePackage) } }
                withClue("'$moduleName' has no file importing from core sub-module package '$corePackage'") {
                    hasImportFromCoreModule.shouldBeTrue()
                }
            }
    }

    test(name = "DevView utils module does not depend on any DevView module") {
        val hasImportFromDevView = Konsist.scopeFromModule(DEVVIEW_UTILS_MODULE)
            .files
            .any { file ->
                file.imports.any { import ->
                    import.name.startsWith(BASE_PACKAGE) &&
                        !import.name.startsWith(UTILS_PACKAGE)
                }
            }
        withClue("'$DEVVIEW_UTILS_MODULE' must not import from '$BASE_PACKAGE'") {
            hasImportFromDevView.shouldBeFalse()
        }
    }
})
