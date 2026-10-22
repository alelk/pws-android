rootProject.name = "pws-android"
include(
  ":data:db-android",
  ":data:content-delivery",
  ":app-compose"
)

pluginManagement {
  // Shared build logic: precompiled `pws.android.*` convention plugins resolve by id like published plugins.
  includeBuild("convention-plugins")
  repositories {
    gradlePluginPortal()
    mavenCentral()
    google()
  }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
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
      substitute(module("io.github.alelk.pws.portable:portable-data"))
        .using(project(":portable-data"))
      substitute(module("io.github.alelk.pws.data:db-room"))
        .using(project(":data:db-room"))
      substitute(module("io.github.alelk.pws.data:repo-room"))
        .using(project(":data:repo-room"))
      substitute(module("io.github.alelk.pws.data:db-room-test-fixtures"))
        .using(project(":data:db-room:db-room-test-fixtures"))
      substitute(module("io.github.alelk.pws.features:features"))
        .using(project(":features"))
      substitute(module("io.github.alelk.pws.core:core-navigation"))
        .using(project(":core:navigation"))
    }
  }
} else {
  println("🌐 Local pws-core not found – will use Maven dependency (GitHub Packages)")
}