package io.github.alelk.pws.database.provider

import android.app.SearchManager
import android.database.Cursor
import android.database.MatrixCursor
import android.provider.BaseColumns
import io.github.alelk.pws.database.search.buildFtsPrefixQuery
import io.github.alelk.pws.database.song.SongDao
import timber.log.Timber

private val EMPTY_SUGGESTION_COLUMNS =
  arrayOf(
    BaseColumns._ID,
    SearchManager.SUGGEST_COLUMN_TEXT_1,
    SearchManager.SUGGEST_COLUMN_TEXT_2,
    SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID,
  )

internal fun emptySuggestions(): Cursor = MatrixCursor(EMPTY_SUGGESTION_COLUMNS)

/**
 * Text suggestions for the system `SearchManager`: by song name first, by lyric when the name gives nothing.
 *
 * The user's [rawText] is turned into a safe FTS query by [buildFtsPrefixQuery] (whole words stay separate terms,
 * FTS syntax is neutralised). Nothing searchable in the input, or any SQLite failure, yields an empty cursor —
 * a content provider must not throw because of what the user typed.
 */
internal fun SongDao.suggestionsByText(rawText: String, limit: Int): Cursor {
  val ftsQuery = buildFtsPrefixQuery(rawText)
  if (ftsQuery.isEmpty()) return emptySuggestions()
  return try {
    val byName = getSuggestionsBySongName(ftsQuery, limit)
    if (byName.count > 0) {
      byName
    } else {
      byName.close()
      getSuggestionsBySongLyric(ftsQuery, limit)
    }
  } catch (e: Exception) {
    Timber.w(e, "text suggestions failed (query length = ${rawText.length})")
    emptySuggestions()
  }
}
