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
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") version "${libs.versions.kotlin.get()}-${libs.versions.ksp.get()}"
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
        resValue("string", "db_authority", "com.alelk.pws.database")
        buildConfigField("String", "DB_AUTHORITY", "\"com.alelk.pws.database\"")
        minSdk = 21
    }

    lint {
        targetSdk = rootProject.extra["sdkVersion"] as Int
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release-ru")
        }
        getByName("debug") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    flavorDimensions.add("contentLevel")
    productFlavors {
        create("ru") {
            dimension = "contentLevel"
        }
        create("uk") {
            dimension = "contentLevel"
            resValue("string", "db_authority", "com.alelk.pws.database.uk")
            buildConfigField("String", "DB_AUTHORITY", "\"com.alelk.pws.database.uk\"")
        }
        create("full") {
            dimension = "contentLevel"
            resValue("string", "db_authority", "com.alelk.pws.database.full")
            buildConfigField("String", "DB_AUTHORITY", "\"com.alelk.pws.database.full\"")
        }
    }
    namespace = "com.alelk.pws.database"

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

dependencies {
    implementation(libs.android.material)
    implementation(libs.room.runtime)
    testImplementation(libs.kotest.runner.junit5)
}
