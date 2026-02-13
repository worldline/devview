plugins {
    alias(libs.plugins.convention.multiplatform.library)
    alias(libs.plugins.convention.compose.multiplatform)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.poko)
}

kotlin {
    addDefaultDevViewTargets()

    androidLibrary {
        namespace = "com.worldline.devview"
    }

    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlinx.collections.immutable)
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
