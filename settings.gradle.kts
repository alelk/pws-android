rootProject.name = "pws-android" // sanitized (was "P&W Songs for Android") to satisfy npm package name rules: lowercase, no spaces, no ampersand
include(
  ":domain",
  ":domain:domain-test-fixtures",
  ":backup",
  ":data:db-room",
  ":data:db-android",
  ":data:db-room:db-room-test-fixtures",
  ":data:repo-room",
  ":app"
)

pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
    google()
  }

  plugins {
  }
}

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      from(files("libs.versions.toml"))
    }
  }
}