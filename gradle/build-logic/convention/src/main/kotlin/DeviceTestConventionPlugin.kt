import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import com.worldline.buildlogic.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class DeviceTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            extensions.configure<KotlinMultiplatformExtension> {
                val androidExtension =
                    extensions.getByType<KotlinMultiplatformAndroidLibraryExtension>()

                androidExtension.apply {
                    withDeviceTestBuilder {
                        sourceSetTreeName = null
                    }.configure {
                        instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                    }
                }

                sourceSets {
                    all {
                        languageSettings.optIn("androidx.compose.ui.test.ExperimentalTestApi")
                    }

                    getByName("androidDeviceTest") {
                        dependencies {
                            val composeBom = libs.findLibrary("androidx.compose.bom").get()
                            implementation(project.dependencies.platform(composeBom))
                            implementation(
                                libs.findLibrary("androidx.compose.ui.test.junit4.android").get()
                            )
                            implementation(
                                libs.findLibrary("androidx.compose.ui.test.manifest").get()
                            )
                            implementation(kotlin("test"))
                            implementation(kotlin("test-annotations-common"))

                            val kotestBom = libs.findLibrary("kotest.bom").get()
                            implementation(project.dependencies.platform(kotestBom))
                            implementation(libs.findLibrary("kotest.assertions.core").get())
                        }
                    }
                }
            }
        }
    }
}
