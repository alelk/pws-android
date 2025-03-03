plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
  jvm()
  iosX64()
  iosArm64()
  iosSimulatorArm64()
  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(project(":domain"))
        implementation(libs.kotest.assertions.core)
        implementation(libs.kotest.property)
      }
    }
  }
}