plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
  jvm()
  sourceSets {
    val commonMain by getting {
      dependencies {
      }
    }
    val jvmMain by getting {}

    val commonTest by getting {
      dependencies {
        implementation(project(":domain:domain-test-fixtures"))
        implementation(libs.kotest.property)
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
  }
}

tasks.withType<Test> {
  useJUnitPlatform()
}