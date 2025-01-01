import com.android.build.api.artifact.SingleArtifact
import com.android.build.gradle.internal.tasks.FinalizeBundleTask
import org.gradle.internal.extensions.stdlib.capitalized

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

  applicationVariants.forEach { variant ->
    variant.outputs.all {
      productFlavors.forEach { productFlavor ->
        val packageName = "pws-app-${variant.name}-${productFlavor.versionName}-${productFlavor.name}"
        val bundleFinalizeTaskName = "sign${productFlavor.name.capitalized()}${variant.buildType.name.capitalized()}Bundle"
        logger.info("packageName: $packageName, bundleFinalizeTaskName: $bundleFinalizeTaskName")
        tasks.named(bundleFinalizeTaskName, FinalizeBundleTask::class.java) {
          val file = finalBundleFile.asFile.get()
          val finalFile = File(file.parentFile, "$packageName.aab")
          finalBundleFile.set(finalFile)
        }
      }
    }
  }
}

dependencies {
  implementation(project(":domain"))
  implementation(project(":database"))
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
