import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.worldline.buildlogic.convention.Versions
import com.worldline.buildlogic.convention.configureDefaultExcludes
import com.worldline.buildlogic.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension

/**
 * Convention plugin for Compose Multiplatform library modules.
 *
 * This plugin configures:
 * - Compose Multiplatform with essential UI dependencies
 * - Compose compiler plugin
 * - Opt-in annotations for experimental Compose APIs
 *
 * Note: This plugin should be applied alongside [MultiplatformLibraryConventionPlugin]
 * which handles Kotlin Multiplatform and Android configuration.
 *
 * TODO: Revisit this file to evaluate if all dependencies belong here or should be
 *       extracted to separate convention plugins (e.g., Adaptive, Navigation3).
 */
class ComposeMultiplatformConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlin.plugin.compose")
            apply(plugin = "org.jetbrains.compose")
            apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

            extensions.configure<KotlinMultiplatformExtension> {
                val androidExtension =
                    extensions.getByType<KotlinMultiplatformAndroidLibraryExtension>()

                androidExtension.apply {
                    targets.withType<KotlinMultiplatformAndroidLibraryTarget> {
                        compileSdk = Versions.COMPILE_SDK

                        packaging {
                            configureDefaultExcludes()
                        }

                        compilerOptions {
                            jvmTarget.set(JvmTarget.JVM_17)
                        }

                        androidResources {
                            enable = true
                        }
                    }
                }

                sourceSets {
                    all {
                        languageSettings.optIn("kotlin.RequiresOptIn")
                        languageSettings.optIn("androidx.compose.material3.ExperimentalMaterial3Api")
                        languageSettings.optIn("androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
                        languageSettings.optIn("androidx.compose.foundation.layout.ExperimentalGridApi")
                    }

                    commonMain {
                        dependencies {
                            // Core Compose dependencies
                            implementation(libs.findLibrary("jetbrains.compose.runtime").get())
                            implementation(libs.findLibrary("jetbrains.compose.foundation").get())
                            implementation(libs.findLibrary("jetbrains.compose.ui").get())

                            // Material 3
                            implementation(libs.findLibrary("jetbrains.compose.material3").get())
                            implementation(libs.findLibrary("jetbrains.compose.material.icons.extended").get())

                            // Resources
                            implementation(libs.findLibrary("jetbrains.compose.components.resources").get())

                            // Preview tooling
                            implementation(libs.findLibrary("jetbrains.compose.ui.tooling.preview").get())

                            // Compose Adaptive
                            implementation(libs.findLibrary("jetbrains.compose.material3.adaptive").get())
                            implementation(libs.findLibrary("jetbrains.compose.material3.adaptive.layout").get())
                            implementation(libs.findLibrary("jetbrains.compose.material3.adaptive.navigation").get())
                            implementation(libs.findLibrary("jetbrains.compose.material3.adaptive.navigation.suite").get())

                            // Navigation3
                            implementation(libs.findLibrary("jetbrains.androidx.navigation3.ui").get())
                            implementation(libs.findLibrary("jetbrains.compose.material3.adaptive.navigation3").get())
                        }
                    }

                    androidMain {
                        dependencies {
                            implementation(libs.findLibrary("jetbrains.compose.ui.tooling").get())
                            implementation(libs.findLibrary("jetbrains.compose.ui.tooling.preview").get())
                        }
                    }
                }
            }

            dependencies {
                "androidRuntimeClasspath"(libs.findLibrary("jetbrains.compose.ui.tooling").get())
            }
        }
    }
}