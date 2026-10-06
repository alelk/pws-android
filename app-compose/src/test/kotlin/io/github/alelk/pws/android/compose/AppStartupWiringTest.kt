@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.alelk.pws.android.compose

import io.github.alelk.pws.domain.booklibrary.model.BookInstallSource
import io.github.alelk.pws.domain.booklibrary.model.InstalledBook
import io.github.alelk.pws.domain.booklibrary.repository.InstalledBookObserveRepository
import io.github.alelk.pws.domain.booklibrary.usecase.ObserveInstalledBooksUseCase
import io.github.alelk.pws.domain.core.Version
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.telemetry.NoOpTelemetry
import io.github.alelk.pws.features.app.startup.AppStartupModel
import io.github.alelk.pws.features.app.startup.StartupGate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/**
 * Plan 2026-09-30, step 05.2: pws-core's [AppStartupModel] driven by the real [AndroidAppStartupTasks]
 * (the binding the shell registers in Koin). The step's mine (README §6 p. 2): until the startup legacy
 * migration has finished the app shows only the loading surface — no onboarding (which installs books),
 * no seeding, no migration retry / backup restore — even though the new database is still empty.
 *
 * Red check: drop `migrationGate.afterMigration` / `migrationGate.await()` in [AndroidAppStartupTasks], or
 * start [AppStartupModel] from `preloaded = false`, and run `./gradlew :app-compose:testRuDebugUnitTest`,
 * then revert.
 */
class AppStartupWiringTest :
  FunSpec({

    class Fixture(scope: TestScope, books: List<InstalledBook>) {
      val migration = CompletableDeferred<Unit>()
      val order = mutableListOf<String>()
      val books = MutableStateFlow(books)
      var seedResult = false

      private val tasks = AndroidAppStartupTasks(
        migrationGate = LegacyMigrationGate(migration),
        telemetry = NoOpTelemetry,
        operations = AndroidAppStartupTasks.StartupOperations(
          seedBooksFromAssets = {
            order += "seed"
            seedResult
          },
          hasPendingLegacyMigration = { true },
          retryLegacyMigration = {
            order += "retry"
            emptyList()
          },
          applyPendingRestore = { order += "restore" },
        ),
        ioDispatcher = StandardTestDispatcher(scope.testScheduler),
      )

      private val repository = object : InstalledBookObserveRepository {
        override fun observeAll() = this@Fixture.books
      }

      val model = AppStartupModel(tasks, ObserveInstalledBooksUseCase(repository), NoOpTelemetry, scope.backgroundScope)

      fun finishMigration() {
        order += "migration"
        migration.complete(Unit)
      }
    }

    test("mine: a clean install stays on the loading surface until the legacy migration finished") {
      runTest {
        val f = Fixture(this, books = emptyList())
        runCurrent()
        f.model.state.value.gate shouldBe StartupGate.Loading
        f.order.shouldBeEmpty()

        f.finishMigration()
        runCurrent()
        f.order shouldContainExactly listOf("migration", "seed")
        f.model.state.value.gate shouldBe StartupGate.Onboarding
      }
    }

    test("mine: an upgraded install with books waits for the migration before the retry / restore and the app") {
      runTest {
        val f = Fixture(this, books = listOf(book()))
        runCurrent()
        f.model.state.value.gate shouldBe StartupGate.Loading
        f.order.shouldBeEmpty()

        f.finishMigration()
        runCurrent()
        f.order.first() shouldBe "migration"
        f.order.drop(1).sorted() shouldContainExactly listOf("restore", "retry", "seed")
        f.order.indexOf("retry") shouldBe f.order.indexOf("restore") - 1
        f.model.state.value.gate shouldBe StartupGate.App
      }
    }

    test("a build with built-in books opens the app right after seeding, without onboarding") {
      runTest {
        val f = Fixture(this, books = emptyList()).apply { seedResult = true }
        runCurrent()
        f.finishMigration()
        runCurrent()
        f.model.state.value.gate shouldBe StartupGate.App
      }
    }
  })

private fun book() = InstalledBook(
  bookId = BookId.parse("book-1"),
  source = BookInstallSource.DOWNLOADED,
  installedAt = 0,
  bundleVersion = Version(1, 0),
)
