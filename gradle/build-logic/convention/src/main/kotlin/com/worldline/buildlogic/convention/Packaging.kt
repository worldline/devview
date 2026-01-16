package com.worldline.buildlogic.convention

import com.android.build.api.dsl.Packaging

fun Packaging.configureDefaultExcludes() {
    resources.excludes.add("META-INF/**")
    resources.excludes.add("META-INF/*.kotlin_module")
    resources.excludes.add("**/attach_hotspot_windows.dll")
}