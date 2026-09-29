package io.github.alelk.pws.android.compose

import io.github.alelk.pws.database.LegacyMigrationOutcome
import io.github.alelk.pws.database.MigrationCount
import io.github.alelk.pws.database.MigrationReport
import io.github.alelk.pws.domain.telemetry.Telemetry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/**
 * T-A02: nothing that installs books (seeding, pending restore) may run before the legacy-database
 * migration has finished — otherwise the migration sees a non-empty database and user data of
 * every other book is not matched (C2).
 */
class LegacyMigrationGateTest : FunSpec({

  test("seeding runs strictly after the migration") {
    runTest {
      val migrationDone = CompletableDeferred<Unit>()
      val gate = LegacyMigrationGate(migrationDone)
      val order = mutableListOf<String>()

      val seeding = launch { gate.afterMigration { order += "seed" } }
      runCurrent()
      order.shouldBeEmpty()
      gate.isCompleted shouldBe false

      order += "migration"
      migrationDone.complete(Unit)
      seeding.join()

      order shouldContainExactly listOf("migration", "seed")
      gate.isCompleted shouldBe true
    }
  }

  test("an already open gate does not delay anything") {
    runTest {
      val gate = LegacyMigrationGate(CompletableDeferred(Unit))
      gate.afterMigration { 42 } shouldBe 42
    }
  }

  test("legacy_migration telemetry carries counts only, plus a non-fatal when the migration gave up") {
    val events = mutableListOf<Pair<String, Map<String, Any?>>>()
    val errors = mutableListOf<String?>()
    val telemetry = object : Telemetry {
      override fun recordError(throwable: Throwable, message: String?, attributes: Map<String, String>) {
        errors += message
      }
      override fun log(message: String) = Unit
      override fun event(name: String, params: Map<String, Any?>) {
        events += name to params
      }
      override fun setUserProperty(key: String, value: String?) = Unit
    }
    val report = MigrationReport(favorites = MigrationCount(4, 2), history = MigrationCount(5, 5))
    telemetry.reportLegacyMigration(
      listOf(
        LegacyMigrationOutcome("pws.2.3.0.db", LegacyMigrationOutcome.Result.PARTIAL_RETRY, report, attempt = 1, durationMs = 120),
        LegacyMigrationOutcome("pws.2.0.0.db", LegacyMigrationOutcome.Result.FAILED_GAVE_UP, null, attempt = 5, durationMs = 7),
      ),
    )
    events shouldContainExactly listOf(
      "legacy_migration" to mapOf(
        "source" to "pws.2.3.0.db", "result" to "partial_retry", "items_found" to 9, "items_migrated" to 7,
        "attempt" to 1, "duration_ms" to 120L,
      ),
      "legacy_migration" to mapOf(
        "source" to "pws.2.0.0.db", "result" to "failed_gave_up", "items_found" to null, "items_migrated" to null,
        "attempt" to 5, "duration_ms" to 7L,
      ),
    )
    errors shouldContainExactly listOf("legacy_migration_gave_up")
  }
})
