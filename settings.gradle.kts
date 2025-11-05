rootProject.name = "pws-android" // sanitized (was "P&W Songs for Android") to satisfy npm package name rules: lowercase, no spaces, no ampersand
include(
  ":domain",
  ":domain:domain-test-fixtures",
  ":backup",
  ":data:room-database",
  ":data:android-database",
  ":data:room-database:database-test-fixtures",
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