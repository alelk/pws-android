plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("com.android.library")
}

kotlin {
  jvm()
  androidTarget()

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(project(":domain"))
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kaml)
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(libs.kotest.assertions.core)
        implementation(libs.kotest.framework.engine)
        implementation(libs.kotest.property)
      }
    }
  }
}


tasks.withType<Test> {
  useJUnitPlatform()
}

android {
  namespace = "io.github.alelk.pws.backup"
  compileSdk = rootProject.extra["sdkVersion"] as Int
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
}