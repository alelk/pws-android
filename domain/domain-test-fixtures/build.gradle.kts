import shadow.bundletool.com.android.tools.r8.internal.no

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
        implementation(project(":domain"))
        implementation(libs.kotest.assertions.core)
        implementation(libs.kotest.property)
      }
    }
  }
}