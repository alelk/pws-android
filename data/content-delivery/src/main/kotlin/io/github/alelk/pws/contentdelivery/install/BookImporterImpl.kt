package io.github.alelk.pws.contentdelivery.install

import arrow.core.getOrElse
import io.github.alelk.pws.domain.booklibrary.model.BookInstallSource
import io.github.alelk.pws.domain.core.error.UpsertError
import io.github.alelk.pws.portable.booklibrary.ImportBookBundleUseCase
import io.github.alelk.pws.portable.model.BookBundle
import timber.log.Timber

/**
 * Imports a decoded bundle through [ImportBookBundleUseCase] (pws-core): the database writes — songs, smart
 * song-number binding, references, tags, orphan clean-up — run there in one transaction (step 06.3 of plan
 * 2026-09-30). This adapter keeps the importer's contract for its callers: a failure is thrown, nothing is written.
 */
class BookImporterImpl(private val importBookBundle: ImportBookBundleUseCase) {

  /**
   * Imports [bundle] into the database.
   *
   * @param source how this book reached the device. Defaults to [BookInstallSource.DOWNLOADED]
   *   (catalog download / file import). Preloaded APK bundles pass [BookInstallSource.ASSET] to
   *   mark the book as a non-removable built-in (see UninstallBookUseCaseImpl). An existing ASSET
   *   marker is never downgraded on re-import.
   */
  suspend fun import(bundle: BookBundle, source: BookInstallSource = BookInstallSource.DOWNLOADED) {
    Timber.i("Importing book ${bundle.book.id} (${bundle.songs.size} songs) as $source")
    importBookBundle(bundle, source).getOrElse { error -> throw error.toThrowable() }
    Timber.i("Import complete: ${bundle.book.id}")
  }

  /** The failure as it was thrown before the move: the original exception of the failed write. */
  private fun UpsertError.toThrowable(): Throwable = when (this) {
    is UpsertError.UnknownError -> cause ?: IllegalStateException(message)
    is UpsertError.ValidationError -> IllegalStateException(message)
  }
}
