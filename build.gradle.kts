import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.Date

val sdkVersion by extra(35)
val versionCode by extra(29)
val versionName by extra(checkNotNull(File("app.version").readText().lines().firstOrNull()?.trim()?.takeIf { it.isNotBlank() }) { "app.version empty" })
val versionNameSuffix by extra(getDate().lowercase())
val kotlinVersion = libs.versions.kotlin.get()

plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.hilt) apply false
}

allprojects {
  buildscript {

    dependencies {
      classpath("com.android.tools.build:gradle:8.7.3")
      classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
  }
  repositories {
    google()
    mavenCentral()
  }

  tasks.withType<KotlinCompile> {
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_21)
    }
  }
}

tasks.register<Delete>("clean") {
  delete(rootProject.layout.buildDirectory)
}

fun getDate(): String {
  val df = SimpleDateFormat("MMM-d-yyyy", Locale.ENGLISH)
  df.timeZone = TimeZone.getTimeZone("UTC")
  return df.format(Date())
}
