plugins {
    alias(libs.plugins.convention.multiplatform.library)
    alias(libs.plugins.dokka)
}

kotlin {
    addDefaultDevViewTargets()

    android {
        namespace = "com.worldline.devview.docs"
    }
}

dependencies {
    dokka(projects.devview)
    dokka(projects.devviewAnalytics)
    dokka(projects.devviewFeatureflip)
    dokka(projects.devviewNetworkmock)
    dokka(projects.devviewNetworkmockCore)
    dokka(projects.devviewNetworkmockKtor)
    dokka(projects.devviewUtils)
}

dokka {
    moduleName.set("DevView")
}