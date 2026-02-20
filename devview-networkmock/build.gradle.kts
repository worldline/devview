plugins {
    alias(libs.plugins.convention.multiplatform.library)
    alias(libs.plugins.convention.compose.multiplatform)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
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
                implementation(projects.devviewNetworkmockCore)
                implementation(libs.kotlinx.collections.immutable)
                implementation(libs.jetbrains.androidx.lifecycle.viewmodel.compose)
            }
        }
    }
}


tasks.withType<Test> {
    failOnNoDiscoveredTests.set(false)
}
