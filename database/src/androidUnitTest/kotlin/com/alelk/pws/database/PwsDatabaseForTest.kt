package com.alelk.pws.database

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import io.github.alelk.pws.database.common.distinctBy
import io.github.alelk.pws.database.common.entity.BookEntity
import io.github.alelk.pws.database.common.entity.SongEntity
import io.github.alelk.pws.database.common.entity.SongNumberEntity
import io.github.alelk.pws.database.common.entity.TagEntity
import io.github.alelk.pws.database.common.entity.bookEntity
import io.github.alelk.pws.database.common.entity.songEntity
import io.github.alelk.pws.database.common.entity.songNumberEntity
import io.github.alelk.pws.database.common.entity.tagEntity
import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.next
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.take
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList

fun pwsDbForTest(context: Context = ApplicationProvider.getApplicationContext()): PwsDatabase =
  Room.inMemoryDatabaseBuilder(context, PwsDatabase::class.java).build()

suspend fun <T> PwsDatabase.withBookEntities(books: List<BookEntity>, body: suspend (books: List<BookEntity>) -> T): T {
  val db = this
  val bookIds = db.bookDao().insert(books)
  check(bookIds.size == books.size)
  return coroutineScope { body(books.zip(bookIds).map { (b, id) -> b.copy(id = id) }) }
}

suspend fun <T> PwsDatabase.withBookEntities(
  countBooks: Int,
  booksArb: Arb<BookEntity> = Arb.bookEntity().distinctBy { it.id }.distinctBy { it.externalId },
  body: suspend (books: List<BookEntity>) -> T
): T =
  this.withBookEntities(booksArb.take(countBooks).toList(), body)

suspend fun <T> PwsDatabase.withSongEntities(songs: List<SongEntity>, body: suspend (songs: List<SongEntity>) -> T): T {
  val db = this
  val songIds = db.songDao().insert(songs)
  check(songIds.size == songs.size)
  return body(songs)
}

suspend fun <T> PwsDatabase.withSongEntities(
  countSongs: Int,
  songsArb: Arb<SongEntity> = Arb.songEntity().distinctBy { it.id },
  body: suspend (songs: List<SongEntity>) -> T
): T =
  this.withSongEntities(songsArb.take(countSongs).toList(), body)

suspend fun <T> PwsDatabase.withSongNumberEntities(songNumbers: List<SongNumberEntity>, body: suspend (songs: List<SongNumberEntity>) -> T): T {
  val db = this
  val songNumberIds = db.songNumberDao().insert(songNumbers)
  check(songNumberIds.size == songNumberIds.size)
  return body(songNumbers.zip(songNumberIds).map { (songNumber, id) -> songNumber.copy(id = id) })
}

suspend fun <T> PwsDatabase.withSongNumberEntities(
  countSongNumbers: Int,
  bookId: Arb<Long>? = null,
  songId: Arb<Long>? = null,
  body: suspend (songs: List<SongNumberEntity>) -> T
): T {
  val db = this
  val songNumbers = db.withTransaction {
    val bookIds = bookId ?: Arb.of(db.bookDao().getAll().map { it.id!! }.toList())
    val songIds = songId ?: Arb.of(db.songDao().getAll().map { it.id }.toList())
    Arb
      .songNumberEntity(bookId = bookIds, songId = songIds)
      .distinctBy { it.songId to it.bookId }
      .distinctBy { it.bookId to it.number }
      .take(countSongNumbers)
      .toList()
  }
  return db.withSongNumberEntities(songNumbers, body)
}

suspend fun <T> PwsDatabase.withTagEntities(tags: List<TagEntity>, body: suspend (tags: List<TagEntity>) -> T): T {
  val db = this
  db.tagDao().insert(tags)
  return body(tags)
}

suspend fun <T> PwsDatabase.withTagEntities(
  countTags: Int,
  tag: Arb<TagEntity> = Arb.tagEntity(),
  body: suspend (tags: List<TagEntity>) -> T
): T {
  val tags = Arb.list(tag.distinctBy { it.id }, countTags..countTags).next()
  return this.withTagEntities(tags, body)
}