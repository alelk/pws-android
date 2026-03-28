plugins {
  id("com.android.library")
  alias(libs.plugins.kotest.multiplatform)
}

android {
  namespace = "io.github.alelk.pws.database"
  compileSdk = rootProject.extra["sdkVersion"] as Int

  defaultConfig {
    minSdk = 23
    buildConfigField("String", "DB_AUTHORITY", "\"com.alelk.pws.database\"")
    resValue("string", "db_authority", "com.alelk.pws.database")
  }

  buildFeatures {
    buildConfig = true
    resValues = true
  }

  lint {
    targetSdk = rootProject.extra["sdkVersion"] as Int
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

  buildTypes {
    getByName("release") {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
    getByName("debug") {
      isMinifyEnabled = false
    }
  }


  testOptions {
    unitTests.isIncludeAndroidResources = true
  }
}

dependencies {
  implementation(libs.pws.domain)
  implementation(libs.pws.dbRoom)
  implementation(libs.android.material)
  implementation(libs.kotlinx.datetime)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.timber)

  testImplementation(libs.pws.dbRoomTestFixtures)
  testImplementation(libs.kotest.runner.junit5)
  testImplementation(libs.kotest.property)
  testImplementation(libs.kotest.assertions.core)
  testImplementation(libs.androidx.test.core)
  testImplementation(libs.kotest.runner.android)
  testImplementation(libs.kotest.extensions.android)
  testImplementation(libs.robolectric)
}

tasks.withType<Test> {
  useJUnitPlatform()
}
