plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  alias(libs.plugins.hilt)
  id("kotlin-kapt")
}


android {
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
  }

  defaultConfig {
    applicationId = "com.alelk.pws.pwapp"
    resValue("string", "db_authority", "com.alelk.pws.database")
    minSdk = 23
    targetSdk = rootProject.extra["sdkVersion"] as Int
    versionCode = rootProject.extra["versionCode"] as Int
    versionName = "${rootProject.extra["versionName"]}-${rootProject.extra["versionNameSuffix"]}"
  }

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

  buildTypes {
    getByName("release") {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
      productFlavors.getByName("ru").signingConfig = signingConfigs.getByName("release-ru")
      productFlavors.getByName("uk").signingConfig = signingConfigs.getByName("release-uk")
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

  namespace = "io.github.alelk.pws.android.app"
  applicationVariants.forEach { variant ->
    variant.resValue("string", "versionName", variant.versionName)
  }

  viewBinding {
    enable = true
  }
}

dependencies {
  implementation(project(":domain"))
  implementation(project(":data:android-database"))
  implementation(project(":backup"))

  implementation(libs.preference.ktx)
  implementation(libs.lifecycle.viewmodel.ktx)
  implementation(libs.material)
  implementation(libs.appcompat)
  implementation(libs.ambilwarna)
  implementation(libs.flexbox)
  implementation(libs.room.runtime)
  implementation(libs.timber)
  implementation(libs.datastore.preferences)
  implementation(libs.navigation.fragment)
  implementation(libs.navigation.ui)
  implementation(libs.androidx.navigation.fragment.ktx)
  implementation(libs.androidx.navigation.ui.ktx)
  implementation(libs.kotlinx.datetime)

  // DI
  implementation(libs.hilt.android)
  kapt(libs.hilt.compiler)

  // Test dependencies
  testImplementation(libs.kotest.runner.junit5)
  testImplementation(libs.kotest.assertions.core)
  testImplementation(libs.kotest.framework.datatest)
  testImplementation(libs.kotest.property)
  testImplementation(libs.robolectric)
  testImplementation(libs.androidx.test.core)
  testImplementation(libs.room.testing)
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
}

kapt {
  correctErrorTypes = true
}
