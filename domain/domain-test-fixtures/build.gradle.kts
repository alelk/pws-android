plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
  jvm()
  iosX64()
  iosArm64()
  iosSimulatorArm64()
  js(IR) {
    outputModuleName = "pws-domain-test-fixtures"
    browser()
    nodejs()
    binaries.executable()
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        api(project(":domain"))
        implementation(libs.kotest.assertions.core)
        api(libs.kotest.property)
      }
    }
  }
}