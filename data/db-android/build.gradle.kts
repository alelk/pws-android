plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.multiplatform")
  alias(libs.plugins.kotest.multiplatform)
}

kotlin {
  androidTarget()

  sourceSets {
    val androidMain by getting {
      dependencies {
        implementation(libs.pws.domain)
        api(libs.pws.dbRoom)
        implementation(libs.android.material)
        implementation(libs.kotlinx.datetime)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.coroutines.android)
        implementation(libs.timber)
      }
    }
    val androidUnitTest by getting {
      dependencies {
        implementation(libs.pws.dbRoomTestFixtures)
        implementation(libs.kotest.runner.junit5)
        implementation(libs.kotest.property)
        implementation(libs.kotest.assertions.core)
        implementation(libs.androidx.test.core)
        implementation(libs.kotest.runner.android)
        implementation(libs.kotest.extensions.android)
        implementation(libs.robolectric)
      }
    }
  }
}

tasks.withType<Test> {
  useJUnitPlatform()
}

android {
  compileSdk = rootProject.extra["sdkVersion"] as Int

  defaultConfig {
    resValue("string", "db_authority", "com.alelk.pws.database")
    buildConfigField("String", "DB_AUTHORITY", "\"com.alelk.pws.database\"")
    minSdk = 23
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  lint {
    targetSdk = rootProject.extra["sdkVersion"] as Int
  }

  buildTypes {
    getByName("release") {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
    }
    getByName("debug") {
      isMinifyEnabled = false
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }

  flavorDimensions.add("contentLevel")
  productFlavors {
    create("ru") {
      dimension = "contentLevel"
    }
    create("uk") {
      dimension = "contentLevel"
      resValue("string", "db_authority", "com.alelk.pws.database.uk")
      buildConfigField("String", "DB_AUTHORITY", "\"com.alelk.pws.database.uk\"")
    }
    create("full") {
      dimension = "contentLevel"
      resValue("string", "db_authority", "com.alelk.pws.database.full")
      buildConfigField("String", "DB_AUTHORITY", "\"com.alelk.pws.database.full\"")
    }
    create("rustore") {
      dimension = "contentLevel"
      resValue("string", "db_authority", "io.github.alelk.pws.database")
      buildConfigField("String", "DB_AUTHORITY", "\"io.github.alelk.pws.database\"")
    }
  }
  namespace = "io.github.alelk.pws.database"

  buildFeatures {
    buildConfig = true
  }
}