import com.android.build.api.artifact.SingleArtifact
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

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
// Catalog mirrors for a bundle variant (release|debug), in fallback order.
fun catalogUrlsFor(variant: String): List<String> =
  listOf(catalogGhPages, catalogCloudflare, catalogYandex).map { "$it/books-catalog-$variant.json" }
fun catalogUrl(variant: String) = catalogUrlsFor(variant).joinToString(",")

// Books preloaded straight into the APK for specific flavors. For each listed flavor the
// `generateSeedBundles<Variant>` task downloads the named bundles from the catalog at build time and
// bakes them into `assets/seed-books/`, so the app ships with that content already installed as a
// non-removable built-in (source=ASSET, imported on first launch by SeedBooksFromAssetsUseCase).
// Flavors absent from this map produce the universal "clean" APK, unchanged.
//   - key   = product flavor name (contentLevel dimension): ru | uk | full | rustore
//   - value = book IDs that must exist in books-catalog-{release|debug}.json
// AppMetrica API key — the SDK's write key. Never committed: supply it via the
// `appmetrica.apiKey` Gradle property (local.properties / ~/.gradle/gradle.properties) or the
// APPMETRICA_API_KEY environment variable (CI secret). When it is absent the app compiles and runs
// exactly as before: PwsComposeApplication skips SDK activation and binds NoOpTelemetry.
val appMetricaApiKey: String =
  (project.findProperty("appmetrica.apiKey") as String?)
    ?: System.getenv("APPMETRICA_API_KEY")
    ?: ""

val seedBooksByFlavor: Map<String, List<String>> = mapOf(
  "rustore" to listOf("PV3300"),   // Песнь Возрождения 3300 (main Russian songbook)
  "uk" to listOf("Psalmovivi"),    // Псалмоспіви (main Ukrainian songbook)
)

/**
 * Downloads the seed bundles for one variant from the catalog and writes them into
 * `<outputDir>/seed-books/`, which AGP wires into the variant's merged assets. Bundle file names
 * (`{bookId}-{bundleVariant}-{catalogVersion}.book.yaml.gz.enc`) are resolved from the catalog's
 * top-level `version`; each download is verified against the catalog `checksum` (SHA-256).
 *
 * Up-to-date checking is keyed on the declared inputs (book IDs / bundle variant / catalog URLs).
 * The remote catalog version is not an input, so run `clean` (or bump the seed list) to force a
 * refresh when the catalog publishes newer bundles.
 */
abstract class DownloadSeedBundlesTask : DefaultTask() {
  @get:Input abstract val bookIds: ListProperty<String>
  @get:Input abstract val bundleVariant: Property<String>
  @get:Input abstract val catalogUrls: ListProperty<String>
  @get:OutputDirectory abstract val outputDir: DirectoryProperty

  @TaskAction
  fun download() {
    val seedDir = outputDir.get().dir("seed-books").asFile
    seedDir.deleteRecursively()
    seedDir.mkdirs()

    val ids = bookIds.get()
    if (ids.isEmpty()) return
    val variant = bundleVariant.get()
    val urls = catalogUrls.get()

    // Fetch the catalog from the first reachable mirror.
    val catalog = urls.firstNotNullOfOrNull { url ->
      runCatching { url to String(fetch(url), Charsets.UTF_8) }
        .onFailure { logger.warn("Seed: catalog fetch failed from $url: ${it.message}") }
        .getOrNull()
    } ?: error("Seed: cannot fetch catalog for variant '$variant' from any mirror: $urls")
    val (catalogUrl, catalogJson) = catalog

    @Suppress("UNCHECKED_CAST")
    val parsed = groovy.json.JsonSlurper().parseText(catalogJson) as Map<String, Any?>
    val catalogVersion = parsed["version"] as? String
      ?: error("Seed: catalog has no 'version' field ($catalogUrl)")
    @Suppress("UNCHECKED_CAST")
    val books = parsed["books"] as? List<Map<String, Any?>>
      ?: error("Seed: catalog has no 'books' array ($catalogUrl)")

    val checksumById: Map<String, String> = books.associate { entry ->
      @Suppress("UNCHECKED_CAST")
      val book = entry["book"] as Map<String, Any?>
      (book["id"] as String) to (entry["checksum"] as String)
    }

    val base = catalogUrl.substringBeforeLast('/')
    ids.forEach { id ->
      val expected = checksumById[id]
        ?: error("Seed: book '$id' not found in catalog for variant '$variant' ($catalogUrl)")
      val fileName = "$id-$variant-$catalogVersion.book.yaml.gz.enc"
      val bytes = fetch("$base/$fileName")
      val actual = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it) }
      check(actual == expected) {
        "Seed: checksum mismatch for $fileName — expected $expected, got $actual"
      }
      seedDir.resolve(fileName).writeBytes(bytes)
      logger.lifecycle("Seed: baked $fileName (${bytes.size} bytes) into assets/seed-books/")
    }
  }

  private fun fetch(url: String): ByteArray {
    val conn = URI(url).toURL().openConnection() as HttpURLConnection
    conn.setRequestProperty("User-Agent", "pws-android-build/1.0 (+github.com/alelk/pws-android)")
    conn.setRequestProperty("Accept", "application/octet-stream, application/json")
    conn.connectTimeout = 30_000
    conn.readTimeout = 120_000
    conn.instanceFollowRedirects = true
    try {
      val code = conn.responseCode
      check(code in 200..299) { "HTTP $code for $url" }
      return conn.inputStream.use { it.readBytes() }
    } finally {
      conn.disconnect()
    }
  }
}

