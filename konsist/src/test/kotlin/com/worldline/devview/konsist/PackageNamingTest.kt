package com.worldline.devview.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.FunSpec

/**
 * Verifies package naming conventions across all DevView modules.
 */
class PackageNamingTest : FunSpec(body = {
    test(name = "DevView module files use the correct package prefix") {
        Konsist.scopeFromProject()
            .files
            .map { it.moduleName }
            .distinct()
            .filter { it.startsWith(DEVVIEW_BASE_MODULE) }
            .forEach { moduleName ->
                val expectedPrefix = expectedPackagePrefixOf(moduleName)
                Konsist.scopeFromModule(moduleName)
                    .files
                    .assertTrue(additionalMessage = "Files in '$moduleName' must use package prefix '$expectedPrefix'") { file ->
                        file.packagee?.name?.startsWith(expectedPrefix) ?: true
                    }
            }
    }

    test(name = "Package name does not contain upper case characters") {
        Konsist
            .scopeFromProject()
            .files
            .assertTrue { file ->
                file.packagee?.name?.let { packageName ->
                    !packageName.any { it.isUpperCase() }
                } ?: true
            }
    }

    test(name = "Package name does not contain underscores") {
        Konsist
            .scopeFromProject()
            .files
            .assertTrue { file ->
                file.packagee?.name?.let { packageName ->
                    !packageName.any { it == '_' }
                } ?: true
            }
    }
})
