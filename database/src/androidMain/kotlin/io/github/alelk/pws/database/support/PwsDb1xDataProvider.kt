package io.github.alelk.pws.database.support

import android.database.sqlite.SQLiteDatabase
import io.github.alelk.pws.domain.model.BookId
import io.github.alelk.pws.domain.model.SongNumber
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
          .also {
            Timber.i("${it.size} favorites fetched from pws database version=${db.version}, path=${db.path}")
          }
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
    Timber.e(
      exc,
      "error fetching favorites from pws database version=${db.version}, path=${db.path}: ${exc.message}"
    )
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
          .also {
            Timber.i("${it.size} history items fetched from pws database version=${db.version}, path=${db.path}")
          }
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
}
