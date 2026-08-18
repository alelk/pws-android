import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

val sdkVersion by extra(36)
val versionCode by extra(46)
val versionName by extra(checkNotNull(projectDir.resolve("app.version").readText().lines().firstOrNull()?.trim()?.takeIf { it.isNotBlank() }) { "app.version empty" })
val versionNameSuffix by extra(getDate().lowercase())
val kotlinVersion = libs.versions.kotlin.get()

plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.kmpLibrary) apply false
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.hilt) apply false
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
    // rustore sdk — restricted to ru.rustore.sdk so the external Artifactory is
    // never queried for any other dependency (perf + isolation). Only the
    // `rustore` flavor pulls these artifacts (see :app-compose flavor-scoped deps).
    maven {
      url = uri("https://artifactory-external.vkpartner.ru/artifactory/maven")
      content { includeGroup("ru.rustore.sdk") }
    }
  }

  tasks.withType<KotlinCompile> {
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_21)
    }
  }
}

fun getDate(): String {
  val df = SimpleDateFormat("MMM-d-yyyy", Locale.ENGLISH)
  df.timeZone = TimeZone.getTimeZone("UTC")
  return df.format(Date())
}