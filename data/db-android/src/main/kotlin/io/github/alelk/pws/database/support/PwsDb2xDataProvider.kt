package io.github.alelk.pws.database.support

import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import io.github.alelk.pws.database.MigrationDbSource
import io.github.alelk.pws.database.book.BookEntity
import io.github.alelk.pws.database.bookstatistic.BookStatisticEntity
import io.github.alelk.pws.database.installed_book.InstalledBookEntity
import io.github.alelk.pws.database.song.SongEntity
import io.github.alelk.pws.database.song_number.SongNumberEntity
import io.github.alelk.pws.domain.booklibrary.model.BookInstallSource
import io.github.alelk.pws.domain.core.BibleRef
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.Color
import io.github.alelk.pws.domain.core.Locale
import io.github.alelk.pws.domain.core.SongNumber
import io.github.alelk.pws.domain.core.Version
import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.core.ids.TagId
import io.github.alelk.pws.domain.person.Person
import io.github.alelk.pws.domain.tonality.Tonality
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format.char
import timber.log.Timber

/** Supports database versions:
 * - 2.0.0 (v11)
 * - 3.0.0 (v12)
 * - 3.2.x (v13)
 */
internal class PwsDb2xDataProvider(val db: MigrationDbSource) : PwsDbDataProvider {

  override val dbVersions: IntRange = 11..13

