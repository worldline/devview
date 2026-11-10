plugins {
    alias(libs.plugins.convention.multiplatform.library)
    alias(libs.plugins.convention.compose.multiplatform)
    alias(libs.plugins.convention.unitTest)
    alias(libs.plugins.convention.deviceTest)
    alias(libs.plugins.convention.kover)
    alias(libs.plugins.convention.metalava)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
}

kotlin {
    addDefaultDevViewTargets()

    android {
        namespace = "com.worldline.devview.consolelogger"
    }

    sourceSets {
        commonMain {
            dependencies {
                api(projects.devview)
                // Non-implementation: DevViewLogWriter extends Kermit's LogWriter,
                // so LogWriter must be visible to consumers of the public API.
                api(libs.kermit)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.collections.immutable)
                implementation(libs.kotlinx.datetime)
            }
        }

        commonTest {
            dependencies {
                implementation(projects.devviewTest)
            }
        }

        androidDeviceTest {
            dependencies {
                implementation(projects.devviewTest)
            }
        }
    }
}

tasks.withType<Test> {
    failOnNoDiscoveredTests.set(false)
}
