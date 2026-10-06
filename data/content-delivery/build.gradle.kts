plugins {
  id("pws.android.library")
  alias(libs.plugins.kotest.multiplatform)
}

android {
  namespace = "io.github.alelk.pws.contentdelivery"

  buildFeatures {
    buildConfig = false
  }
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
