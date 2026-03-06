import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.worldline.buildlogic.convention.Versions
import com.worldline.buildlogic.convention.configureAndroidMultiplatformLibrary
import com.worldline.buildlogic.convention.configureDetekt
import com.worldline.buildlogic.convention.configureJava
import com.worldline.buildlogic.convention.configureKotlinCompiler
import com.worldline.buildlogic.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Convention plugin for Kotlin Multiplatform library modules targeting Android and iOS.
 *
 * This plugin configures:
 * - Kotlin Multiplatform with default hierarchy template
 * - Android library target with SDK versions
 * - iOS targets (x64, arm64, simulatorArm64)
 * - Kotlin compiler options
 * - Detekt for code analysis
 */
class MultiplatformLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlin.multiplatform")
            apply(plugin = "com.android.kotlin.multiplatform.library")

            configureJava()

            extensions.configure<KotlinMultiplatformExtension> {
                applyDefaultHierarchyTemplate()
                explicitApi()

                sourceSets {
                    commonMain {
                        dependencies {
                            implementation(libs.findLibrary("kermit").get())
                        }
                    }
                    commonTest {
                        dependencies {
                            implementation(kotlin("test"))
                            implementation(kotlin("test-annotations-common"))
                        }
                    }
                }
            }

            // Configure Android SDK versions and packaging
            configureAndroidMultiplatformLibrary()

            // Configure Kotlin compiler options
            configureKotlinCompiler()

            // Configure Detekt for code analysis
            configureDetekt()
        }
    }
}

fun KotlinMultiplatformExtension.addDefaultDevViewTargets() {
    targets.withType<KotlinMultiplatformAndroidLibraryTarget> {
        compileSdk = Versions.COMPILE_SDK
        minSdk = Versions.MIN_SDK

        withJava()
    }

    iosArm64()
    iosSimulatorArm64()
}
