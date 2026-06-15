import java.security.SecureRandom
import java.util.Properties

plugins {
  id("com.android.library")
  alias(libs.plugins.kotest.multiplatform)
}

// ── Decrypt keys ──────────────────────────────────────────────────────────────
// Debug key is public — committed to source control, anyone can build & run.
// Prod key must NEVER be committed. Sources (in priority order):
//   1. Environment variable DB_DECRYPT_KEY_PROD  (CI / GitHub Actions secret)
//   2. local.properties → db.decrypt.key.prod    (local dev only, not in VCS)
val localProps = Properties().also { props ->
  rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { props.load(it) }
}
fun localOrEnv(propKey: String, envKey: String): String? =
  localProps.getProperty(propKey) ?: System.getenv(envKey)

// Public debug key — safe to embed in source.
// Generated once for the debug dataset; corresponds to -debug*.dbz.enc files.
val dbDecryptKeyDebug = "7b4cabf2662f9f7ffab9e916fdd0d64a184a70c7cd5f585cf89501ca474bd995"

// Prod key — loaded from env/local.properties, never hardcoded.
val dbDecryptKeyProd: String = localOrEnv("db.decrypt.key.prod", "DB_DECRYPT_KEY_PROD") ?: ""

// Asset version — tracks the book-content version in pws-docs releases (db.version file).
// Decoupled from PwsDatabaseProvider.DB_VERSION (app-side DB filename, changes with schema bumps).
val dbAssetVersion: String = rootProject.file("db.version").readText().trim()

// XOR-obfuscate the hex key so its literal string does not appear in the DEX.
// Returns a pair (maskedField, maskField) as Java byte-array initialiser literals.
// The mask is freshly random on every Gradle invocation — each APK build gets different bytes.
fun xorObfuscate(hexKey: String): Pair<String, String> {
  val keyBytes = ByteArray(hexKey.length / 2) { i -> hexKey.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
  val mask = ByteArray(keyBytes.size).also { SecureRandom().nextBytes(it) }
  val masked = ByteArray(keyBytes.size) { i -> (keyBytes[i].toInt() xor mask[i].toInt()).toByte() }
  fun ByteArray.toLiteral() = "{" + joinToString(",") { "(byte)0x${"%02x".format(it.toInt() and 0xFF)}" } + "}"
  return masked.toLiteral() to mask.toLiteral()
}

android {
  namespace = "io.github.alelk.pws.database"
  compileSdk = rootProject.extra["sdkVersion"] as Int

  defaultConfig {
    minSdk = 23
    buildConfigField("String", "DB_AUTHORITY", "\"com.alelk.pws.database\"")
    resValue("string", "db_authority", "com.alelk.pws.database")
    // Version of the .dbz.enc asset in pws-docs — may differ from DB_VERSION (app-side DB name).
    buildConfigField("String", "DB_ASSET_VERSION", "\"$dbAssetVersion\"")
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
      val (masked, mask) = xorObfuscate(dbDecryptKeyProd)
      buildConfigField("byte[]", "DB_KEY_MASKED", masked)
      buildConfigField("byte[]", "DB_KEY_MASK", mask)
    }
    getByName("debug") {
      isMinifyEnabled = false
      val (masked, mask) = xorObfuscate(dbDecryptKeyDebug)
      buildConfigField("byte[]", "DB_KEY_MASKED", masked)
      buildConfigField("byte[]", "DB_KEY_MASK", mask)
    }
  }


  testOptions {
    unitTests.isIncludeAndroidResources = true
  }
}

dependencies {
  implementation(libs.pws.domain)
  implementation(libs.pws.dbRoom)
  implementation(libs.pws.portableData)
  implementation(libs.sqlcipher)
  implementation(libs.security.crypto)
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
