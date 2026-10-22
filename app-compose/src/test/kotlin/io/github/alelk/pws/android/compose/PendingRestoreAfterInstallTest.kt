package io.github.alelk.pws.android.compose

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.colman.kotest.FeatureSpec
import br.com.colman.kotest.android.extensions.robolectric.RobolectricTest
import io.github.alelk.pws.contentdelivery.install.BookImporterImpl
import io.github.alelk.pws.data.repository.room.book.BookObserveRepositoryImpl
import io.github.alelk.pws.data.repository.room.booklibrary.BookContentWriterImpl
import io.github.alelk.pws.data.repository.room.installed_book.InstalledBookRepositoryImpl
import io.github.alelk.pws.data.repository.room.transaction.RoomTransactionRunner
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.booklibrary.BookLibraryScenario
import io.github.alelk.pws.database.booklibrary.BookLibraryScenario.dumpContentTables
import io.github.alelk.pws.domain.core.Color
import io.github.alelk.pws.domain.core.Locale
import io.github.alelk.pws.domain.core.Version
import io.github.alelk.pws.portable.BackupService
import io.github.alelk.pws.portable.booklibrary.ImportBookBundleUseCase
import io.github.alelk.pws.portable.model.Backup
import io.github.alelk.pws.portable.model.BookPreference
import io.github.alelk.pws.portable.model.Song
import io.github.alelk.pws.portable.model.SongNumber
import io.github.alelk.pws.portable.model.Tag
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import org.koin.core.context.stopKoin
import java.io.File
import java.util.UUID

/**
 * Characterization test (plan 2026-09-30, step 06.3) of the deferred Auto Backup restore: the records of a pending
 * backup are applied book by book as the books get installed, and the pending file is kept until every book of the
 * backup is installed. The snapshots were recorded from the pre-refactoring importer and agent.
 */
@RobolectricTest(sdk = [34])
class PendingRestoreAfterInstallTest :
  FeatureSpec({

    afterTest { runCatching { stopKoin() } }

    feature("pending Auto Backup restore") {
      scenario("records are restored as their books get installed; the file goes once all books are installed") {
        runTest {
          val ctx = ApplicationProvider.getApplicationContext<Context>()
          val db = Room.inMemoryDatabaseBuilder(ctx, PwsDatabase::class.java).allowMainThreadQueries().build()
          val dsFile = File(ctx.filesDir, "ds-${UUID.randomUUID()}.preferences_pb")
          val backupManager = BackupManager(db, PreferenceDataStoreFactory.create(scope = backgroundScope) { dsFile })
          val importer = BookImporterImpl(ImportBookBundleUseCase(BookContentWriterImpl(db, RoomTransactionRunner(db))))
          val books = BookObserveRepositoryImpl(db.bookDao())
          val installedBooks = InstalledBookRepositoryImpl(db.installedBookDao())
          val pending = PwsBackupAgent.pendingRestoreFile(ctx)
          pending.writeText(BackupService().writeAsString(pendingBackup()), Charsets.UTF_8)

          val steps = mutableListOf<String>()
          suspend fun applyAndSnapshot(title: String) {
            PwsBackupAgent.applyPendingRestoreIfNeeded(ctx, books, installedBooks, backupManager)
            steps += "#### $title -> pending file kept: ${pending.exists()}\n${db.dumpContentTables()}"
          }

          applyAndSnapshot("no books installed")
          importer.import(BookLibraryScenario.book1v1())
          applyAndSnapshot("Book-1 installed")
          importer.import(BookLibraryScenario.book2v1())
          applyAndSnapshot("Book-2 installed")

          steps.joinToString("\n") shouldBe PENDING_RESTORE_GOLDEN
          db.close()
        }
      }
    }
  })

private fun pendingBackup(): Backup {
  val b1 = BookLibraryScenario.book1
  val b2 = BookLibraryScenario.book2
  return Backup(
    metadata = Backup.Metadata(createdAt = LocalDateTime(2026, 1, 1, 0, 0), source = "test"),
    songs = listOf(
      Song(number = SongNumber(b1, 2), version = Version(1, 0), locale = Locale.RU, name = "Mine", lyric = "my edit"),
    ),
    favorites = listOf(SongNumber(b1, 1), SongNumber(b2, 1), SongNumber(b1, 3)),
    tags = listOf(Tag(name = "Mine", color = Color(1, 2, 3), songs = setOf(SongNumber(b1, 1), SongNumber(b2, 7)))),
    bookPreferences = listOf(BookPreference(b1, 9), BookPreference(b2, 4)),
    history = listOf(
      Backup.HistoryEntry(SongNumber(b2, 7), LocalDateTime(2026, 1, 2, 10, 0)),
      Backup.HistoryEntry(SongNumber(b1, 5), LocalDateTime(2026, 1, 2, 11, 0)),
    ),
  )
}

