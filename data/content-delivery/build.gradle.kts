plugins {
  id("com.android.library")
  alias(libs.plugins.kotest.multiplatform)
}

android {
  namespace = "io.github.alelk.pws.contentdelivery"
  compileSdk = rootProject.extra["sdkVersion"] as Int

  defaultConfig {
    minSdk = 23
  }

  buildFeatures {
    buildConfig = false
  }

  buildTypes {
    create("localSeed") {
      // mirrors localSeed from :data:db-android and :app-compose for variant resolution
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }

  testOptions {
    unitTests.isIncludeAndroidResources = true
    unitTests.all {
      it.jvmArgs(
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED",
        "--add-opens=java.base/java.io=ALL-UNNAMED",
        "--add-opens=java.base/java.net=ALL-UNNAMED",
        "--add-opens=java.base/java.security=ALL-UNNAMED",
        "--add-opens=java.base/java.text=ALL-UNNAMED",
        "--add-opens=java.base/java.nio=ALL-UNNAMED",
        "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
        "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
        "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
        "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED"
      )
    }
  }
}

kotlin {
  jvmToolchain(21)
}

dependencies {
  implementation(libs.pws.domain)
  implementation(libs.pws.dbRoom)
  implementation(libs.pws.portableData)
  implementation(libs.room.runtime)
  implementation("androidx.room:room-ktx:${libs.versions.room.get()}")

  implementation(libs.ktor.client.core)
  implementation(libs.ktor.client.cio)
  implementation(libs.ktor.client.contentNegotiation)
  implementation(libs.ktor.serialization.kotlinx.json)

  implementation(libs.koin.android)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.timber)

  testImplementation(libs.pws.dbRoomTestFixtures)
  testImplementation(libs.kotest.runner.junit5)
  testImplementation(libs.kotest.property)
  testImplementation(libs.kotest.assertions.core)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.kotlinx.datetime)
  testImplementation(libs.ktor.client.mock)
  testImplementation(libs.androidx.test.core)
  testImplementation(libs.kotest.runner.android)
  testImplementation(libs.kotest.extensions.android)
  testImplementation(libs.robolectric)
}

tasks.withType<Test> {
  useJUnitPlatform()
}
