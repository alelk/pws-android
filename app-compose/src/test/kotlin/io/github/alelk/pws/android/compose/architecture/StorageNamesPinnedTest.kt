package io.github.alelk.pws.android.compose.architecture

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Rule (G3): the names of persistent stores are pinned. Each name below occurs as a string literal in
 * production code in exactly the expected file and nowhere else.
 * Why: renaming or recreating a store silently drops user data (favorites, history, settings, the paid
 * status of RuStore users). Changing a name is a decision of the owner, not a refactoring: update this
 * list in the same change, with the reason.
 *
 * See it red: change `"pws_donation"` in `di/DonationModule.kt` to `"pws_donation2"` (or copy the
 * literal `"pws.db"` into another main file) and run `./gradlew :app-compose:testRuDebugUnitTest`,
 * then revert.
 */
class StorageNamesPinnedTest :
  FunSpec({
    val production =
      scanFiles("app-compose/src", "data").filter { file ->
        val segments = file.relativePath.split('/')
        val setName = segments.getOrNull(segments.indexOf("src") + 1).orEmpty()
        !setName.startsWith("test") && !setName.startsWith("androidTest") && !setName.startsWith("commonTest")
      }

    PINNED_STORAGE_NAMES.forEach { (name, expectedFile) ->
      test("store name \"$name\" lives only in ${expectedFile.substringAfterLast('/')}") {
        val actual = production.violations { "\"$name\"" in it.code }
        withClue("Literal \"$name\" found in:\n${actual.joinToString("\n")}") { actual shouldBe listOf(expectedFile) }
      }
    }
  })

/** Store name -> the only production file that holds it. */
private val PINNED_STORAGE_NAMES: Map<String, String> =
  mapOf(
    "app-settings" to "app-compose/src/main/kotlin/io/github/alelk/pws/android/compose/ThemePreferences.kt",
    "pws-app-preferences" to
      "app-compose/src/rustore/kotlin/io/github/alelk/pws/android/compose/payment/LegacyRuStoreEntitlementStore.kt",
    "pws_donation" to "app-compose/src/main/kotlin/io/github/alelk/pws/android/compose/di/DonationModule.kt",
    "pws_catalog_source" to
      "data/content-delivery/src/main/kotlin/io/github/alelk/pws/contentdelivery/di/ContentDeliveryModule.kt",
    "pws.db" to "data/db-android/src/main/kotlin/io/github/alelk/pws/database/PwsDatabaseProvider.kt",
  )
