plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
}


android {
  compileSdk = rootProject.extra["sdkVersion"] as Int

  signingConfigs {
    create("release-ru") {
      keyAlias = project.findProperty("android.release.keyAliasRu") as String?
      keyPassword = project.findProperty("android.release.keyPassword") as String?
      storeFile =
        (project.findProperty("android.release.keystorePath") as String?)?.let(::file)
      storePassword = project.findProperty("android.release.storePassword") as String?
    }
  }

  defaultConfig {
    applicationId = "com.alelk.pws.pwapp"
    resValue("string", "db_authority", "com.alelk.pws.database")
    minSdk = 21
    targetSdk = rootProject.extra["sdkVersion"] as Int
    versionCode = rootProject.extra["versionCode"] as Int
    versionName =
      "${rootProject.extra["versionName"]}-${rootProject.extra["versionNameSuffix"]}"
  }

  buildTypes {
    getByName("release") {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release-ru")
    }
    getByName("debug") {
      isDebuggable = true
      isMinifyEnabled = false
      versionNameSuffix = "-debug"
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }

  sourceSets {
    getByName("main") {
      assets.srcDirs("src/main/assets")
    }
  }

  flavorDimensions.add("contentLevel")

  productFlavors {
    create("ru") {
      dimension = "contentLevel"
    }
    create("uk") {
      dimension = "contentLevel"
      applicationIdSuffix = ".uk"
      versionNameSuffix = "-uk"
      resValue("string", "db_authority", "com.alelk.pws.database.uk")
    }
    create("full") {
      dimension = "contentLevel"
      applicationIdSuffix = ".full"
      versionNameSuffix = "-full"
      resValue("string", "db_authority", "com.alelk.pws.database.full")
    }
  }

  namespace = "com.alelk.pws.pwapp"
  applicationVariants.forEach { variant ->
    variant.resValue("string", "versionName", variant.versionName)
  }

  viewBinding {
    enable = true
  }
}

dependencies {
  implementation(project(":database"))

  implementation(libs.preference.ktx)
  implementation(libs.lifecycle.viewmodel.ktx)
  implementation(libs.material)
  implementation(libs.appcompat)
  implementation(libs.ambilwarna)
  implementation(libs.flexbox)
  implementation(libs.room.runtime)
  implementation(libs.timber)
  implementation(libs.datastore.preferences)
  implementation(libs.jackson.kotlin)
  implementation(libs.jackson.databind)
}
