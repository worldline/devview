import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import com.worldline.buildlogic.convention.libs
import dev.mokkery.gradle.ApplicationRule
import dev.mokkery.gradle.MokkeryGradleExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.Actions.with
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Convention plugin for unit test configuration in multiplatform modules.
 *
 * This plugin configures test dependencies across all test source sets:
 * - **commonTest**: Kotest framework, assertions, property testing, coroutine testing
 * - **androidHostTest**: Kotest runner (JUnit 5), Android test core utilities, mocking
 *
 * For device/emulator tests (`androidDeviceTest`), apply [ComposeDeviceTestConventionPlugin]
 * in addition to this plugin.
 *
 * ## Applied Frameworks
 * - **Kotest** (6.1.7): Multiplatform testing framework
 * - **kotlinx-coroutines-test**: Coroutine testing utilities
 * - **Mockk** (1.13.15): Kotlin-native mocking library
 * - **Turbine** (1.1.0): Flow testing utilities
 * - **androidx-test-core**: Android test utilities (androidHostTest only)
 *
 * ## Usage
 * Apply this plugin in your library's `build.gradle.kts`:
 * ```kotlin
 * plugins {
 *     alias(libs.plugins.convention.multiplatform.library)
 *     alias(libs.plugins.convention.unitTest)
 * }
 * ```
 */
class UnitTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.google.devtools.ksp")
            apply(plugin = "io.kotest")
            apply(plugin = "dev.mokkery")

            extensions.configure<KotlinMultiplatformExtension> {
                val androidExtension =
                    extensions.getByType<KotlinMultiplatformAndroidLibraryExtension>()

                androidExtension.apply {
                    withHostTestBuilder {
                        sourceSetTreeName = null
                    }.configure {
                        isIncludeAndroidResources = true
                    }
                }

                sourceSets {
                    all {
                        languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                    }

                    val kotestBom = libs.findLibrary("kotest.bom").get()
                    val ktorBom = libs.findLibrary("ktor.bom").get()

                    commonTest {
                        dependencies {
                            implementation(project.dependencies.platform(kotestBom))
                            implementation(libs.findLibrary("kotest.framework.engine").get())
                            implementation(libs.findLibrary("kotest.assertions.core").get())
                            implementation(libs.findLibrary("kotest.property").get())

                            implementation(libs.findLibrary("kotlinx.coroutines.test").get())
                            implementation(libs.findLibrary("turbine").get())

                            implementation(kotlin("test"))
                            implementation(kotlin("test-annotations-common"))

                            implementation(dependencies.platform(ktorBom))
                            implementation(libs.findLibrary("ktor.client.mock").get())
                        }
                    }

                    getByName("androidHostTest") {
                        dependencies {
                            implementation(project.dependencies.platform(kotestBom))
                            implementation(libs.findLibrary("kotest.runner.junit5").get())
                            implementation(libs.findLibrary("mockk").get())

                            implementation(kotlin("test"))
                            implementation(kotlin("test-annotations-common"))

                            implementation(dependencies.platform(ktorBom))
                            implementation(libs.findLibrary("ktor.client.mock").get())
                        }
                    }
                }
            }

            extensions.configure<MokkeryGradleExtension> {
                rule.set(ApplicationRule.All)
                ignoreFinalMembers.set(true)
            }
        }
    }
}