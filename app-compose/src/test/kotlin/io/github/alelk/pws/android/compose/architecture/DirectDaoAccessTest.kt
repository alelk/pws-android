package io.github.alelk.pws.android.compose.architecture

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Rule: Room DAOs (`import io.github.alelk.pws.database.*.*Dao` or a `xxxDao()` call) are used only
 * inside `:data:db-android`; the shell (`app-compose/src/main`) and `:data:content-delivery` reach data
 * through repositories / ports.
 * Why: a DAO call from the shell bypasses the repository layer (mapping, transactions, invariants) and
 * ties Android glue to the Room schema (G1: version 15 stays). The ratchet below was emptied in step 06.3.
 *
 * See it red: add `val d = db.songDao()` to any file in `app-compose/src/main` or
 * `data/content-delivery/src/main` and run `./gradlew :app-compose:testRuDebugUnitTest`, then revert.
 * Fixing a KNOWN file without removing it from the list is red too.
 */
class DirectDaoAccessTest :
  FunSpec({
    val sources = scanFiles("app-compose/src/main", "data/content-delivery/src/main")

    test("DAOs are not used outside :data:db-android") {
      val actual = sources.violations { DAO_USE.containsMatchIn(it.code) }
      withClue("Actual violations:\n${actual.joinToString("\n")}") { actual shouldBe KNOWN_DIRECT_DAO_USERS }
    }
  })

private val DAO_USE =
  Regex("""^import\s+io\.github\.alelk\.pws\.database\.[\w.]*Dao\b|\b\w+Dao\(\)""", RegexOption.MULTILINE)

/** Only shrinks (G10). Files outside `:data:db-android` that use DAOs directly. */
private val KNOWN_DIRECT_DAO_USERS: List<String> = emptyList()
