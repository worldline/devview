plugins {
    alias(libs.plugins.convention.multiplatform.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.convention.unitTest)
    alias(libs.plugins.convention.kover)
    alias(libs.plugins.convention.metalava)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
}

kotlin {
    addDefaultDevViewTargets()

    android {
        namespace = "com.worldline.devview.crashhistory.core"
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.jetbrains.compose.runtime)
                implementation(libs.jetbrains.compose.ui)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.io.core)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}

tasks.withType<Test> {
    failOnNoDiscoveredTests.set(false)
}
