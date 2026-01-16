import com.worldline.buildlogic.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import kotlin.apply
import org.gradle.kotlin.dsl.configure

class KtorConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets {
                    val bom = libs.findLibrary("ktor.bom").get()

                    commonMain {
                        dependencies {
                            implementation(dependencies.platform(bom))
                            implementation(libs.findLibrary("ktor.client.core").get())
                            implementation(libs.findLibrary("ktor.client.content.negotiation").get())
                            implementation(libs.findLibrary("ktor.client.logging").get())
                            implementation(libs.findLibrary("ktor.serialization.kotlinx.json").get())
                        }
                    }

                    androidMain {
                        dependencies {
                            implementation(dependencies.platform(bom))
                            implementation(libs.findLibrary("ktor.client.okhttp").get())
                        }
                    }

                    iosMain {
                        dependencies {
                            implementation(dependencies.platform(bom))
                            implementation(libs.findLibrary("ktor.client.darwin").get())
                        }
                    }
                }
            }
        }
    }
}