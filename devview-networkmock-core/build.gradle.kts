plugins {
    alias(libs.plugins.convention.multiplatform.library)
    alias(libs.plugins.convention.datastore)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
}

kotlin {
    addDefaultDevViewTargets()

    androidLibrary {
        namespace = "com.worldline.devview.networkmock.core"
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.devviewUtils)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.jetbrains.compose.runtime)
            }
        }
    }
}

tasks.withType<Test> {
    failOnNoDiscoveredTests.set(false)
}
