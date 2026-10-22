@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.alelk.pws.android.compose

import io.github.alelk.pws.database.LegacyMigrationOutcome
import io.github.alelk.pws.domain.telemetry.Telemetry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/**
 * Plan 2026-09-30, step 05 (mine): nothing that installs books or restores user data — seeding, the
 * legacy-migration retry, the pending backup restore — may start before the startup legacy migration
 * has finished (README §6 p. 2, plan 2026-09-29 C2).
 *
 * Red check: drop `migrationGate.afterMigration` / `migrationGate.await()` in [AndroidAppStartupTasks]
 * and run `./gradlew :app-compose:testRuDebugUnitTest --tests '*AndroidAppStartupTasksTest*'`, then revert.
 */
class AndroidAppStartupTasksTest :
  FunSpec({

    class Fixture(scope: TestScope, migrationDone: Boolean = false) {
      val migration = CompletableDeferred<Unit>().apply { if (migrationDone) complete(Unit) }
      val order = mutableListOf<String>()
      val events = mutableListOf<String>()
      val errors = mutableListOf<String?>()
      var pendingLegacy = true
      var retryFailure: Throwable? = null
      var seedResult = false

      private val telemetry = object : Telemetry {
        override fun recordError(throwable: Throwable, message: String?, attributes: Map<String, String>) {
          errors += message
        }
        override fun log(message: String) = Unit
        override fun event(name: String, params: Map<String, Any?>) {
          events += name
        }
        override fun setUserProperty(key: String, value: String?) = Unit
      }

      val tasks = AndroidAppStartupTasks(
        migrationGate = LegacyMigrationGate(migration),
        telemetry = telemetry,
        operations = AndroidAppStartupTasks.StartupOperations(
          seedBooksFromAssets = {
            order += "seed"
            seedResult
          },
          hasPendingLegacyMigration = { pendingLegacy },
          retryLegacyMigration = {
            order += "retry"
            retryFailure?.let { throw it }
            listOf(LegacyMigrationOutcome("pws.2.3.0.db", LegacyMigrationOutcome.Result.COMPLETE, null, 1, 1))
          },
          applyPendingRestore = { order += "restore" },
        ),
        ioDispatcher = StandardTestDispatcher(scope.testScheduler),
      )

      fun finishMigration() {
        order += "migration"
        migration.complete(Unit)
      }
    }

    test("seeding built-in books starts strictly after the legacy migration") {
      runTest {
        val f = Fixture(this).apply { seedResult = true }
        val preloaded = async { f.tasks.seedPreloadedBooks() }
        runCurrent()
        f.order.shouldBeEmpty()

        f.finishMigration()
        preloaded.await() shouldBe true
        f.order shouldContainExactly listOf("migration", "seed")
      }
    }

    test("a book install retries the migration and applies the pending restore strictly after the startup migration") {
      runTest {
        val f = Fixture(this)
        val job = launch { f.tasks.onBooksInstalled(1) }
        runCurrent()
        f.order.shouldBeEmpty()

        f.finishMigration()
        job.join()
        f.order shouldContainExactly listOf("migration", "retry", "restore")
        f.events shouldContainExactly listOf("legacy_migration")
      }
    }

    test("no installed books: neither the retry nor the restore runs") {
      runTest {
        val f = Fixture(this, migrationDone = true)
        f.tasks.onBooksInstalled(0)
        f.order.shouldBeEmpty()
      }
    }

    test("no pending legacy database: only the pending restore runs") {
      runTest {
        val f = Fixture(this, migrationDone = true).apply { pendingLegacy = false }
        f.tasks.onBooksInstalled(3)
        f.order shouldContainExactly listOf("restore")
        f.events.shouldBeEmpty()
      }
    }

    test("a failed retry is reported as a non-fatal and the pending restore still runs") {
      runTest {
        val f = Fixture(this, migrationDone = true).apply { retryFailure = IllegalStateException("boom") }
        f.tasks.onBooksInstalled(2)
        f.order shouldContainExactly listOf("retry", "restore")
        f.errors shouldContainExactly listOf("legacy_migration_retry_failed")
      }
    }
  })
