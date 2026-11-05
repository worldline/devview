import io.github.frankois944.spmForKmp.swiftPackageConfig
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.convention.multiplatform.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.convention.unitTest)
    alias(libs.plugins.convention.kover)
    alias(libs.plugins.convention.metalava)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.spmforkmp)
}

kotlin {
    addDefaultDevViewTargets()

    targets.withType<KotlinNativeTarget>().configureEach {
        compilerOptions.optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
        binaries.getTest("debug").freeCompilerArgs +=
            "-Xoverride-konan-properties=osVersionMin.ios_simulator_arm64=15.0"
        swiftPackageConfig("crashHistoryBridge") {
            minIos = "15.0"
            publishSafe = true
        }
    }

    android {
        namespace = "com.worldline.devview.crashhistory.core"
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.jetbrains.compose.runtime)
                implementation(libs.jetbrains.compose.ui)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.io.core)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}

// spmForKmp 1.9.7 prepares a shared Sources directory; concurrent target builds race on clean output.
val deviceBridgeBuild = tasks.matching {
    it.name == "SwiftPackageConfigAppleCrashHistoryBridgeCompileSwiftPackageIosArm64"
}
tasks.matching {
    it.name == "SwiftPackageConfigAppleCrashHistoryBridgeCompileSwiftPackageIosSimulatorArm64"
}.configureEach {
    mustRunAfter(deviceBridgeBuild)
}

tasks.withType<Test> {
    failOnNoDiscoveredTests.set(false)
}
