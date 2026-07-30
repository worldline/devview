plugins {
    alias(libs.plugins.convention.multiplatform.library)
    alias(libs.plugins.convention.compose.multiplatform)
    alias(libs.plugins.convention.unitTest)
    alias(libs.plugins.convention.deviceTest)
    alias(libs.plugins.convention.kover)
    alias(libs.plugins.convention.metalava)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.poko)
}

kotlin {
    addDefaultDevViewTargets()

    android {
        namespace = "com.worldline.devview"
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.jetbrains.compose.components.resources)

                api(libs.kotlinx.collections.immutable)
                api(projects.devviewUtils)
            }
        }
    }
}

poko {
    pokoAnnotation.set("com/worldline/devview/core/Poko")
}

tasks.withType<Test> {
    failOnNoDiscoveredTests.set(false)
}

compose {
    resources {
        packageOfResClass = "com.worldline.devview"
    }
}

dependencies {
    kover(projects.devviewAnalytics)
    kover(projects.devviewFeatureflip)
    kover(projects.devviewTimecapsule)
    kover(projects.devviewNetworkmock)
    kover(projects.devviewNetworkmockCore)
    kover(projects.devviewNetworkmockKtor)
    kover(projects.devviewUtils)
}
