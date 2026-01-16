import com.worldline.buildlogic.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension

class KonsistConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlin.jvm")
            apply(plugin = "org.jetbrains.kotlin.plugin.compose")
            apply(plugin = "org.jetbrains.compose")


            tasks.withType<Test> {
                useJUnitPlatform()
            }

            extensions.configure<KotlinJvmProjectExtension> {
                jvmToolchain(21)
            }

            dependencies {
                "testImplementation"(libs.findLibrary("konsist").get())
                val kotestBom = libs.findLibrary("kotest.bom").get()
                "testImplementation"(dependencies.platform(kotestBom))
                "testImplementation"(libs.findLibrary("kotest.runner.junit5").get())

                "testImplementation"(libs.findLibrary("jetbrains.androidx.lifecycle.viewmodel").get())

                "testImplementation"(libs.findLibrary("jetbrains.compose.runtime").get())
                "testImplementation"(libs.findLibrary("jetbrains.compose.ui.tooling.preview").get())
            }
        }
    }
}
