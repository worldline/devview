import me.tylerbwong.gradle.metalava.extension.MetalavaExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension

class MetalavaConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("me.tylerbwong.gradle.metalava")

        configure<MetalavaExtension> {
            filename.set("api/api.txt")
            excludedSourceSets.setFrom(
                kotlinExtension
                    .sourceSets
                    .filter { it.name.contains("test", ignoreCase = true) }
                    .flatMap { it.kotlin.sourceDirectories },
            )
            version.set("1.0.0-alpha14")
        }

        tasks.named { it.startsWith("metalava") }.configureEach {
            dependsOn(tasks.named { it.startsWith("generateResourceAccessors") || it.startsWith("generateActualResourceCollectors") })
        }
    }
}
