import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

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

    // publishSafe keeps the producer's Xcode paths out of klibs; consumers supply their own.
    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.getTest("debug").freeCompilerArgs +=
            "-Xoverride-konan-properties=osVersionMin.ios_simulator_arm64=15.0"
        val sdk = if (name == "iosArm64") "iphoneos" else "iphonesimulator"
        val swiftRuntime = providers.exec {
            commandLine("xcrun", "--sdk", sdk, "xcode-select", "-p")
        }.standardOutput.asText.map {
            "${it.trim()}/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/$sdk"
        }
        binaries.configureEach {
            linkTaskProvider.configure {
                toolOptions.freeCompilerArgs.addAll(swiftRuntime.map { listOf("-linker-option", "-L$it") })
            }
        }
    }

    android {
        namespace = "com.worldline.devview.crashhistory"
    }

    sourceSets {
        commonMain {
            dependencies {
                api(projects.devview)
                api(projects.devviewCrashhistoryCore)
                implementation(libs.kotlinx.collections.immutable)
                implementation(libs.kotlinx.datetime)
            }
        }

        androidMain {
            dependencies {
                // FileProvider, to share the exported JSON file with other apps.
                implementation(libs.androidx.core)
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
