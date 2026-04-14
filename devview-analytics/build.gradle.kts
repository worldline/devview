plugins {
    alias(libs.plugins.convention.multiplatform.library)
    alias(libs.plugins.convention.compose.multiplatform)
    alias(libs.plugins.convention.datastore)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.poko)
}

kotlin {
    addDefaultDevViewTargets()

    android {
        namespace = "com.worldline.devview.analytics"
    }

    sourceSets {
        commonMain {
            dependencies {
                api(projects.devview)
                implementation(libs.kotlinx.collections.immutable)
                implementation(libs.kotlinx.datetime)
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
