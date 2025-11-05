import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget

plugins {
  id("com.google.devtools.ksp") version "${libs.versions.kotlin.get()}-${libs.versions.ksp.get()}"
  id("com.android.library")
  id("org.jetbrains.kotlin.multiplatform")
  alias(libs.plugins.kotest.multiplatform)
  id("maven-publish")
}

kotlin {
  androidTarget()
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
        api(project(":domain"))
        api(libs.room.runtime)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.datetime)
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(project(":domain:domain-test-fixtures"))
        implementation(project(":data:room-database:database-test-fixtures"))
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
        implementation(libs.robolectric)
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
  add("kspIosSimulatorArm64", libs.room.compiler)
  add("kspIosX64", libs.room.compiler)
  add("kspIosArm64", libs.room.compiler)
}

tasks.withType<Test> {
  useJUnitPlatform()
}

android {
  compileSdk = rootProject.extra["sdkVersion"] as Int

  defaultConfig {
    minSdk = 23
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  lint {
    targetSdk = rootProject.extra["sdkVersion"] as Int
  }

  buildTypes {
    getByName("release") {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
    }
    getByName("debug") {
      isMinifyEnabled = false
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
  namespace = "io.github.alelk.pws.data.room_database"
  ksp {
    arg("room.generateKotlin", "true")
  }
}