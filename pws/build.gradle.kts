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

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.Date

val sdkVersion by extra(34)
val supportVersion by extra("29.0.2")
val constraintLayoutVersion by extra("2.1.4")
val versionCode by extra(27)
val versionName by extra("1.7.0")
val versionNameSuffix by extra(getDate().lowercase())
val kotlinVersion by extra("1.7.20")

plugins {
    id("com.android.application") version "8.5.1" apply false
    id("org.jetbrains.kotlin.android") version "1.7.20" apply false
}

allprojects {
    buildscript {
        dependencies {
            classpath("com.android.tools.build:gradle:8.5.1")
            classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        }
    }
    repositories {
        google()
        mavenCentral()
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
