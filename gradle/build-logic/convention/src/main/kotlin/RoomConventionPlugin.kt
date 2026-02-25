import androidx.room.gradle.RoomExtension
import com.worldline.buildlogic.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension

class RoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.google.devtools.ksp")
            apply(plugin = "androidx.room")

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets {
                    commonMain {
                        dependencies {
                            implementation(libs.findLibrary("androidx.room.runtime").get())
                            implementation(libs.findLibrary("androidx.sqlite").get())
                        }
                    }
                }
            }

            extensions.configure<RoomExtension> {
                // The schemas directory contains a schema file for each version of the Room database.
                // This is required to enable Room auto migrations.
                // See https://developer.android.com/reference/kotlin/androidx/room/AutoMigration.
                schemaDirectory("$projectDir/schemas")
                generateKotlin = true
            }

            dependencies {
                "kspAndroid"(libs.findLibrary("androidx.room.compiler").get())
                "kspIosSimulatorArm64"(libs.findLibrary("androidx.room.compiler").get())
                "kspIosArm64"(libs.findLibrary("androidx.room.compiler").get())
            }
        }
    }
}