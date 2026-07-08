import dev.detekt.gradle.Detekt
import org.gradle.kotlin.dsl.withType

plugins {
    alias(libs.plugins.convention.konsist)
    alias(libs.plugins.detekt)
}

tasks.withType<Test> {
    outputs.upToDateWhen { false }
}

detekt {
    source.setFrom(files("$projectDir/src"))
    config.setFrom(files(
        "$rootDir/config/quality/detekt/default-config.yml",
        "$rootDir/config/quality/detekt/ktlint-config.yml"
    ))
    baseline.set(file("$rootDir/config/quality/detekt/baseline.xml"))
    autoCorrect.set(true)
}

tasks.withType<Detekt> {
    reports {
        sarif.required.set(false)
        html.required.set(true)
        checkstyle.required.set(true)
        markdown.required.set(false)
    }
}

dependencies {
    detektPlugins(libs.detekt.cli)
    detektPlugins(libs.detekt.ktlint)
    detektPlugins(libs.detekt.libraries)
}