plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
  jvm()
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