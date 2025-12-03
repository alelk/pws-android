rootProject.name = "pws-android"
include(
  ":data:db-android",
  ":app"
)

pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
    google()
  }
}

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      from(files("libs.versions.toml"))
    }
  }
}

val localCoreDir = File(rootDir.parent, "pws-core")
if (localCoreDir.exists() && localCoreDir.isDirectory) {
  println("🔗 Local pws-core found – using composite build")
  includeBuild("../pws-core") {
    dependencySubstitution {
      substitute(module("io.github.alelk.pws.domain:domain"))
        .using(project(":domain"))
      substitute(module("io.github.alelk.pws.domain:domain-test-fixtures"))
        .using(project(":domain:domain-test-fixtures"))
      substitute(module("io.github.alelk.pws.backup:backup"))
        .using(project(":backup"))
      substitute(module("io.github.alelk.pws.data:db-room"))
        .using(project(":data:db-room"))
      substitute(module("io.github.alelk.pws.data:repo-room"))
        .using(project(":data:repo-room"))
      substitute(module("io.github.alelk.pws.data:db-room-test-fixtures"))
        .using(project(":data:db-room:db-room-test-fixtures"))
    }
  }
} else {
  println("🌐 Local pws-core not found – will use Maven dependency (GitHub Packages)")
}