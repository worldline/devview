package com.worldline.buildlogic.convention

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Configure Android SDK versions and settings for a Kotlin Multiplatform library.
 */
internal fun Project.configureAndroidMultiplatformLibrary() {
    extensions.configure<KotlinMultiplatformExtension> {
        extensions.configure<KotlinMultiplatformAndroidLibraryExtension> {
            compileSdk = Versions.COMPILE_SDK
            minSdk = Versions.MIN_SDK

            packaging {
                configureDefaultExcludes()
            }
        }
    }
}
