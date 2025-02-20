plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("com.android.library")
  alias(libs.plugins.kotlin.serialization)
}

kotlin {
  jvm()
  iosArm64()
  androidTarget()

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(libs.kotlinx.serialization.core)
      }
    }
    val jvmMain by getting {}
    val iosArm64Main by getting {}
    val androidMain by getting {}

    val commonTest by getting {
      dependencies {
        implementation(project(":domain:domain-test-fixtures"))
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.kotest.property)
        implementation(libs.kotest.assertions.core)
        implementation(libs.kotest.assertions.json)
        implementation(libs.kotest.framework.engine)
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
      }
    }

    val jvmTest by getting {
      dependencies {
        implementation(libs.kotest.runner.junit5)
      }
    }

    val androidUnitTest by getting {
      dependencies {
        runtimeOnly(libs.kotest.runner.junit5)
      }
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