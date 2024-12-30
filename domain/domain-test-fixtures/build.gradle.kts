plugins {
  kotlin("multiplatform")
  id("com.android.library")
}

kotlin {
  jvm()
  androidTarget()

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(project(":domain"))
        implementation(libs.kotest.property)
      }
    }
  }
}

android {
  namespace = "io.github.alelk.pws.domain.test_fixtures"
  compileSdk = rootProject.extra["sdkVersion"] as Int
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
}