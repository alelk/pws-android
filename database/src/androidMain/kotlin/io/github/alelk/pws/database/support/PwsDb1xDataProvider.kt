package io.github.alelk.pws.database.support

import android.database.sqlite.SQLiteDatabase
import androidx.core.database.getStringOrNull
import io.github.alelk.pws.domain.bible.BibleRef
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.Color
import io.github.alelk.pws.domain.core.SongNumber
import io.github.alelk.pws.domain.core.ids.TagId
import io.github.alelk.pws.domain.tonality.Tonality
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format.char

class PwsDb1xDataProvider(val db: SQLiteDatabase) : PwsDbDataProvider {

  override val dbVersions: IntRange = 1..10

  /** Get favorites.
   *
   * Database versions:
   * - 0.9.1 (1)
   * - 1.1.0 (2)
   * - 1.2.0 (3, 4, 5)
   * - 1.8.0 (6, 7, 8, 9, 10)
   */
  override suspend fun getFavorites(): Result<List<SongNumber>> =
    db.fetchData(
      collectionName = "favorites",
      table = "favorites as f inner join psalmnumbers as pn on f.psalmnumberid=pn._id inner join books as b on pn.bookid=b._id",
      columns = arrayOf("f.position as position", "pn.number as number", "b.edition as edition"),
      dbVersion = 1..10
    ) { cursor ->
      val bookId = cursor.getString(cursor.getColumnIndexOrThrow("edition"))
      val songNumber = cursor.getInt(cursor.getColumnIndexOrThrow("number"))
      SongNumber(BookId.parse(bookId), songNumber)
    }

  val TIMESTAMP_FORMAT = LocalDateTime.Format { date(LocalDate.Formats.ISO); char(' '); time(LocalTime.Formats.ISO) }

  /** Get history.
   *
   * Database versions:
   * - 0.9.1 (1)
   * - 1.1.0 (2)
   * - 1.2.0 (3, 4, 5)
   * - 1.8.0 (6, 7, 8, 9, 10)
   */
  override suspend fun getHistory(): Result<List<HistoryItem>> =
    db.fetchData(
      collectionName = "history",
      table = "history as h inner join psalmnumbers as pn on h.psalmnumberid=pn._id inner join books as b on pn.bookid=b._id",
      columns = arrayOf("h.accesstimestamp as accesstimestamp", "pn.number as number", "b.edition as edition"),
      dbVersion = 1..10
    ) { cursor ->
      val bookId = cursor.getString(cursor.getColumnIndexOrThrow("edition"))
      val songNumber = cursor.getInt(cursor.getColumnIndexOrThrow("number"))
      val timestamp = cursor.getString(cursor.getColumnIndexOrThrow("accesstimestamp"))
      HistoryItem(SongNumber(BookId.parse(bookId), songNumber), TIMESTAMP_FORMAT.parse(timestamp))
    }

  /** Get edited songs.
   *
   * Database versions:
   * - 1.2.0 (3, 4, 5)
   * - 1.8.0 (6, 7, 8, 9, 10)
   */
  override suspend fun getEditedSongs(): Result<List<SongChange>> =
    db.fetchData(
      dbVersion = 4..10,
      collectionName = "edited songs",
      table = "psalms as p inner join psalmnumbers as pn on pn.psalmid=p._id inner join books b on pn.bookid=b._id",
      columns = arrayOf("p.text as text", "p.bibleref as bibleref", "p.tonalities as tonalities", "pn.number as number", "b.edition as edition"),
      selection = "p.edited=?",
      selectionArgs = arrayOf(1.toString()),
    ) { cursor ->
      val bookId = cursor.getString(cursor.getColumnIndexOrThrow("edition"))
      val songNumber = cursor.getInt(cursor.getColumnIndexOrThrow("number"))
      val tonalities = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("tonalities"))
      val lyric = cursor.getString(cursor.getColumnIndexOrThrow("text"))
      val bibleRef = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("bibleref"))
      SongChange(
        SongNumber(BookId.parse(bookId), songNumber),
        lyric = lyric,
        tonalities = tonalities?.split(';')?.map { Tonality.fromIdentifier(it.trim()) },
        bibleRef = bibleRef?.takeIf { it.isNotBlank() }?.let(::BibleRef)
      )
    }

  /** Get song tags.
   *
   * Database versions:
   * - 1.8.0 (6, 7, 8, 9, 10)
   */
  override suspend fun getTags(): Result<List<Tag>> =
    db.fetchData(
      dbVersion = 6..10,
      collectionName = "tags",
      table =
      """
        |tags as t inner join song_number_tags snt on t.id = snt.tag_id 
        |inner join psalmnumbers pn on snt.song_number_id = pn._id 
        |inner join books b on pn.bookid = b._id
        |""".trimMargin(),
      columns = arrayOf("t.id as id", "t.name as name", "t.color as color", "t.predefined as predefined", "b.edition as book_id", "pn.number as song_number"),
    ) { cursor ->
      val tagId = cursor.getString(cursor.getColumnIndexOrThrow("id"))
      val tagName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
      val tagColor = cursor.getString(cursor.getColumnIndexOrThrow("color"))
      val predefined = cursor.getString(cursor.getColumnIndexOrThrow("predefined"))
      val bookId = cursor.getString(cursor.getColumnIndexOrThrow("book_id"))
      val songNumber = cursor.getInt(cursor.getColumnIndexOrThrow("song_number"))
      Tag(
        id = TagId.parse(tagId),
        name = tagName,
        color = Color.parse(tagColor.trim()),
        predefined = predefined == "1" || predefined.toBoolean(),
        songNumbers = mapOf(BookId.parse(bookId.trim()) to setOf(songNumber))
      )
    }.map { tags ->
      tags
        .groupBy { it.id }
        .mapNotNull { (_, values) ->
          runCatching {
            val tag = values.first()
            check(values.all { it.id == tag.id }) { "impossible state" }
            check(values.all { it.name == tag.name }) { "expected all tag ${tag.id} to have name '${tag.name}'" }
            check(values.all { it.color == tag.color }) { "expected all tag ${tag.id} to have color ${tag.color}" }
            check(values.all { it.predefined == tag.predefined }) { "expected all tag ${tag.id} to be predefined=${tag.predefined}" }
            val songNumbers =
              values
                .flatMap { it.songNumbers.toList() }
                .groupBy { (bookId, _) -> bookId }
                .mapValues { (_, values) -> values.flatMap { (_, numbers) -> numbers }.toSet() }
            tag.copy(songNumbers = songNumbers)
          }.getOrNull()
        }
    }
}