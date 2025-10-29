package io.github.alelk.pws.database.support

import android.database.sqlite.SQLiteDatabase
import androidx.core.database.getIntOrNull
import androidx.core.database.getShortOrNull
import androidx.core.database.getStringOrNull
import io.github.alelk.pws.domain.model.BibleRef
import io.github.alelk.pws.domain.model.BookId
import io.github.alelk.pws.domain.model.Color
import io.github.alelk.pws.domain.model.SongNumber
import io.github.alelk.pws.domain.model.TagId
import io.github.alelk.pws.domain.model.Tonality
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format.char

class PwsDb2xDataProvider(val db: SQLiteDatabase) : PwsDbDataProvider {

  override val dbVersions: IntRange = 11..11

  /** Get favorites.
   *
   * Database versions:
   * - 2.0.0 (11)
   */
  override suspend fun getFavorites(): Result<List<SongNumber>> =
    db.fetchData(
      dbVersion = 11..11,
      collectionName = "favorites",
      table = "favorites as f inner join song_numbers as sn on f.song_id=sn.song_id and f.book_id=sn.book_id",
      columns = arrayOf("f.position as position", "sn.number as number", "f.book_id as book_id")
    ) { cursor ->
      val bookId = cursor.getString(cursor.getColumnIndexOrThrow("book_id"))
      val songNumber = cursor.getInt(cursor.getColumnIndexOrThrow("number"))
      SongNumber(BookId.parse(bookId), songNumber)
    }

  val TIMESTAMP_FORMAT = LocalDateTime.Format { date(LocalDate.Formats.ISO); char(' '); time(LocalTime.Formats.ISO) }

  /** Get history.
   *
   * Database versions:
   * - 2.0.0 (11)
   */
  override suspend fun getHistory(): Result<List<HistoryItem>> =
    db.fetchData(
      dbVersion = 11..11,
      collectionName = "history",
      table = "history as h inner join song_numbers as sn on h.song_id=sn.song_id and h.book_id=sn.book_id",
      columns = arrayOf("h.access_timestamp as access_timestamp", "sn.number as number", "h.book_id as book_id"),
    ) { cursor ->
      val bookId = cursor.getString(cursor.getColumnIndexOrThrow("book_id"))
      val songNumber = cursor.getInt(cursor.getColumnIndexOrThrow("number"))
      val timestamp = cursor.getString(cursor.getColumnIndexOrThrow("access_timestamp"))
      HistoryItem(SongNumber(BookId.parse(bookId), songNumber), TIMESTAMP_FORMAT.parse(timestamp))
    }

  /** Get edited songs.
   *
   * Database versions:
   * - 2.0.0 (11)
   */
  override suspend fun getEditedSongs(): Result<List<SongChange>> =
    db.fetchData(
      dbVersion = 11..11,
      collectionName = "edited songs",
      table = "songs as s inner join song_numbers as sn on sn.song_id=s.id",
      columns = arrayOf("s.lyric as lyric", "s.bibleref as bibleref", "s.tonalities as tonalities", "sn.number as number", "sn.book_id as book_id"),
      selection = "s.edited=?",
      selectionArgs = arrayOf(1.toString()),
    ) { cursor ->
      val bookId = cursor.getString(cursor.getColumnIndexOrThrow("book_id"))
      val songNumber = cursor.getInt(cursor.getColumnIndexOrThrow("number"))
      val tonalities = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("tonalities"))
      val lyric = cursor.getString(cursor.getColumnIndexOrThrow("lyric"))
      val bibleRef = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("bibleref"))
      SongChange(
        SongNumber(BookId.parse(bookId), songNumber),
        lyric = lyric,
        tonalities = tonalities?.split(';')?.filter { it.isNotBlank() }?.map { Tonality.fromIdentifier(it.trim()) },
        bibleRef = bibleRef?.takeIf { it.isNotBlank() }?.let(::BibleRef)
      )
    }

  /** Get song tags.
   *
   * Database versions:
   * - 2.0.0 (11)
   */
  override suspend fun getTags(): Result<List<Tag>> =
    db.fetchData(
      dbVersion = 11..11,
      collectionName = "tags",
      table =
        """
        |tags as t left join song_tags st on t.id = st.tag_id 
        |left join songs s on st.song_id = s.id  
        |left join song_numbers sn on st.song_id = sn.song_id
        |""".trimMargin(),
      columns = arrayOf("t.id as id", "t.name as name", "t.color as color", "t.predefined as predefined", "sn.book_id as book_id", "sn.number as song_number"),
    ) { cursor ->
      val tagId = cursor.getString(cursor.getColumnIndexOrThrow("id"))
      val tagName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
      val tagColor = cursor.getString(cursor.getColumnIndexOrThrow("color"))
      val predefined = cursor.getString(cursor.getColumnIndexOrThrow("predefined"))
      val bookId = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("book_id"))
      val songNumber = cursor.getIntOrNull(cursor.getColumnIndexOrThrow("song_number"))
      Tag(
        id = TagId.parse(tagId),
        name = tagName,
        color = Color.parse(tagColor.trim()),
        predefined = predefined == "1" || predefined.toBoolean(),
        songNumbers = if (bookId != null && songNumber != null) mapOf(BookId.parse(bookId.trim()) to setOf(songNumber)) else mapOf()
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

