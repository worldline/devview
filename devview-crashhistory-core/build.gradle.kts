plugins {
    alias(libs.plugins.convention.multiplatform.library)
    alias(libs.plugins.convention.compose.multiplatform)
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
