package io.github.alelk.pws.android.compose

import io.github.alelk.pws.database.LegacyMigrationOutcome
import io.github.alelk.pws.domain.telemetry.Telemetry
import io.github.alelk.pws.domain.telemetry.TelemetryAttr
import io.github.alelk.pws.domain.telemetry.TelemetryEvent
import kotlinx.coroutines.Deferred

/**
 * Startup barrier: opens once the legacy-database migration (and the flavor's startup tasks) have
 * finished — successfully or not; [PwsComposeApplication] completes it in a `finally`.
 *
 * Everything that installs books or restores user data — seeding built-in books, onboarding, the
 * pending backup restore — must [await] it first. Otherwise a seeded book makes the target database
 * non-empty, the migration skips installing the old books, and user data of every other book
 * cannot be matched (plan 2026-09-29, C2).
 */
class LegacyMigrationGate(private val done: Deferred<Unit>) {
  val isCompleted: Boolean get() = done.isCompleted

  suspend fun await() = done.await()
}

/** Runs [block] strictly after the legacy migration has finished. */
suspend fun <T> LegacyMigrationGate.afterMigration(block: suspend () -> T): T {
  await()
  return block()
}

/** One `legacy_migration` event per legacy file: counts, attempt, duration — never content. */
internal fun Telemetry.reportLegacyMigration(outcomes: List<LegacyMigrationOutcome>) {
  outcomes.forEach { outcome ->
    event(
      TelemetryEvent.LEGACY_MIGRATION,
      mapOf(
        TelemetryAttr.SOURCE to outcome.sourceName,
        TelemetryAttr.RESULT to outcome.result.id,
        TelemetryAttr.ITEMS_FOUND to outcome.report?.found,
        TelemetryAttr.ITEMS_MIGRATED to outcome.report?.migrated,
        TelemetryAttr.ATTEMPT to outcome.attempt,
        TelemetryAttr.DURATION_MS to outcome.durationMs,
      ),
    )
    when (outcome.result) {
      LegacyMigrationOutcome.Result.PARTIAL_GAVE_UP, LegacyMigrationOutcome.Result.FAILED_GAVE_UP ->
        recordError(
          IllegalStateException("legacy migration gave up after ${outcome.attempt} attempts"),
          "legacy_migration_gave_up",
          mapOf(TelemetryAttr.SOURCE to outcome.sourceName, TelemetryAttr.RESULT to outcome.result.id),
        )

      else -> Unit
    }
  }
}
