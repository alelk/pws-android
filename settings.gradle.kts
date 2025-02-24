rootProject.name = "P&W Songs for Android"
include( ":domain", ":domain:domain-test-fixtures", ":backup", ":database", ":database:database-test-fixtures", ":app")

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