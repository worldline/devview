package com.worldline.buildlogic.convention

import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

internal fun Project.configureDetekt() {
    apply(plugin = "dev.detekt")

    configure<DetektExtension> {
        source.setFrom(files("$projectDir/src"))
        config.setFrom(files(
            "$rootDir/config/quality/detekt/default-config.yml",
            "$rootDir/config/quality/detekt/compose-config.yml",
            "$rootDir/config/quality/detekt/ktlint-config.yml",
            "$rootDir/config/quality/detekt/libraries-config.yml"
        ))
        baseline.set(file("$rootDir/config/quality/detekt/baseline.xml"))
        autoCorrect.set(true)
    }

    tasks.withType<Detekt>().configureEach {
        exclude(
            "**/test/",
            "**/android*Test/",
            "**/commonTest/",
            "**/screenshotTest/",
            ".*/tmp/.*"
        )
        exclude { it.file.invariantSeparatorsPath.contains("/build/") }

        reports {
            sarif.required.set(false)
            html.required.set(true)
            checkstyle.required.set(true)
            markdown.required.set(false)
        }
    }

    dependencies {
        "detektPlugins"(libs.findLibrary("detekt.cli").get())
        "detektPlugins"(libs.findLibrary("detekt.ktlint").get())
        "detektPlugins"(libs.findLibrary("detekt.libraries").get())
        "detektPlugins"(libs.findLibrary("detekt.compose.rules").get())
    }
}