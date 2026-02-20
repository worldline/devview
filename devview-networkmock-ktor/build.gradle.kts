plugins {
    alias(libs.plugins.convention.multiplatform.library)
    alias(libs.plugins.convention.ktor)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
}

kotlin {
    addDefaultDevViewTargets()

    androidLibrary {
        namespace = "com.worldline.devview.networkmock.ktor"
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.devviewNetworkmockCore)
            }
        }
    }
}

tasks.withType<Test> {
    failOnNoDiscoveredTests.set(false)
}
