import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget

plugins {
  id("com.google.devtools.ksp") version "${libs.versions.kotlin.get()}-${libs.versions.ksp.get()}"
  id("com.android.library")
  id("org.jetbrains.kotlin.multiplatform")
  alias(libs.plugins.kotest.multiplatform)
  id("maven-publish")
}

kotlin {
  androidTarget {
    publishLibraryVariants("ruRelease")
  }
  jvm()

  iosX64()
  iosArm64()
  iosSimulatorArm64()

  sourceSets {

    all {
      languageSettings.optIn("kotlin.experimental.ExperimentalObjCName")
    }

    val commonMain by getting {
      dependencies {
        implementation(project(":domain"))
        implementation(libs.room.runtime)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.datetime)
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(project(":domain:domain-test-fixtures"))
        implementation(project(":database:database-test-fixtures"))
        implementation(libs.kotest.assertions.core)
        implementation(libs.kotest.framework.engine)
        implementation(libs.kotest.property)
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
      }
    }
    val androidMain by getting {
      dependencies {
        implementation(libs.android.material)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.coroutines.android)
        runtimeOnly(libs.room.runtime)
        runtimeOnly(libs.room.ktx)
        implementation(libs.timber)
      }
    }
    val androidUnitTest by getting {
      dependencies {
        implementation(libs.kotest.runner.junit5)
        implementation(libs.kotest.property)
        implementation(libs.kotest.assertions.core)
        implementation(libs.androidx.test.core)
        implementation(libs.kotest.runner.android)
        implementation(libs.kotest.extensions.android)
      }
    }
    val jvmMain by getting {
      dependencies {
        implementation(libs.kotlinx.coroutines.core)
        runtimeOnly(libs.room.runtime.jvm)
      }
    }
    val jvmTest by getting {
      dependencies {
        implementation(libs.kotest.runner.junit5)
        implementation(libs.sqlite.bundled)
      }
    }
    val iosX64Main by getting {}
    val iosArm64Main by getting {}
    val iosSimulatorArm64Main by getting {}
    val iosArm64Test by getting {}
  }

  targets.withType<KotlinAndroidTarget> {
    ksp {
      arg("room.generateKotlin", "true")
    }
  }
}

dependencies {
  add("kspAndroid", libs.room.compiler)
  add("kspJvm", libs.room.compiler)

  // todo: fix native compilation
  // add("kspIosSimulatorArm64", libs.room.compiler)
  // add("kspIosX64", libs.room.compiler)
  // add("kspIosArm64", libs.room.compiler)
}

tasks.withType<Test> {
  useJUnitPlatform()
}

android {
  compileSdk = rootProject.extra["sdkVersion"] as Int

  defaultConfig {
    resValue("string", "db_authority", "com.alelk.pws.database")
    buildConfigField("String", "DB_AUTHORITY", "\"com.alelk.pws.database\"")
    minSdk = 21
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  lint {
    targetSdk = rootProject.extra["sdkVersion"] as Int
  }

  buildTypes {
    getByName("release") {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
      //signingConfig = signingConfigs.getByName("release-ru")
    }
    getByName("debug") {
      isMinifyEnabled = false
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
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
  namespace = "io.github.alelk.pws.database"

  buildFeatures {
    buildConfig = true
  }
  ksp {
    arg("room.generateKotlin", "true")
  }
}