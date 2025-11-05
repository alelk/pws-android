plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("com.android.library")
  alias (libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

kotlin {
  jvm()
  iosX64()
  iosArm64()
  iosSimulatorArm64()
  androidTarget()
  js(IR) {
    outputModuleName = "pws-features"
    browser()
    nodejs()
    binaries.executable()
  }

  sourceSets {
    val commonMain by getting {
      dependencies { implementation(project(":domain")) }
    }
    val androidMain by getting {
      dependencies {
        implementation(libs.compose.ui)
        implementation(libs.compose.foundation)
        implementation(libs.compose.material3)
        implementation(libs.compose.ui.tooling.preview)
        implementation(libs.compose.navigation)
        implementation(libs.compose.material.icons.core)
        implementation(libs.compose.material.icons.extended)
        implementation(libs.activity.compose)
        implementation(libs.lifecycle.viewmodel.ktx)
      }
    }
    jvmMain.dependencies {}
    iosArm64Main.dependencies {}

    commonTest.dependencies {}

    jvmTest.dependencies {
      implementation(libs.kotest.runner.junit5)
    }

    androidUnitTest.dependencies {
      runtimeOnly(libs.kotest.runner.junit5)
    }
  }
}

android {
  namespace = "io.github.alelk.pws.features"

  compileSdk = rootProject.extra["sdkVersion"] as Int
  defaultConfig {
    minSdk = 24
  }

  buildFeatures { compose = true }

  lint {
    targetSdk = rootProject.extra["sdkVersion"] as Int
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
}

dependencies {
  // Preview tooling only for debug builds
  debugImplementation(libs.compose.ui.tooling)
}

tasks.withType<Test> {
  useJUnitPlatform()
}