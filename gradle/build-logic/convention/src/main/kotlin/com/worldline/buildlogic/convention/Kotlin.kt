package com.worldline.buildlogic.convention

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget

/**
 * Configure base Kotlin with Android options for application modules.
 */
internal fun Project.configureKotlinForApplication() {
    extensions.configure<ApplicationExtension> {
        compileSdk = Versions.COMPILE_SDK
        defaultConfig.minSdk = Versions.MIN_SDK

        compileOptions.sourceCompatibility = JavaVersion.VERSION_17
        compileOptions.targetCompatibility = JavaVersion.VERSION_17
    }

    extensions.configure<KotlinAndroidProjectExtension> {
        target {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
            }
        }

        compilerOptions {
            // Treat all Kotlin warnings as errors (disabled by default)
            // Override by setting warningsAsErrors=true in your ~/.gradle/gradle.properties
            val warningsAsErrors: String? by project
            allWarningsAsErrors = warningsAsErrors.toBoolean()

            freeCompilerArgs.addAll(kotlinCompilerOptions)
        }
    }
}

/**
 * Configure Kotlin compiler options for multiplatform projects.
 */
internal fun Project.configureKotlinCompiler() {
    extensions.configure<KotlinMultiplatformExtension> {
        compilerOptions {
            // Treat all Kotlin warnings as errors (disabled by default)
            // Override by setting warningsAsErrors=true in your ~/.gradle/gradle.properties
            val warningsAsErrors: String? by project
            allWarningsAsErrors = warningsAsErrors.toBoolean()

            // Add experimental compiler options
            freeCompilerArgs.addAll(kotlinCompilerOptions)
        }

        targets.withType<KotlinAndroidTarget> {
            compilerOptions {
                jvmTarget = JvmTarget.JVM_17
            }
        }
    }

    tasks.withType<AbstractTestTask>().configureEach {
        failOnNoDiscoveredTests = false
    }
}

/**
 * Experimental Kotlin compiler options used across the project.
 */
val kotlinCompilerOptions = listOf(
    "-Xexpect-actual-classes",
    "-Xcontext-parameters",
    "-Xcontext-sensitive-resolution"
)
