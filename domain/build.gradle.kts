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
        implementation(libs.kotlinx.coroutines.core)
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(libs.kotest.assertions.core)
        implementation(libs.kotest.framework.engine)
        implementation(libs.kotest.property)
        //implementation(project(":domain:domain-test-fixtures"))
        //implementation(kotlin("test-common"))
        //implementation(kotlin("test-annotations-common"))
      }
    }
  }
}


tasks.withType<Test> {
  useJUnitPlatform()
}

android {
  namespace = "io.github.alelk.pws.domain"
  compileSdk = rootProject.extra["sdkVersion"] as Int
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
}