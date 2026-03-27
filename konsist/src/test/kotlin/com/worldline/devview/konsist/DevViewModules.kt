package com.worldline.devview.konsist

import com.lemonappdev.konsist.api.Konsist

internal const val DEVVIEW_BASE_MODULE = "devview"
internal const val DEVVIEW_UTILS_MODULE = "devview-utils"
internal const val DEVVIEW_MODULE_PREFIX = "devview-"
internal const val CORE_IDENTIFIER = "core"
internal const val BASE_PACKAGE = "com.worldline.devview"
internal const val UTILS_PACKAGE = "com.worldline.devview.utils"

/** Extracts `<feature>` from `devview-<feature>` or `devview-<feature>-<identifier>`. */
internal fun featureNameOf(moduleName: String): String? {
    if (!moduleName.startsWith(DEVVIEW_MODULE_PREFIX)) return null
    val withoutPrefix = moduleName.removePrefix(DEVVIEW_MODULE_PREFIX)
    if (withoutPrefix.isEmpty() || withoutPrefix == CORE_IDENTIFIER) return null
    // Parts after the prefix, e.g. "networkmock-core" → ["networkmock", "core"]
    return withoutPrefix.split("-").first()
}

/**
 * Returns all devview feature module names discovered from the project scope,
 * excluding the base (`devview`) and utils (`devview-utils`) modules.
 */
internal fun devviewFeatureModuleNames(): List<String> =
    Konsist.scopeFromProject()
        .files
        .map { it.moduleName }
        .distinct()
        .filter { name ->
            name.startsWith(DEVVIEW_MODULE_PREFIX) &&
                name != DEVVIEW_UTILS_MODULE
        }

/**
 * Returns the expected package prefix for a given devview module name by converting
 * the module name to a package name: `com.worldline.<module-name-with-hyphens-as-dots>`.
 *
 *  - `devview`                        → `com.worldline.devview`
 *  - `devview-utils`                  → `com.worldline.devview.utils`
 *  - `devview-<feature>`              → `com.worldline.devview.<feature>`
 *  - `devview-<feature>-<identifier>` → `com.worldline.devview.<feature>.<identifier>`
 */
internal fun expectedPackagePrefixOf(moduleName: String): String =
    "com.worldline.${moduleName.replace("-", ".")}"