  override suspend fun getBooks(): Result<List<BookMigrationData>> = runCatching {
    data class RawBook(val id: BookId, val name: String, val shortName: String, val displayName: String, val locale: Locale, val version: Version)

    val books = db.fetchData("books", "books", arrayOf("*"), 11..13) { cursor ->
      val id = BookId.parse(cursor.getString(cursor.getColumnIndexOrThrow("id")))
      val name = cursor.getStringOrNull(cursor.getColumnIndex("name"))?.takeIf { it.isNotBlank() } ?: id.toString()
      val shortNameIdx = cursor.getColumnIndex("display_short_name")
      val shortName = if (shortNameIdx >= 0) cursor.getStringOrNull(shortNameIdx)?.takeIf { it.isNotBlank() } ?: name else name
      val displayNameIdx = cursor.getColumnIndex("display_name")
      val displayName = if (displayNameIdx >= 0) cursor.getStringOrNull(displayNameIdx)?.takeIf { it.isNotBlank() } ?: name else name
      val localeIdx = cursor.getColumnIndex("locale")
      val locale = if (localeIdx >= 0) cursor.getStringOrNull(localeIdx)?.let { runCatching { Locale.of(it) }.getOrNull() } ?: Locale.RU else Locale.RU
      val versionIdx = cursor.getColumnIndex("version")
      val version = if (versionIdx >= 0) cursor.getStringOrNull(versionIdx)?.let { runCatching { Version.fromString(it) }.getOrNull() } ?: Version(0, 0) else Version(0, 0)
      RawBook(id, name, shortName, displayName, locale, version)
    }.getOrThrow()

    val priorityById: Map<BookId, Int> = runCatching {
      db.fetchData("book priorities", "book_statistic", arrayOf("*"), 11..13) { cursor ->
        val id = BookId.parse(cursor.getString(cursor.getColumnIndexOrThrow("id")))
        val priorityIdx = cursor.getColumnIndex("priority")
        val priority = if (priorityIdx >= 0) cursor.getIntOrNull(priorityIdx) ?: 1 else 1
        id to priority
      }.getOrThrow().toMap()
    }.onFailure { Timber.w("book_statistic table not found in old DB, using default priority") }
      .getOrDefault(emptyMap())

    data class RawSongRow(
      val songId: SongId, val bookId: BookId, val number: Int,
      val name: String, val lyric: String, val author: String?, val translator: String?,
      val composer: String?, val bibleRef: String?, val tonalities: String?, val edited: Boolean,
    )

    val songRows = db.fetchData(
      "songs with song_numbers",
      "songs s INNER JOIN song_numbers sn ON sn.song_id = s.id",
      arrayOf(
        "s.id as _sid", "COALESCE(s.name, '') as _sname", "COALESCE(s.lyric, '') as _lyric",
        "s.author as _author", "s.translator as _translator", "s.composer as _composer",
        "s.bibleref as _bibleref", "s.tonalities as _tonalities", "s.edited as _edited",
        "sn.book_id as _book_id", "sn.number as _snumber",
      ),
      11..13,
    ) { cursor ->
      RawSongRow(
        songId = SongId(cursor.getLong(cursor.getColumnIndexOrThrow("_sid"))),
        bookId = BookId.parse(cursor.getString(cursor.getColumnIndexOrThrow("_book_id"))),
        number = cursor.getInt(cursor.getColumnIndexOrThrow("_snumber")),
        name = cursor.getString(cursor.getColumnIndexOrThrow("_sname")) ?: "",
        lyric = cursor.getString(cursor.getColumnIndexOrThrow("_lyric")) ?: "",
        author = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("_author")),
        translator = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("_translator")),
        composer = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("_composer")),
        bibleRef = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("_bibleref")),
        tonalities = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("_tonalities")),
        edited = cursor.getInt(cursor.getColumnIndexOrThrow("_edited")) != 0,
      )
    }.getOrThrow()

    val songsByBookId = songRows.groupBy { it.bookId }

    books.mapNotNull { rawBook ->
      runCatching {
        val songsForBook = songsByBookId[rawBook.id] ?: emptyList()
        if (songsForBook.isEmpty()) return@runCatching null
        val songEntities = songsForBook.distinctBy { it.songId }.map { row ->
          SongEntity(
            id = row.songId,
            version = Version(0, 0),
            locale = rawBook.locale,
            name = row.name.ifBlank { "?" },
            lyric = row.lyric,
            author = row.author?.takeIf { it.isNotBlank() }?.let { Person(it) },
            translator = row.translator?.takeIf { it.isNotBlank() }?.let { Person(it) },
            composer = row.composer?.takeIf { it.isNotBlank() }?.let { Person(it) },
            bibleRef = row.bibleRef?.takeIf { it.isNotBlank() }?.let { BibleRef(it) },
            tonalities = row.tonalities?.takeIf { it.isNotBlank() }
              ?.split(';')?.filter { it.isNotBlank() }
              ?.mapNotNull { runCatching { Tonality.fromIdentifier(it.trim()) }.getOrNull() },
            edited = row.edited,
          )
        }
        val songNumberEntities = songsForBook.map { row ->
          SongNumberEntity(bookId = rawBook.id, songId = row.songId, number = row.number, priority = 0)
        }
        BookMigrationData(
          book = BookEntity(
            id = rawBook.id,
            version = rawBook.version,
            locales = listOf(rawBook.locale),
            name = rawBook.name,
            displayShortName = rawBook.shortName,
            displayName = rawBook.displayName,
          ),
          bookStatistic = BookStatisticEntity(id = rawBook.id, priority = priorityById[rawBook.id] ?: 1),
          installedBook = InstalledBookEntity(
            bookId = rawBook.id,
            bundleVersion = rawBook.version,
            installedAt = System.currentTimeMillis(),
            source = BookInstallSource.MIGRATION,
          ),
          songs = songEntities,
          songNumbers = songNumberEntities,
        )
      }.onFailure { e -> Timber.e(e, "failed to assemble book ${rawBook.id} for migration: ${e.message}") }
        .getOrNull()
    }
  }

  /** Get favorites.
   *
   * Database versions:
   * - 2.0.0 (11)
   * - 3.0.0 (12)
   * - 3.2.x (13)
   */
  override suspend fun getFavorites(): Result<List<SongNumber>> =
    db.fetchData(
      dbVersion = 11..13,
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
   * - 3.0.0 (12)
   * - 3.2.x (13)
   */
  override suspend fun getHistory(): Result<List<HistoryItem>> =
    db.fetchData(
      dbVersion = 11..13,
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
   * - 3.0.0 (12)
   * - 3.2.x (13)
   */
  override suspend fun getEditedSongs(): Result<List<SongChange>> =
    db.fetchData(
      dbVersion = 11..13,
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
   * - 3.0.0 (12)
   * - 3.2.x (13)
   */
  override suspend fun getTags(): Result<List<Tag>> =
    db.fetchData(
      dbVersion = 11..13,
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