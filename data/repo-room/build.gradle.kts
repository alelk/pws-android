import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget

plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.multiplatform")
  alias(libs.plugins.kotest.multiplatform)
}

kotlin {
  androidTarget()
  jvm()

  iosX64()
  iosArm64()
  iosSimulatorArm64()

  sourceSets {

    val commonMain by getting {
      dependencies {
        api(project(":domain"))
        implementation(project(":data:db-room"))
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(project(":data:db-room:db-room-test-fixtures"))
        implementation(libs.kotest.assertions.core)
        implementation(libs.kotest.framework.engine)
        implementation(libs.kotest.property)
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
      }
    }
    val androidMain by getting {
      dependencies {
      }
    }
    val androidUnitTest by getting {
      dependencies {
      }
    }
    val jvmMain by getting {
      dependencies {
      }
    }
    val jvmTest by getting {
      dependencies {
      }
    }
    val iosX64Main by getting {}
    val iosArm64Main by getting {}
    val iosSimulatorArm64Main by getting {}
    val iosArm64Test by getting {}
  }

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
  namespace = "io.github.alelk.pws.data.repository.room"
}