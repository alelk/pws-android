import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

val versionCode by extra(48)
val versionName by extra(checkNotNull(projectDir.resolve("app.version").readText().lines().firstOrNull()?.trim()?.takeIf { it.isNotBlank() }) { "app.version empty" })
val versionNameSuffix by extra(getDate().lowercase())
val kotlinVersion = libs.versions.kotlin.get()

plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.kmpLibrary) apply false
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.ksp) apply false
  id("maven-publish")
}

allprojects {
  group = "io.github.alelk.pws.android"
  version = versionName

  repositories {
    google()
    mavenCentral()
    maven {
      url = uri("https://maven.pkg.github.com/alelk/pws-core")
      credentials {
        username = findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USER") ?: "alelk"
        password = findProperty("gpr.token") as String? ?: System.getenv("GITHUB_TOKEN")
      }
    }

    // RuStore SDK — restricted to ru.rustore.sdk so the external repository is
    // never queried for any other dependency (perf + isolation). Only the
    // `rustore` flavor pulls these artifacts (see :app-compose flavor-scoped deps).
    maven {
      // New RuStore SDK releases are published here. The former
      // artifactory-external.vkpartner.ru endpoint does not contain the 2026 BOMs.
      url = uri("https://nexus-external.vkteam.ru/repository/maven-rustore-exposed/")
      content { includeGroup("ru.rustore.sdk") }
    }
  }
}

// F2: pws-android keeps its own version catalog (must build without an adjacent pws-core
// checkout), but values of the dependency keys explicitly shared with pws-core (README §4) must
// not drift. pws-core is the source of truth for those shared values. Only this explicit list is
// checked — other keys that happen to share a name (e.g. robolectric, kotest-runner-android) are
// deliberately per-repo (pws-android's test code may depend on a newer API than pws-core uses).
val sharedVersionKeys = listOf(
  "kotlin", "ksp", "kotlinx-coroutines", "kotlinx-datetime", "kotlinx-serialization",
  "kaml", "ktor", "koin", "voyager", "composeMultiplatform", "room", "kotest", "arrow"
)

val verifyCoreVersionAlignment = tasks.register("verifyCoreVersionAlignment") {
  group = "verification"
  description = "Fails if a version shared with pws-core's catalog has diverged (see README §4, F2)."

  val androidCatalog = rootDir.resolve("gradle/libs.versions.toml")
  val coreCatalog = rootDir.resolve("../pws-core/gradle/libs.versions.toml")
  inputs.file(androidCatalog)
  if (coreCatalog.exists()) inputs.file(coreCatalog)

  doLast {
    if (!coreCatalog.exists()) {
      logger.lifecycle("verifyCoreVersionAlignment: ../pws-core not found next to pws-android — skipping (no local composite build)")
      return@doLast
    }

    fun parseVersions(file: java.io.File): Map<String, String> {
      val versions = mutableMapOf<String, String>()
      var inVersionsSection = false
      file.readLines().forEach { rawLine ->
        val line = rawLine.substringBefore("#").trim()
        if (line.isEmpty()) return@forEach
        if (line.startsWith("[")) {
          inVersionsSection = line == "[versions]"
          return@forEach
        }
        if (!inVersionsSection) return@forEach
        val eq = line.indexOf('=')
        if (eq <= 0) return@forEach
        val key = line.substring(0, eq).trim()
        val value = line.substring(eq + 1).trim().trim('"')
        versions[key] = value
      }
      return versions
    }

    val androidVersions = parseVersions(androidCatalog)
    val coreVersions = parseVersions(coreCatalog)
    val mismatches = sharedVersionKeys
      .filter { androidVersions.containsKey(it) && coreVersions.containsKey(it) }
      .filter { androidVersions[it] != coreVersions[it] }
      .sorted()
      .map { "  $it: pws-android=${androidVersions[it]} vs pws-core=${coreVersions[it]}" }

    if (mismatches.isNotEmpty()) {
      throw GradleException(
        "Shared dependency versions diverged from pws-core's catalog (pws-core is the source of truth):\n" +
          mismatches.joinToString("\n")
      )
    }
  }
}

subprojects {
  tasks.matching { it.name == "check" }.configureEach {
    dependsOn(verifyCoreVersionAlignment)
  }
}

fun getDate(): String {
  val df = SimpleDateFormat("MMM-d-yyyy", Locale.ENGLISH)
  df.timeZone = TimeZone.getTimeZone("UTC")
  return df.format(Date())
}
