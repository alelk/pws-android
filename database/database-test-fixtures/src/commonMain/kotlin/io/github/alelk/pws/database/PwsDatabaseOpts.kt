package io.github.alelk.pws.database

import androidx.room.exclusiveTransaction
import androidx.room.useWriterConnection
import io.github.alelk.pws.database.entity.BookEntity
import io.github.alelk.pws.database.entity.SongEntity
import io.github.alelk.pws.database.entity.SongNumberEntity
import io.github.alelk.pws.database.entity.TagEntity
import io.github.alelk.pws.database.entity.bookEntity
import io.github.alelk.pws.database.entity.songEntity
import io.github.alelk.pws.database.entity.songNumberEntity
import io.github.alelk.pws.database.entity.tagEntity
import io.github.alelk.pws.domain.distinctBy
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.ids.SongId
import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.next
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.removeEdgecases
import io.kotest.property.arbitrary.take
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList

suspend fun PwsDatabase.clean() {
  val db = this
  db.useWriterConnection {
    it.exclusiveTransaction {
      db.songReferenceDao().deleteAll()
      db.historyDao().deleteAll()
      db.favoriteDao().deleteAll()
      db.bookStatisticDao().deleteAll()
      db.songNumberDao().deleteAll()
      db.songDao().deleteAll()
      db.bookDao().deleteAll()
    }
  }
}

suspend fun <T> PwsDatabase.withBookEntities(books: List<BookEntity>, body: suspend (books: List<BookEntity>) -> T): T {
  val db = this
  val bookIds = books.map { it.id }
  check(bookIds.distinct().size == bookIds.size)
  db.bookDao().insert(books)
  return coroutineScope { body(books.zip(bookIds).map { (b, id) -> b.copy(id = id) }) }
}

suspend fun <T> PwsDatabase.withBookEntities(
  countBooks: Int,
  books: Arb<BookEntity> = Arb.bookEntity().removeEdgecases().distinctBy { it.id },
  body: suspend (books: List<BookEntity>) -> T
): T = this.withBookEntities(books.take(countBooks).toList(), body)

suspend fun <T> PwsDatabase.withSongEntities(songs: List<SongEntity>, body: suspend (songs: List<SongEntity>) -> T): T {
  val db = this
  val songIds = songs.map { it.id }
  check(songIds.distinct().size == songIds.size)
  db.songDao().insert(songs)
  return body(songs)
}

suspend fun <T> PwsDatabase.withSongEntities(
  countSongs: Int,
  songsArb: Arb<SongEntity> = Arb.songEntity().removeEdgecases().distinctBy { it.id },
  body: suspend (songs: List<SongEntity>) -> T
): T =
  this.withSongEntities(songsArb.take(countSongs).toList(), body)

suspend fun <T> PwsDatabase.withTagEntities(tags: List<TagEntity>, body: suspend (tags: List<TagEntity>) -> T): T {
  val db = this
  db.tagDao().insert(tags)
  return body(tags)
}

suspend fun <T> PwsDatabase.withTagEntities(
  countTags: Int,
  tag: Arb<TagEntity> = Arb.tagEntity().removeEdgecases(),
  body: suspend (tags: List<TagEntity>) -> T
): T {
  val tags = Arb.list(tag.distinctBy { it.id }, countTags..countTags).next()
  return this.withTagEntities(tags, body)
}

suspend fun <T> PwsDatabase.withSongNumberEntities(songNumbers: List<SongNumberEntity>, body: suspend (songs: List<SongNumberEntity>) -> T): T {
  val db = this
  check(songNumbers.distinctBy { it.id }.size == songNumbers.size) { "Song numbers must have unique id" }
  db.songNumberDao().insert(songNumbers)
  return body(songNumbers)
}

suspend fun <T> PwsDatabase.withSongNumberEntities(
  countSongNumbers: Int,
  bookId: Arb<BookId>? = null,
  songId: Arb<SongId>? = null,
  body: suspend (songs: List<SongNumberEntity>) -> T
): T {
  val db = this
  val bookIds = bookId ?: Arb.of(db.bookDao().getAll().map { it.id }.toList())
  val songIds = songId ?: Arb.of(db.songDao().getAll().map { it.id }.toList())
  val songNumbers =
    Arb.songNumberEntity(bookId = bookIds, songId = songIds)
      .distinctBy { it.songId to it.bookId }
      .distinctBy { it.bookId to it.number }
      .take(countSongNumbers)
      .toList()
  return db.withSongNumberEntities(songNumbers, body)
}
