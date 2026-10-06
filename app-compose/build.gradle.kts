import com.android.build.api.artifact.SingleArtifact
import io.github.alelk.pws.build.DownloadSeedBundlesTask
import io.github.alelk.pws.build.RustoreInvariants
import io.github.alelk.pws.build.StageMappingFileTask
import io.github.alelk.pws.build.VerifyRustoreInvariantsTask
import java.util.Properties

plugins {
  id("pws.android.application")
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
val catalogGhPages = "https://alelk.github.io/pws-catalog/v$catalogMajor"
val catalogCloudflare = "https://pws-catalog.pages.dev/v$catalogMajor"
val catalogYandex = "https://pws-catalog.storage.yandexcloud.net/v$catalogMajor"

// Catalog mirrors for a bundle variant (release|debug), in fallback order.
fun catalogUrlsFor(variant: String): List<String> =
  listOf(catalogGhPages, catalogCloudflare, catalogYandex).map { "$it/books-catalog-$variant.json" }

fun catalogUrl(variant: String) = catalogUrlsFor(variant).joinToString(",")

// ── AppMetrica API key ────────────────────────────────────────────────────────
// The SDK's write key. Never committed. Sources, in priority order (same pattern as the DB decrypt
// key in :data:db-android):
//   1. Environment variable APPMETRICA_API_KEY        (CI / GitHub Actions secret)
//   2. local.properties → appmetrica.apiKey           (local dev only, not in VCS)
//   3. Gradle property appmetrica.apiKey              (-P… or ~/.gradle/gradle.properties)
// NB: Gradle does NOT expose local.properties as project properties — findProperty() alone silently
// returns null for it, which is exactly how this key ended up empty in a build that had it set.
val appMetricaLocalProps = Properties().also { props ->
  rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { props.load(it) }
}
val appMetricaApiKey: String =
  System.getenv("APPMETRICA_API_KEY")
    ?: appMetricaLocalProps.getProperty("appmetrica.apiKey")
    ?: project.findProperty("appmetrica.apiKey") as String?
    ?: ""

// Loud at configuration time: a silently keyless build looks identical to a working one until you
// stare at an empty AppMetrica console.
if (appMetricaApiKey.isBlank()) {
  logger.lifecycle(
    "AppMetrica: no API key (env APPMETRICA_API_KEY / local.properties appmetrica.apiKey) — " +
      "this build ships WITHOUT telemetry (NoOpTelemetry). See docs/monitoring.md."
  )
}

// ── RuStore purchases ─────────────────────────────────────────────────────────
// Off by default: RuStore monetisation is not enabled on the account yet, so the rustore build ships
// as MonetizationMode.PremiumComingSoon — paid users keep Pro, everyone else sees "Pro — coming soon",
// and the RuStore Pay SDK is never called. Turning sales on = building with
// -Ppws.rustore.purchasesEnabled=true (CI input `rustore_purchases_enabled`). See
// docs/ai/plans/2026-09-29_rustore-release-compat-pro-coming-soon_plan.md.
val rustorePurchasesEnabled: Boolean =
  (project.findProperty("pws.rustore.purchasesEnabled") as String?)?.toBoolean() ?: false
logger.lifecycle(
  "RuStore purchases: ${if (rustorePurchasesEnabled) "ENABLED (PremiumSales)" else "disabled (PremiumComingSoon)"}" +
    " — Gradle property pws.rustore.purchasesEnabled"
)

// Books preloaded straight into the APK for specific flavors. For each listed flavor the
// `generateSeedBundles<Variant>` task downloads the named bundles from the catalog at build time and
// bakes them into `assets/seed-books/`, so the app ships with that content already installed as a
// non-removable built-in (source=ASSET, imported on first launch by SeedBooksFromAssetsUseCase).
// Flavors absent from this map produce the universal "clean" APK, unchanged.
//   - key   = product flavor name (contentLevel dimension): ru | uk | full | rustore
//   - value = book IDs that must exist in books-catalog-{release|debug}.json
val seedBooksByFlavor: Map<String, List<String>> =
  mapOf(
    "rustore" to listOf("PV3300"),
    "uk" to listOf(
      "Psalmovivi",
      "PisniSpasennyh",
      "EvangelskiPisni",
      "PV3300",
      "PV3055",
      "PV2555",
      "PV2001",
      "PV800",
      "YunostIisusu",
      "DerjisHrista",
      "PesnHvaly"
    ),
  )

android {
  namespace = "io.github.alelk.pws.android.compose"

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
    versionCode = rootProject.extra["versionCode"] as Int
    versionName = "${rootProject.extra["versionName"]}-${rootProject.extra["versionNameSuffix"]}"
    resValue("string", "db_authority", "com.alelk.pws.database")
    buildConfigField("String", "APPMETRICA_API_KEY", "\"$appMetricaApiKey\"")
    // Store purchases (paywall + payment SDK). Only the rustore flavor can turn this on.
    buildConfigField("boolean", "PURCHASES_ENABLED", "false")
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
      applicationId = RustoreInvariants.APPLICATION_ID
      versionNameSuffix = "-rustore"
      resValue("string", "db_authority", RustoreInvariants.DB_AUTHORITY)
      buildConfigField("boolean", "PURCHASES_ENABLED", "$rustorePurchasesEnabled")
      // Платёжный SDK RuStore живёт только в этом флейворе — и его keep-правила тоже.
      // Флейворы ru/uk/full не должны платить размером за чужие правила.
      proguardFiles("proguard-rules-rustore.pro")
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
    getByName("localSeed") {
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

  androidResources {
    // Библиотечные строки (AndroidX и пр.) переводятся на ~70 языков — в APK нужны только наши.
    // Список синхронизирован с pws-core/features/src/commonMain/composeResources/
    // (values = en, values-pl, values-ru, values-uk).
    localeFilters += listOf("en", "pl", "ru", "uk")
  }

}

// rustoreDebug is signed with the RuStore release key when it is configured (as the fork did), so a
// debuggable build installs over the published fork 2.3.1 and `run-as io.github.alelk.pws.app` can
// inspect its files (upgrade tests, tools/rustore-upgrade-test.md). Without the key: debug key.
val rustoreSigningConfigured = listOf(
  "android.release.keystorePathRustore", "android.release.keyAliasRuRustore",
  "android.release.keyPasswordRustore", "android.release.storePasswordRustore",
).all { !(project.findProperty(it) as String?).isNullOrBlank() }

androidComponents {
  onVariants { variant ->
    if (variant.name == "rustoreDebug" && rustoreSigningConfigured) {
      variant.signingConfig.setConfig(android.signingConfigs.getByName("release-rustore"))
    }

    if (variant.name == "rustoreRelease") {
      val guard = tasks.register<VerifyRustoreInvariantsTask>("verifyRustoreReleaseInvariants") {
        applicationId.set(variant.applicationId)
        versionCode.set(variant.outputs.single().versionCode.map { it })
        minSdk.set(variant.minSdk.apiLevel)
        dbAuthority.set(
          variant.resValues.getting(variant.makeResValueKey("string", "db_authority")).map { it.value }
        )
      }
      tasks.matching { it.name == "preRustoreReleaseBuild" }.configureEach { dependsOn(guard) }
    }

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