private val PENDING_RESTORE_GOLDEN: String =
  """
  #### no books installed -> pending file kept: true
  == books
  == book_statistic
  == installed_books
  == songs
  == song_numbers
  == song_references
  == tags
  == song_tags
  == favorites
  == history
  #### Book-1 installed -> pending file kept: true
  == books
  Book-1 | 1.0 | [ru];[uk] | Book One | Book-1 | Book One | 2020 | Author A | null | null | Editor E | About Book One | null
  == book_statistic
  Book-1 | 9 | null | null
  == installed_books
  Book-1 | 1.0 | DOWNLOADED
  == songs
  1 | 1.0 | ru | Song 1 | one v1 | Author | null | null | a major | null | null | 0
  2 | 1.0 | ru | Mine | my edit | null | null | null | null | null | null | 1
  3 | 1.0 | ru | Song 3 | three v1 | null | null | null | null | 1990 | Ps 1:1 | 0
  4 | 1.0 | ru | Song 4 | four v1 | null | null | null | null | null | null | 0
  5 | 1.0 | ru | Song 5 | five v1 | null | null | null | null | null | null | 0
  == song_numbers
  Book-1 | 1 | 1 | 0
  Book-1 | 2 | 2 | 0
  Book-1 | 3 | 3 | 0
  Book-1 | 4 | 4 | 0
  Book-1 | 5 | 5 | 0
  == song_references
  1 | 2 | variation | 50 | 0
  == tags
  custom-00001 | Mine | 0 | #010203 | 0
  prayer | Prayer | 1 | #010203 | 1
  == song_tags
  1 | custom-00001 | 0
  1 | prayer | 0
  3 | prayer | 0
  == favorites
  1 | Book-1 | 1
  3 | Book-1 | 2
  == history
  5 | Book-1 | 1 | 2026-01-02 11:00:00
  #### Book-2 installed -> pending file kept: false
  == books
  Book-1 | 1.0 | [ru];[uk] | Book One | Book-1 | Book One | 2020 | Author A | null | null | Editor E | About Book One | null
  Book-2 | 1.0 | [ru];[uk] | Book Two | Book-2 | Book Two | 2020 | Author A | null | null | Editor E | About Book Two | null
  == book_statistic
  Book-1 | 9 | null | null
  Book-2 | 4 | null | null
  == installed_books
  Book-1 | 1.0 | DOWNLOADED
  Book-2 | 1.0 | DOWNLOADED
  == songs
  1 | 1.0 | ru | Song 1 | one v1 | Author | null | null | a major | null | null | 0
  2 | 1.0 | ru | Mine | my edit | null | null | null | null | null | null | 1
  3 | 1.0 | ru | Song 3 | three v1 | null | null | null | null | 1990 | Ps 1:1 | 0
  4 | 1.0 | ru | Song 4 | four v1 | null | null | null | null | null | null | 0
  5 | 1.1 | ru | Song 5 | five v1.1 | null | null | null | null | null | null | 0
  20 | 1.0 | ru | Song 20 | twenty v1 | null | null | null | null | null | null | 0
  22 | 1.0 | ru | Song 22 | twenty-two v1 | null | null | null | null | null | null | 0
  == song_numbers
  Book-1 | 1 | 1 | 0
  Book-1 | 2 | 2 | 0
  Book-1 | 3 | 3 | 0
  Book-1 | 4 | 4 | 0
  Book-1 | 5 | 5 | 0
  Book-2 | 20 | 1 | 0
  Book-2 | 4 | 3 | 0
  Book-2 | 22 | 4 | 1
  Book-2 | 5 | 7 | 0
  == song_references
  1 | 2 | variation | 50 | 0
  3 | 20 | variation | 40 | 0
  == tags
  custom-00001 | Mine | 0 | #010203 | 0
  hymn | Hymn | 2 | #040506 | 1
  prayer | Prayer | 1 | #010203 | 1
  == song_tags
  1 | custom-00001 | 0
  1 | prayer | 0
  3 | prayer | 0
  5 | custom-00001 | 0
  5 | hymn | 0
  20 | hymn | 0
  == favorites
  1 | Book-1 | 1
  3 | Book-1 | 2
  20 | Book-2 | 3
  == history
  5 | Book-1 | 1 | 2026-01-02 11:00:00
  5 | Book-2 | 2 | 2026-01-02 10:00:00
  """.trimIndent()
