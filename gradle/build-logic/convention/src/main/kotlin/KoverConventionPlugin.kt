import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import kotlinx.kover.gradle.plugin.dsl.KoverReportFilter
import kotlinx.kover.gradle.plugin.dsl.KoverReportsConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

class KoverConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlinx.kover")

            extensions.configure<KoverProjectExtension> {
                reports {
                    filters.excludes {
                        androidGeneratedClasses()
                        packages(
                            "*.generated.resources",
                            "*.internal.*"
                        )
                        classes(
                            "*_*",
                            "*.*Constructor*",
                            "*.*BuildKonfig*",
                            "*.*ComposableSingletons*",
                            "*.*ScreenKt",
                            "*.TimeRange*"
                        )
                        annotatedBy(
                            "androidx.compose.runtime.Composable",
                            "androidx.compose.ui.tooling.preview.Preview"
                        )
                        inheritedFrom(
                            "*.HasTitle",
                            "*.PolymorphicModuleBuilder",
                            "*.ProvidableCompositionLocal",
                            "*.HighlightedAnalyticsLog"
                        )
                    }
                }
            }
        }
    }
}