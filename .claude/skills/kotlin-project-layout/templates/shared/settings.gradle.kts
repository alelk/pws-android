pluginManagement {
    // Shared build logic lives in an included build so precompiled convention plugins
    // (app.kmp / app.jvm / app.compose …) resolve by id like published plugins.
    includeBuild("convention-plugins")
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "my-project"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Foundation (KMP)
include(":domain")

// API layer (KMP)
include(":api:contract")
include(":api:mapping")
include(":api:client")
include(":api:client:di")

// UI (KMP, Compose Multiplatform) + thin shells
include(":features")
include(":webapp")

// Server (JVM only)
include(":server:infra")
include(":server:transport")
include(":server:di")
include(":server:app")
