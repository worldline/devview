import com.android.build.api.dsl.ApplicationExtension
import com.worldline.buildlogic.convention.ProjectBuildType
import com.worldline.buildlogic.convention.Versions
import com.worldline.buildlogic.convention.configureDefaultExcludes
import com.worldline.buildlogic.convention.configureDetekt
import com.worldline.buildlogic.convention.configureKotlinForApplication
import com.worldline.buildlogic.convention.kotlinCompilerOptions
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.support.kotlinCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * Convention plugin for Android application modules (e.g., sample app).
 *
 * This plugin configures:
 * - Android application with SDK versions
 * - Compose compiler plugin
 * - Detekt for code analysis
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.application")
            apply(plugin = "org.jetbrains.kotlin.plugin.compose")

            configureKotlinForApplication()

            extensions.configure<ApplicationExtension> {
                defaultConfig.targetSdk = Versions.TARGET_SDK

                @Suppress("UnstableApiUsage")
                testOptions.animationsDisabled = true

                packaging {
                    configureDefaultExcludes()
                }

                buildTypes {
                    debug {
                        applicationIdSuffix = ProjectBuildType.DEBUG.applicationIdSuffix
                        isDefault = true
                    }
                    release {
                        applicationIdSuffix = ProjectBuildType.RELEASE.applicationIdSuffix

                        isMinifyEnabled = true
                        isShrinkResources = true
                    }
                }
            }
        }
    }
}