/**
 * Copies a minified variant's R8 `mapping.txt` out of `build/` into a stable, release-labelled
 * location so it can be uploaded to the AppMetrica console (Settings → "Mapping files"). Without a
 * mapping, release crash reports arrive obfuscated and are effectively unreadable.
 *
 * We deliberately do not use the official AppMetrica Gradle plugin: its current release (1.0.1)
 * drives the removed `com.android.build.gradle.api.ApplicationVariant` API and does not work on
 * AGP 9. Staging the file is AGP-version-proof; the upload itself is a manual (or CI) step,
 * documented in docs/monitoring.md.
 */
abstract class StageMappingFileTask : DefaultTask() {
  @get:InputFile abstract val mappingFile: RegularFileProperty
  @get:OutputFile abstract val stagedFile: RegularFileProperty

  @TaskAction
  fun stage() {
    val target = stagedFile.get().asFile
    target.parentFile?.mkdirs()
    mappingFile.get().asFile.copyTo(target, overwrite = true)
    logger.lifecycle("AppMetrica: mapping staged at ${target.absolutePath} — upload it to the AppMetrica console for this release")
  }
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
    buildConfigField("String", "APPMETRICA_API_KEY", "\"$appMetricaApiKey\"")
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

    // Minified variants: stage mapping.txt under output/appmetrica-mapping/, named by variant and
    // release, so the file uploaded to AppMetrica is unambiguously tied to a versionName/versionCode.
    if (variant.buildType == "release") {
      val release = "${rootProject.extra["versionName"]}-${rootProject.extra["versionCode"]}"
      tasks.register<StageMappingFileTask>(
        "stageAppMetricaMapping${variant.name.replaceFirstChar { it.uppercase() }}"
      ) {
        mappingFile.set(variant.artifacts.get(SingleArtifact.OBFUSCATION_MAPPING_FILE))
        stagedFile.set(
          rootProject.layout.projectDirectory.file(
            "output/appmetrica-mapping/mapping-${variant.name}-$release.txt"
          )
        )
      }
    }

    // Preloaded-content variants: download the flavor's seed bundles at build time and add them as
    // generated assets (assets/seed-books/). Variants whose flavor isn't in seedBooksByFlavor
    // register no task and stay clean/universal.
    val seedBooks = seedBooksByFlavor[variant.flavorName].orEmpty()
    if (seedBooks.isNotEmpty()) {
      // Bundle files/keys are per build type: release uses release-signed bundles, everything else
      // (debug, localSeed) uses the debug variant — matching BUNDLE_VARIANT and the runtime key.
      val bundleVariant = if (variant.buildType == "release") "release" else "debug"
      val seedTask = tasks.register<DownloadSeedBundlesTask>(
        "generateSeedBundles${variant.name.replaceFirstChar { it.uppercase() }}"
      ) {
        bookIds.set(seedBooks)
        this.bundleVariant.set(bundleVariant)
        catalogUrls.set(catalogUrlsFor(bundleVariant))
      }
      // AGP owns the task's output location and merges it into this variant's assets.
      variant.sources.assets?.addGeneratedSourceDirectory(seedTask) { it.outputDir }
    }
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

  // RuStore Pay SDK — only the `rustore` flavor pulls this in; the Google Play
  // flavors (ru/uk/full) stay free of any payment SDK. Flavor-scoped config names
  // are created by AGP and referenced here as strings.
  "rustoreImplementation"(platform(libs.rustore.sdk.bom))
  "rustoreImplementation"(libs.rustore.sdk.pay)

  // Monitoring — crashes/ANR/non-fatals/analytics for every flavor (no Google Play Services needed)
  implementation(libs.appmetrica.analytics)

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



