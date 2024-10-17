/*
 * Copyright (C) 2018 The P&W Songs Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.Date

val sdkVersion by extra(35)
val supportVersion by extra("29.0.2")
val constraintLayoutVersion by extra("2.1.4")
val versionCode by extra(28)
val versionName by extra(checkNotNull(File("app.version").readText().lines().firstOrNull()?.trim()?.takeIf { it.isNotBlank() }) { "app.version empty" })
val versionNameSuffix by extra(getDate().lowercase())
val kotlinVersion = libs.versions.kotlin.get()
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.android) apply false
}

allprojects {
  buildscript {

    dependencies {
      classpath("com.android.tools.build:gradle:8.6.1")
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
  val df = SimpleDateFormat("MMM-d-yyyy", Locale("en_EN"))
  df.timeZone = TimeZone.getTimeZone("UTC")
  return df.format(Date())
}
