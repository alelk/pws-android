package io.github.alelk.pws.contentdelivery.install

import io.github.alelk.pws.database.song.SongDao
import io.github.alelk.pws.database.song_number.SongNumberDao
import io.github.alelk.pws.database.song_number.SongNumberEntity
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.portable.model.Song
import timber.log.Timber

/**
 * Reconciles the `song_numbers` of a book with a freshly imported bundle.
 *
 * The bundle optimizer deduplicates identical songs to a shared id and may assign a *different*
 * canonical id to the same content between builds. A naive per-number upsert cannot express an id
 * permutation (e.g. numbers 1 and 2 trading song ids) without transiently violating one of the
 * `song_numbers` unique keys — `(book_id, song_id)` or `(book_id, number)` — which aborts the whole
 * import transaction.
 *
 * Strategy — minimal-touch reconciliation:
 *  - rows already matching the bundle are left untouched. This is the common case (content-only
 *    update) and it preserves `favorites` and `history`, which cascade-delete together with their
 *    `song_numbers` row;
 *  - rows whose number changed song id, or whose number disappeared, are deleted **before** any
 *    insert, freeing both unique keys so arbitrary id permutations (incl. swaps) re-insert cleanly;
 *  - new/changed numbers are then inserted.
 *
 * User-edited songs are protected: their numbers are kept, never reassigned to another song, and the
 * song itself is never moved to a different number.
 */
class SmartSongBinder(
    private val songNumberDao: SongNumberDao,
    private val songDao: SongDao,
) {

    private data class Target(val songId: SongId, val priority: Int)

    suspend fun bind(bookId: BookId, bundleSongs: List<Song>) {
        val currentRows = songNumberDao.getByBookId(bookId)
        val editedIds =
            if (currentRows.isEmpty()) emptySet()
            else songDao.getByIds(currentRows.map { it.songId }).filter { it.edited }.map { it.id }.toSet()

        val protectedRows = currentRows.filter { it.songId in editedIds }
        val protectedNumbers = protectedRows.map { it.number }.toSet()
        val protectedSongIds = protectedRows.map { it.songId }.toSet()

        // Desired state from the bundle (number -> song), excluding slots and ids reserved by
        // user-edited songs.
        val desired = LinkedHashMap<Int, Target>()
        for (song in bundleSongs) {
            val songId = song.id ?: continue
            val sn = song.allNumbers.find { it.bookId == bookId } ?: continue
            if (sn.number in protectedNumbers) {
                Timber.d("SmartSongBinder: keep user-edited slot #${sn.number} in book $bookId")
                continue
            }
            if (songId in protectedSongIds) continue
            desired[sn.number] = Target(songId, song.allNumbers.indexOfFirst { it.bookId == bookId })
        }

        val currentByNumber = currentRows.filterNot { it.songId in editedIds }.associateBy { it.number }

        // Delete rows whose number changed song id or disappeared from the bundle. Untouched rows
        // (number kept the same song id) keep their favorites/history.
        currentByNumber.values
            .filter { row -> desired[row.number]?.songId != row.songId }
            .forEach { row ->
                Timber.d("SmartSongBinder: drop #${row.number} (song ${row.songId}) in book $bookId")
                songNumberDao.deleteByBookIdAndSongId(bookId, row.songId)
            }

        // Insert new and remapped numbers (deletes above already freed any conflicting keys).
        desired.forEach { (number, target) ->
            if (currentByNumber[number]?.songId != target.songId) {
                songNumberDao.insert(
                    SongNumberEntity(bookId = bookId, songId = target.songId, number = number, priority = target.priority)
                )
            }
        }
    }
}
