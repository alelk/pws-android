package io.github.alelk.pws.contentdelivery

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.alelk.pws.contentdelivery.install.BookImporterImpl
import io.github.alelk.pws.data.repository.room.booklibrary.BookContentWriterImpl
import io.github.alelk.pws.data.repository.room.installed_book.InstalledBookRepositoryImpl
import io.github.alelk.pws.data.repository.room.transaction.RoomTransactionRunner
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.portable.booklibrary.ImportBookBundleUseCase

/** Creates a fresh in-memory [PwsDatabase] for a single test run. */
fun inMemoryPwsDb(): PwsDatabase {
  val context = ApplicationProvider.getApplicationContext<Context>()
  return Room.inMemoryDatabaseBuilder(context, PwsDatabase::class.java)
    .allowMainThreadQueries()
    .build()
}

/** The production [io.github.alelk.pws.domain.booklibrary.repository.BookContentWriter] over [db], as Koin wires it. */
fun bookContentWriter(db: PwsDatabase) = BookContentWriterImpl(db, RoomTransactionRunner(db))

/** The production [BookImporterImpl] over [db], as Koin wires it. */
fun bookImporter(db: PwsDatabase) = BookImporterImpl(ImportBookBundleUseCase(bookContentWriter(db)))

/** The production [io.github.alelk.pws.domain.booklibrary.repository.InstalledBookRepository] over [db]. */
fun installedBookRepository(db: PwsDatabase) = InstalledBookRepositoryImpl(db.installedBookDao())
