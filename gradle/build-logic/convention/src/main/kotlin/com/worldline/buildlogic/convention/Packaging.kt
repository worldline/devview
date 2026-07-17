package com.worldline.buildlogic.convention

import com.android.build.api.dsl.Packaging

fun Packaging.configureDefaultExcludes() {
    // Exclude licenses and signatures, but preserve Kotlin module metadata
    resources.excludes.add("META-INF/LICENSE*")
    resources.excludes.add("META-INF/*.SF")
    resources.excludes.add("META-INF/*.RSA")
    resources.excludes.add("**/attach_hotspot_windows.dll")
}