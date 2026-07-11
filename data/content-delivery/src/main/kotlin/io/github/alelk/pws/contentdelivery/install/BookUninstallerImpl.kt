package io.github.alelk.pws.contentdelivery.install

import androidx.room.withTransaction
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.domain.booklibrary.model.BookInstallSource
import io.github.alelk.pws.domain.core.ids.BookId
import timber.log.Timber

class BookUninstallerImpl(private val db: PwsDatabase) {

    suspend fun uninstall(bookId: BookId) {
        Timber.i("Uninstalling book $bookId")
        db.withTransaction {
            val installed = db.installedBookDao().getByBookId(bookId)
                ?: error("Book $bookId is not installed")
            check(installed.source != BookInstallSource.ASSET) {
                "Cannot uninstall built-in book $bookId"
            }

            // 1. Collect song IDs for this book
            val songNumbers = db.songNumberDao().getByBookIds(listOf(bookId))
            val songIds = songNumbers.map { it.songId }.toSet()

            // 2. Determine orphans BEFORE removing song_numbers:
            //    a song is orphan if it has no song_numbers in any other book
            val orphanSongIds = songIds.filter { songId ->
                db.songNumberDao().getBySongId(songId).none { it.bookId != bookId }
            }
            Timber.d("Uninstall $bookId: ${songIds.size} songs, ${orphanSongIds.size} orphans")

            // 3. Delete this book's song_numbers (CASCADE removes favorites + history)
            db.songNumberDao().delete(songNumbers)

            // 4. Delete orphan songs; their song_tags and song_references
            //    are removed automatically via FK CASCADE on SongEntity
            orphanSongIds.forEach { db.songDao().deleteById(it) }

            // 5. Delete BookStatisticEntity
            db.bookStatisticDao().getById(bookId)?.let { db.bookStatisticDao().delete(it) }

            // 6. Delete BookEntity
            db.bookDao().getById(bookId)?.let { db.bookDao().delete(it) }

            // 7. Delete InstalledBookEntity
            db.installedBookDao().deleteByBookId(bookId)

            // 8. Safety sweep for any remaining orphans (not user-edited)
            val orphanCount = db.songDao().deleteOrphans()
            if (orphanCount > 0) Timber.d("Deleted $orphanCount orphan songs after uninstall")
        }
        Timber.i("Uninstall complete: $bookId")
    }
}
