plugins {
    alias(libs.plugins.convention.multiplatform.library)
    alias(libs.plugins.dokka)
}

kotlin {
    addDefaultDevViewTargets()

    androidLibrary {
        namespace = "com.worldline.devview.docs"
    }
}

dokka {
    moduleName.set("DevView")
}