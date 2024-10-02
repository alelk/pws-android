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

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}


android {
    compileSdk = rootProject.extra["sdkVersion"] as Int

    signingConfigs {
        create("release-ru") {
            keyAlias = project.findProperty("android.release.keyAliasRu") as String?
            keyPassword = project.findProperty("android.release.keyPassword") as String?
            storeFile =
                (project.findProperty("android.release.keystorePath") as String?)?.let(::file)
            storePassword = project.findProperty("android.release.storePassword") as String?
        }
    }

    defaultConfig {
        applicationId = "com.alelk.pws.pwapp"
        resValue("string", "db_authority", "com.alelk.pws.database")
        minSdk = 21
        targetSdk = rootProject.extra["sdkVersion"] as Int
        versionCode = rootProject.extra["versionCode"] as Int
        versionName =
            "${rootProject.extra["versionName"]}-${rootProject.extra["versionNameSuffix"]}"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release-ru")
        }
        getByName("debug") {
            isDebuggable = true
            isMinifyEnabled = false
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets")
        }
    }

    flavorDimensions.add("contentLevel")

    productFlavors {
        create("ru") {
            dimension = "contentLevel"
        }
        create("uk") {
            dimension = "contentLevel"
            applicationIdSuffix = ".uk"
            versionNameSuffix = "-uk"
            resValue("string", "db_authority", "com.alelk.pws.database.uk")
        }
        create("full") {
            dimension = "contentLevel"
            applicationIdSuffix = ".full"
            versionNameSuffix = "-full"
            resValue("string", "db_authority", "com.alelk.pws.database.full")
        }
    }

    namespace = "com.alelk.pws.pwapp"
    applicationVariants.forEach { variant ->
        variant.resValue("string", "versionName", variant.versionName)
    }
}

dependencies {
    // project modules
    implementation(project(":database"))

    // support libraries
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.github.yukuku:ambilwarna:2.0.1")
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation(libs.room.runtime)
}
