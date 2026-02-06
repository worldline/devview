plugins {
    alias(libs.plugins.convention.multiplatform.library)
    alias(libs.plugins.convention.compose.multiplatform)
    alias(libs.plugins.convention.datastore)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
}

kotlin {
    addDefaultDevViewTargets()

    androidLibrary {
        namespace = "com.worldline.devview.utils"
    }

    sourceSets {
        commonMain {
            dependencies {
                api(projects.devview)
                implementation(libs.kotlinx.collections.immutable)
            }
        }
    }
}
