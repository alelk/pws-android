plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose)
}

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
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      productFlavors.getByName("ru").signingConfig = signingConfigs.getByName("release-ru")
      productFlavors.getByName("full").signingConfig = signingConfigs.getByName("release-ru")
      productFlavors.getByName("uk").signingConfig = signingConfigs.getByName("release-uk")
      productFlavors.getByName("rustore").signingConfig = signingConfigs.getByName("release-rustore")
    }
    getByName("debug") {
      isDebuggable = true
      isMinifyEnabled = false
      versionNameSuffix = "-debug"
    }
  }

  buildFeatures {
    compose = true
    resValues = true
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
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
  implementation(libs.pws.backup)

  // local db provider from :data:db-android
  implementation(project(":data:db-android"))

  // Koin DI
  implementation(libs.koin.android)
  implementation(libs.koin.compose)
  implementation(libs.voyager.navigator)
  implementation(libs.voyager.koin)

  // Android
  implementation(libs.appcompat)
  implementation(libs.activity.compose)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.datastore.preferences)

  // Compose BOM
  implementation(platform(libs.compose.bom))
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-tooling-preview")
  implementation("androidx.compose.foundation:foundation")
  implementation(libs.material3)
  debugImplementation("androidx.compose.ui:ui-tooling")
  debugImplementation("androidx.compose.ui:ui-test-manifest")
}



