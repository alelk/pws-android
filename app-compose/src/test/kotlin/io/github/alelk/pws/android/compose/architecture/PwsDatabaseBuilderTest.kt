package io.github.alelk.pws.android.compose.architecture

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Rule (search plan R8): every `Room.databaseBuilder` / `Room.inMemoryDatabaseBuilder` for `PwsDatabase` — production
 * (`PwsDatabaseProvider`) and tests alike — goes through `.withPwsSchema()` before `.build()`.
 * Why: `songs_fts` is not a Room entity; the view, FTS table and sync triggers are created by the callback and
 * migrations that `withPwsSchema()` registers. A database built without it opens fine but has no search index and
 * raises no error — search just finds nothing. (pws-core has the twin test for its own sources.)
 *
 * See it red: drop `.withPwsSchema()` from `PwsDatabaseProvider.buildDatabase` and run
 * `./gradlew :app-compose:testRuDebugUnitTest --tests '*PwsDatabaseBuilderTest*'`, then revert.
 */
class PwsDatabaseBuilderTest :
  FunSpec({
    val sources = scanFiles("app-compose/src", "data")

    test("the scan sees the builders (not vacuous)") {
      (sources.sumOf { pwsBuilders(it.code).size } >= 5) shouldBe true
    }

    test("the rule detects a builder without withPwsSchema") {
      pwsBuilders("val db = Room.inMemoryDatabaseBuilder(c, PwsDatabase::class.java).build()").single() shouldBe false
      pwsBuilders("val db = Room\n  .databaseBuilder(c, PwsDatabase::class.java, n)\n  .withPwsSchema()\n  .build()").single() shouldBe true
    }

    test("every PwsDatabase builder uses withPwsSchema()") {
      // this file holds deliberately wrong sample builders in string literals
      val actual = sources.violations { f -> !f.relativePath.endsWith("/PwsDatabaseBuilderTest.kt") && pwsBuilders(f.code).any { !it } }
      withClue("Builders without withPwsSchema():\n${actual.joinToString("\n")}") { actual shouldBe emptyList() }
    }
  })

private val BUILDER = Regex("""\bRoom\s*\.\s*(?:inMemoryDatabaseBuilder|databaseBuilder)\b([\s\S]*?)\.\s*build\s*\(\)""")
private val WITH_SCHEMA = Regex("""\.\s*withPwsSchema\s*\(\)""")

/** One flag per `PwsDatabase` builder in [code]: `true` when it calls `withPwsSchema()`. */
private fun pwsBuilders(code: String): List<Boolean> = BUILDER.findAll(code)
  .map { it.groupValues[1] }
  .filter { "PwsDatabase" in it }
  .map { WITH_SCHEMA.containsMatchIn(it) }
  .toList()
