plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("com.android.library")
  alias(libs.plugins.kotlin.serialization)
}

kotlin {
  jvm()
  iosX64()
  iosArm64()
  iosSimulatorArm64()
  androidTarget()
  js(IR) {
    outputModuleName = "pws-domain"
    browser()
    nodejs()
    binaries.executable()
  }

  sourceSets {
    commonMain.dependencies {
      implementation(libs.kotlinx.serialization.core)
    }

    jvmMain.dependencies {}
    iosArm64Main.dependencies {}
    androidMain.dependencies {}

    commonTest.dependencies {
      implementation(project(":domain:domain-test-fixtures"))
      implementation(libs.kotlinx.serialization.json)
      implementation(libs.kotest.property)
      implementation(libs.kotest.assertions.core)
      implementation(libs.kotest.assertions.json)
      implementation(libs.kotest.framework.engine)
      implementation(kotlin("test-common"))
      implementation(kotlin("test-annotations-common"))
    }

    jvmTest.dependencies {
      implementation(libs.kotest.runner.junit5)
    }

    androidUnitTest.dependencies {
      runtimeOnly(libs.kotest.runner.junit5)
    }
  }
}

android {
  namespace = "io.github.alelk.pws.domain"

  compileSdk = rootProject.extra["sdkVersion"] as Int
  defaultConfig {
    minSdk = 21
  }

  lint {
    targetSdk = rootProject.extra["sdkVersion"] as Int
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
}

tasks.withType<Test> {
  useJUnitPlatform()
}