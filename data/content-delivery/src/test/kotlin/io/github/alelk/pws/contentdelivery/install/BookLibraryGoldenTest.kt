package io.github.alelk.pws.contentdelivery.install

import br.com.colman.kotest.FeatureSpec
import br.com.colman.kotest.android.extensions.robolectric.RobolectricTest
import io.github.alelk.pws.contentdelivery.bookContentWriter
import io.github.alelk.pws.contentdelivery.bookImporter
import io.github.alelk.pws.contentdelivery.inMemoryPwsDb
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.booklibrary.BookLibraryGolden
import io.github.alelk.pws.database.booklibrary.BookLibraryScenario
import io.github.alelk.pws.database.booklibrary.BookLibraryScenario.dumpContentTables
import io.github.alelk.pws.database.booklibrary.BookLibraryScenario.failDeletesFrom
import io.github.alelk.pws.database.booklibrary.BookLibraryScenario.failInsertsInto
import io.github.alelk.pws.database.booklibrary.BookLibraryScenario.prepareUpdate
import io.github.alelk.pws.database.booklibrary.BookLibraryScenario.runScenario
import io.github.alelk.pws.domain.booklibrary.model.BookInstallSource
import io.github.alelk.pws.domain.core.error.DeleteError
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.portable.model.BookBundle
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith

/**
 * Golden (characterization) test of installing, updating and uninstalling books (plan 2026-09-30, step 06.3): the
 * shared [BookLibraryScenario] runs through the app's importer and uninstall use case, and the database after each
 * step must equal [BookLibraryGolden], recorded from the pre-refactoring implementation. `:portable-data` checks the
 * use cases in pws-core against the same snapshots.
 */
@RobolectricTest(sdk = [34])
class BookLibraryGoldenTest :
  FeatureSpec({

    fun library(db: PwsDatabase) = object : BookLibraryScenario.Library {
      val importer = bookImporter(db)
      val uninstall = UninstallBookUseCaseImpl(bookContentWriter(db))

      override suspend fun import(bundle: BookBundle, source: BookInstallSource) = importer.import(bundle, source)

      override suspend fun uninstall(bookId: BookId): String = uninstall.invoke(bookId).fold(
        ifLeft = { e ->
          when (e) {
            DeleteError.NotFound -> "not found"
            is DeleteError.ValidationError -> "refused: ${e.message}"
            is DeleteError.UnknownError -> "error: ${e.message}"
          }
        },
        ifRight = { "ok" },
      )
    }

    suspend fun <T> withDb(block: suspend (PwsDatabase) -> T): T {
      val db = inMemoryPwsDb()
      return try {
        block(db)
      } finally {
        db.close()
      }
    }

    feature("book library golden snapshots") {
      scenario("install, update and uninstall write the golden tables") {
        withDb { db -> db.runScenario(library(db)) shouldBe BookLibraryGolden.STEPS }
      }
    }

    feature("a failure in the middle writes nothing") {
      scenario("update failing on the installed-book record leaves the database unchanged") {
        withDb { db ->
          val library = library(db)
          db.prepareUpdate(library)
          val before = db.dumpContentTables()
          db.failInsertsInto("installed_books")

          shouldThrowAny { library.import(BookLibraryScenario.book1v2()) }

          db.dumpContentTables() shouldBe before
        }
      }

      scenario("update with a reference of an unknown reason leaves the database unchanged") {
        withDb { db ->
          val library = library(db)
          db.prepareUpdate(library)
          val before = db.dumpContentTables()

          shouldThrowAny { library.import(BookLibraryScenario.book1v2WithUnknownReferenceReason()) }

          db.dumpContentTables() shouldBe before
        }
      }

      scenario("uninstall failing on the book row leaves the database unchanged") {
        withDb { db ->
          val library = library(db)
          db.prepareUpdate(library)
          val before = db.dumpContentTables()
          db.failDeletesFrom("books")

          library.uninstall(BookLibraryScenario.book1) shouldStartWith "error: "

          db.dumpContentTables() shouldBe before
        }
      }
    }
  })
