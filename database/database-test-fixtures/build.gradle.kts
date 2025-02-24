plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
  jvm()
  iosArm64()

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(project(":database"))
        implementation(project(":domain"))
        implementation(project(":domain:domain-test-fixtures"))
        implementation(libs.room.runtime)
        implementation(libs.kotlinx.datetime)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotest.property)
        implementation(libs.kotest.property.datetime)
      }
    }
  }
}