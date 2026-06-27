package io.github.alelk.pws.contentdelivery.install

import androidx.room.withTransaction
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.book.BookEntity
import io.github.alelk.pws.database.bookstatistic.BookStatisticEntity
import io.github.alelk.pws.database.installed_book.InstalledBookEntity
import io.github.alelk.pws.domain.booklibrary.model.BookInstallSource
import io.github.alelk.pws.database.song.SongEntity
import io.github.alelk.pws.database.song_number.SongNumberEntity
import io.github.alelk.pws.database.song_reference.SongRefReason
import io.github.alelk.pws.database.song_reference.SongReferenceEntity
import io.github.alelk.pws.database.song_tag.SongTagEntity
import io.github.alelk.pws.database.tag.TagEntity
import io.github.alelk.pws.portable.model.BookBundle
import timber.log.Timber

class BookImporterImpl(private val db: PwsDatabase) {

    suspend fun import(bundle: BookBundle) {
        Timber.i("Importing book ${bundle.book.id} (${bundle.songs.size} songs)")
        db.withTransaction {
            val book = bundle.book

            // 1. Upsert BookEntity
            db.bookDao().update(
                BookEntity(
                    id = book.id,
                    version = book.version,
                    locales = book.locales,
                    name = book.name,
                    displayShortName = book.displayShortName,
                    displayName = book.displayName,
                    releaseDate = book.releaseDate,
                    authors = book.authors?.takeIf { it.isNotEmpty() },
                    creators = book.creators?.takeIf { it.isNotEmpty() },
                    reviewers = book.reviewers?.takeIf { it.isNotEmpty() },
                    editors = book.editors?.takeIf { it.isNotEmpty() },
                    description = book.description,
                    preface = book.preface,
                )
            )

            // 2. Upsert BookStatisticEntity
            db.bookStatisticDao().upsert(
                BookStatisticEntity(id = book.id, priority = book.priority)
            )

            // 3. Upsert SongEntity (skip user-edited songs to preserve lyrics)
            for (song in bundle.songs) {
                val songId = song.id ?: continue
                val existing = db.songDao().getById(songId)
                if (existing?.edited == true) continue
                db.songDao().insert(
                    SongEntity(
                        id = songId,
                        version = song.version,
                        locale = song.locale,
                        name = song.name,
                        lyric = song.lyric,
                        author = song.author,
                        translator = song.translator,
                        composer = song.composer,
                        tonalities = song.tonalities,
                        year = song.year,
                        bibleRef = song.bibleRef,
                    )
                )
            }

            // 4. Upsert SongNumberEntity for this book
            for (song in bundle.songs) {
                val songId = song.id ?: continue
                for ((index, sn) in song.allNumbers.withIndex()) {
                    if (sn.bookId != book.id) continue
                    val existing = db.songNumberDao().getById(book.id, songId)
                    val entity = SongNumberEntity(
                        bookId = book.id,
                        songId = songId,
                        number = sn.number,
                        priority = index,
                    )
                    if (existing != null) db.songNumberDao().update(entity) else db.songNumberDao().insert(entity)
                }
            }

            // 5. SongReferenceEntity — skip refs where either song isn't installed yet
            val songRefs = bundle.songReferences.orEmpty()
            if (songRefs.isNotEmpty()) {
                val bundleSongIds = bundle.songs.mapNotNull { it.id }.toSet()

                // Collect IDs from both sides that are not in this bundle (may be from other books)
                val externalIds = songRefs.flatMap { listOf(it.songId, it.refSongId) }
                    .filter { it !in bundleSongIds }
                    .distinct()

                val existingExternalIds = if (externalIds.isNotEmpty()) {
                    db.songDao().getByIds(externalIds).map { it.id }.toSet()
                } else emptySet()

                val availableIds = bundleSongIds + existingExternalIds
                var deferred = 0

                songRefs.forEach { ref ->
                    if (ref.songId !in availableIds || ref.refSongId !in availableIds) {
                        deferred++
                        return@forEach
                    }
                    val existing = db.songReferenceDao().getById(ref.songId, ref.refSongId)
                    if (existing == null) {
                        runCatching {
                            db.songReferenceDao().insert(
                                SongReferenceEntity(
                                    songId = ref.songId,
                                    refSongId = ref.refSongId,
                                    reason = SongRefReason.fromIdentifier(ref.reason),
                                    volume = ref.volume,
                                )
                            )
                        }.onFailure { Timber.w(it, "skip song reference ${ref.songId}→${ref.refSongId}") }
                    }
                }
                if (deferred > 0) Timber.d("Deferred $deferred cross-book song references")
            }

            // 6. Upsert TagEntity + insert SongTagEntity (predefined tags from bundle)
            bundle.tags?.forEach { tag ->
                val tagId = tag.id ?: return@forEach  // skip tags without ID
                db.tagDao().update(
                    TagEntity(
                        id = tagId,
                        name = tag.name,
                        predefined = tag.predefined,
                        color = tag.color,
                        priority = tag.priority,
                    )
                )
                tag.songs.forEach { songNumber ->
                    val songId = bundle.songs.find { s ->
                        s.allNumbers.any {
                            it.bookId == songNumber.bookId && it.number == songNumber.number
                        }
                    }?.id ?: return@forEach
                    db.songTagDao().insertIfMissing(
                        listOf(SongTagEntity(songId = songId, tagId = tagId, priority = 0))
                    )
                }
            }

            // 7. Mark as installed (DOWNLOADED)
            db.installedBookDao().upsert(
                InstalledBookEntity(
                    bookId = book.id,
                    bundleVersion = book.version,
                    installedAt = System.currentTimeMillis(),
                    source = BookInstallSource.DOWNLOADED,
                )
            )
        }
        Timber.i("Import complete: ${bundle.book.id}")
    }
}
