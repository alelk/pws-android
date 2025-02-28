package io.github.alelk.pws.database.support

import android.database.sqlite.SQLiteDatabase
import androidx.core.database.getStringOrNull
import io.github.alelk.pws.domain.model.BibleRef
import io.github.alelk.pws.domain.model.BookId
import io.github.alelk.pws.domain.model.SongNumber
import io.github.alelk.pws.domain.model.Tonality
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format.char
import timber.log.Timber

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
  override suspend fun getFavorites(): Result<List<SongNumber>> = kotlin.runCatching {
    Timber.i("get favorites from pws database version=${db.version}, path=${db.path}...")
    val songNumbers = db.query(
      false,
      "favorites as f inner join psalmnumbers as pn on f.psalmnumberid=pn._id inner join books as b on pn.bookid=b._id",
      arrayOf("f.position as position", "pn.number as number", "b.edition as edition"),
      null, null, null, null, null, null
    ).use { cursor ->
      if (!cursor.moveToFirst()) {
        emptyList()
      } else
        flow {
          do {
            val songNumber = kotlin.runCatching {
              val bookId = cursor.getString(cursor.getColumnIndexOrThrow("edition"))
              val songNumber = cursor.getInt(cursor.getColumnIndexOrThrow("number"))
              SongNumber(BookId.parse(bookId), songNumber)
            }
            emit(songNumber)
          } while (cursor.moveToNext())
        }.toList()
          .also { Timber.i("${it.filter { v -> v.isSuccess }.size} favorites fetched from pws database version=${db.version}, path=${db.path}") }
    }
    val errors = songNumbers.mapNotNull { it.exceptionOrNull() }.distinctBy { it.message }
    if (errors.isNotEmpty()) {
      Timber.e(
        errors.first(),
        "error fetching favorites from pws database version=${db.version}, path=${db.path}: ${errors.joinToString(",") { it.message.toString() }}"
      )
    }
    songNumbers.mapNotNull { it.getOrNull() }
  }.onFailure { exc ->
    Timber.e(exc, "error fetching favorites from pws database version=${db.version}, path=${db.path}: ${exc.message}")
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
  override suspend fun getHistory(): Result<List<HistoryItem>> = kotlin.runCatching {
    Timber.i("get history from pws database version=${db.version}, path=${db.path}...")
    val historyItems = db.query(
      false,
      "history as h inner join psalmnumbers as pn on h.psalmnumberid=pn._id inner join books as b on pn.bookid=b._id",
      arrayOf("h.accesstimestamp as accesstimestamp", "pn.number as number", "b.edition as edition"),
      null, null, null, null, null, null
    ).use { cursor ->
      if (!cursor.moveToFirst()) {
        emptyList()
      } else
        flow {
          do {
            val historyItem = kotlin.runCatching {
              val bookId = cursor.getString(cursor.getColumnIndexOrThrow("edition"))
              val songNumber = cursor.getInt(cursor.getColumnIndexOrThrow("number"))
              val timestamp = cursor.getString(cursor.getColumnIndexOrThrow("accesstimestamp"))
              HistoryItem(SongNumber(BookId.parse(bookId), songNumber), TIMESTAMP_FORMAT.parse(timestamp))
            }
            emit(historyItem)
          } while (cursor.moveToNext())
        }.toList()
          .also { Timber.i("${it.filter { v -> v.isSuccess }.size} history items fetched from pws database version=${db.version}, path=${db.path}") }
    }
    val errors = historyItems.mapNotNull { it.exceptionOrNull() }.distinctBy { it.message }
    if (errors.isNotEmpty()) {
      Timber.e(
        errors.first(),
        "error fetching history from pws database version=${db.version}, path=${db.path}: " +
          errors.take(3).joinToString(",") { it.message.toString() }
      )
    }
    historyItems.mapNotNull { it.getOrNull() }
  }.onFailure { exc ->
    Timber.e(
      exc,
      "error fetching history from pws database version=${db.version}, path=${db.path}: ${exc.message}"
    )
  }

  /** Get history.
   *
   * Database versions:
   * - 1.8.0 (6, 7, 8, 9, 10)
   */
  override suspend fun getEditedSongs(): Result<List<SongChange>> =
    // todo: посмотреть, начиная с какой версии была возможность редактировать текст песен
    if (db.version < 6) {
      Timber.i("skip fetching songs from pws database version=${db.version}, path=${db.path} (song editing wasn't supported yet)")
      Result.success(emptyList())
    } else kotlin.runCatching {
      Timber.i("get edited songs from pws database version=${db.version}, path=${db.path}...")
      val songItems = db.query(
        false,
        "psalms as p inner join psalmnumbers as pn on pn.psalmid=p._id inner join books b on pn.bookid=b._id",
        arrayOf("p.text as text", "p.bibleref as bibleref", "p.tonalities as tonalities", "pn.number as number", "b.edition as edition"),
        "p.edited=?", arrayOf(1.toString()), null, null, null, null
      ).use { cursor ->
        if (!cursor.moveToFirst()) {
          emptyList()
        } else
          flow {
            do {
              val songChange = kotlin.runCatching {
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
              emit(songChange)
            } while (cursor.moveToNext())
          }.toList().distinct()
            .also { Timber.i("${it.filter { v -> v.isSuccess }.size} edited songs fetched from pws database version=${db.version}, path=${db.path}") }
      }
      val errors = songItems.mapNotNull { it.exceptionOrNull() }.distinctBy { it.message }
      if (errors.isNotEmpty()) {
        Timber.e(
          errors.first(),
          "error fetching edited songs from pws database version=${db.version}, path=${db.path}: " +
            errors.take(3).joinToString(",") { it.message.toString() }
        )
      }
      songItems.mapNotNull { it.getOrNull() }.filter { it.lyric.isNotBlank() }
    }.onFailure { exc -> Timber.e(exc, "error fetching edited songs from pws database version=${db.version}, path=${db.path}: ${exc.message}") }


}
