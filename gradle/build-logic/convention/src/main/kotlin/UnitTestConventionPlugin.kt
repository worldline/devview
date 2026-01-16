import com.worldline.buildlogic.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension

/**
 * Convention plugin for unit test configuration in multiplatform modules.
 *
 * This plugin configures:
 * - Kotest as the testing framework
 * - Common test dependencies (assertions, property testing, coroutines)
 *
 * Note: This plugin should be applied alongside [MultiplatformLibraryConventionPlugin]
 * which handles Kotlin Multiplatform and Android configuration.
 *
 * TODO: Revisit this file - currently it mainly configures Kotest. Consider either:
 *       - Renaming to KotestConventionPlugin for clarity, or
 *       - Adding more globally necessary unit test dependencies
 */
class UnitTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "io.kotest")

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets {
                    commonTest {
                        dependencies {
                            val bom = libs.findLibrary("kotest.bom").get()
                            implementation(project.dependencies.platform(bom))
                            implementation(libs.findLibrary("kotest.framework.engine").get())
                            implementation(libs.findLibrary("kotest.assertions.core").get())
                            implementation(libs.findLibrary("kotest.property").get())

                            implementation(libs.findLibrary("kotlinx.coroutines.test").get())
                        }
                    }
                }
            }
        }
    }
}