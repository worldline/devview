plugins {
    alias(libs.plugins.convention.multiplatform.library)
    alias(libs.plugins.convention.compose.multiplatform)
    alias(libs.plugins.convention.datastore)
    alias(libs.plugins.convention.ktor)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.poko)
}

kotlin {
    addDefaultDevViewTargets()

    androidLibrary {
        namespace = "com.worldline.devview.networkmock"
    }

    sourceSets {
        commonMain {
            dependencies {
                api(projects.devview)
                implementation(projects.devviewUtils)
                implementation(libs.kotlinx.collections.immutable)
                implementation(libs.jetbrains.androidx.lifecycle.viewmodel.compose)
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
