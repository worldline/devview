package com.worldline.buildlogic.convention

/**
 * This is shared between :app and :benchmarks module to provide configurations type safety.
 */
enum class ProjectBuildType(val applicationIdSuffix: String? = null) {
    DEBUG(applicationIdSuffix = ".debug"),
    RELEASE
}
