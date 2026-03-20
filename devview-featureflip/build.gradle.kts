plugins {
    alias(libs.plugins.convention.multiplatform.library)
    alias(libs.plugins.convention.compose.multiplatform)
    alias(libs.plugins.convention.unitTest)
    alias(libs.plugins.convention.deviceTest)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.poko)
}

kotlin {
    addDefaultDevViewTargets()

    android {
        namespace = "com.worldline.devview.featureflip"
    }

    sourceSets {
        commonMain {
            dependencies {
                api(projects.devview)
                implementation(projects.devviewUtils)
                implementation(libs.kotlinx.collections.immutable)
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
