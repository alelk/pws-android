plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose)
}

val catalogVersion: String = rootProject.file("catalog.version").readText().trim()
// Major version path (e.g. "v3") lets pws-catalog push updated bundles without
// requiring a new app release. Breaking changes bump the major → new path "v4",
// so apps pinned to "v3" keep receiving their own content indefinitely.
// Bundles are also mirrored on Cloudflare Pages; the app tries both in order.
//   - Catalog filename:  books-catalog-{variant}.json
//   - Bundle filename:   {bookId}-{variant}-{version}.book.yaml.gz.enc
val catalogMajor = catalogVersion.split(".").first()
val catalogGhPages    = "https://alelk.github.io/pws-catalog/v$catalogMajor"
val catalogCloudflare = "https://pws-catalog.pages.dev/v$catalogMajor"
val catalogYandex     = "https://pws-catalog.storage.yandexcloud.net/v$catalogMajor"
fun catalogUrl(variant: String) = listOf(catalogGhPages, catalogCloudflare, catalogYandex)
  .joinToString(",") { "$it/books-catalog-$variant.json" }

android {
  namespace = "io.github.alelk.pws.android.compose"
  compileSdk = rootProject.extra["sdkVersion"] as Int

  signingConfigs {
    create("release-ru") {
      keyAlias = project.findProperty("android.release.keyAliasRu") as String?
      keyPassword = project.findProperty("android.release.keyPassword") as String?
      storeFile = (project.findProperty("android.release.keystorePath") as String?)?.let(::file)
      storePassword = project.findProperty("android.release.storePassword") as String?
    }
    create("release-uk") {
      keyAlias = project.findProperty("android.release.keyAliasUk") as String?
      keyPassword = project.findProperty("android.release.keyPassword") as String?
      storeFile = (project.findProperty("android.release.keystorePath") as String?)?.let(::file)
      storePassword = project.findProperty("android.release.storePassword") as String?
    }
    create("release-rustore") {
      keyAlias = project.findProperty("android.release.keyAliasRuRustore") as String?
      keyPassword = project.findProperty("android.release.keyPasswordRustore") as String?
      storeFile = (project.findProperty("android.release.keystorePathRustore") as String?)?.let(::file)
      storePassword = project.findProperty("android.release.storePasswordRustore") as String?
    }
  }

  defaultConfig {
    applicationId = "com.alelk.pws.pwapp"
    minSdk = 23
    targetSdk = rootProject.extra["sdkVersion"] as Int
    versionCode = rootProject.extra["versionCode"] as Int
    versionName = "${rootProject.extra["versionName"]}-${rootProject.extra["versionNameSuffix"]}"
    resValue("string", "db_authority", "com.alelk.pws.database")
  }

  flavorDimensions.add("contentLevel")

  productFlavors {
    create("ru") {
      dimension = "contentLevel"
    }
    create("full") {
      dimension = "contentLevel"
      applicationIdSuffix = ".full"
      versionNameSuffix = "-full"
      resValue("string", "db_authority", "com.alelk.pws.database.full")
    }
    create("uk") {
      dimension = "contentLevel"
      applicationIdSuffix = ".uk"
      versionNameSuffix = "-uk"
      resValue("string", "db_authority", "com.alelk.pws.database.uk")
    }
    create("rustore") {
      dimension = "contentLevel"
      applicationId = "io.github.alelk.pws.app"
      versionNameSuffix = "-rustore"
      resValue("string", "db_authority", "io.github.alelk.pws.database")
    }
  }

  buildTypes {
    getByName("release") {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      productFlavors.getByName("ru").signingConfig = signingConfigs.getByName("release-ru")
      productFlavors.getByName("full").signingConfig = signingConfigs.getByName("release-ru")
      productFlavors.getByName("uk").signingConfig = signingConfigs.getByName("release-uk")
      productFlavors.getByName("rustore").signingConfig = signingConfigs.getByName("release-rustore")
      buildConfigField("String", "CATALOG_URLS", "\"${catalogUrl("release")}\"")
      buildConfigField("String", "BUNDLE_VARIANT", "\"release\"")
    }
    getByName("debug") {
      isDebuggable = true
      isMinifyEnabled = false
      versionNameSuffix = "-debug"
      buildConfigField("String", "CATALOG_URLS", "\"${catalogUrl("debug")}\"")
      buildConfigField("String", "BUNDLE_VARIANT", "\"debug\"")
    }
    create("localSeed") {
      isDebuggable = true
      isMinifyEnabled = false
      versionNameSuffix = "-localSeed"
      buildConfigField("String", "CATALOG_URLS", "\"${catalogUrl("debug")}\"")
      buildConfigField("String", "BUNDLE_VARIANT", "\"debug\"")
    }
  }

  buildFeatures {
    compose = true
    resValues = true
    buildConfig = true
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }

  testOptions {
    unitTests.isIncludeAndroidResources = true
  }
}

kotlin {
  jvmToolchain(21)
}

androidComponents {
  onVariants { variant ->
    variant.resValues.put(
      variant.makeResValueKey("string", "versionName"),
      com.android.build.api.variant.ResValue(variant.name)
    )
  }
}

dependencies {
  // pws-core modules
  implementation(libs.pws.features)
  implementation(libs.pws.repoRoom)
  implementation(libs.pws.dbRoom)
  implementation(libs.pws.domain)
  implementation(libs.pws.portableData)

  // local db provider from :data:db-android
  implementation(project(":data:db-android"))
  implementation(project(":data:content-delivery"))

  // Koin DI
  implementation(libs.koin.android)
  implementation(libs.koin.compose)
  implementation(libs.voyager.navigator)
  implementation(libs.voyager.koin)

  // Android
  implementation(libs.appcompat)
  implementation(libs.activity.compose)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.datetime)
  implementation(libs.datastore.preferences)

  // Compose BOM
  implementation(platform(libs.compose.bom))
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-tooling-preview")
  implementation("androidx.compose.foundation:foundation")
  implementation(libs.material3)
  debugImplementation("androidx.compose.ui:ui-tooling")
  debugImplementation("androidx.compose.ui:ui-test-manifest")

  testImplementation(libs.pws.dbRoomTestFixtures)
  testImplementation(libs.kotest.runner.junit5)
  testImplementation(libs.kotest.assertions.core)
  testImplementation(libs.kotest.property)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.androidx.test.core)
  testImplementation(libs.kotest.runner.android)
  testImplementation(libs.kotest.extensions.android)
  testImplementation(libs.robolectric)
}

tasks.withType<Test> {
  useJUnitPlatform()
}



