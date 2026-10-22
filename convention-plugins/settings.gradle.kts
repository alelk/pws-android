rootProject.name = "convention-plugins"

dependencyResolutionManagement {
  repositories {
    google()
    gradlePluginPortal()
    mavenCentral()
  }
  // Reuse the project's single version catalogue so plugin versions are never inlined here.
  versionCatalogs {
    create("libs") {
      from(files("../gradle/libs.versions.toml"))
    }
  }
}
