package io.github.alelk.pws.database.dao

import android.app.SearchManager
import android.database.Cursor
import android.provider.BaseColumns
import androidx.room.Dao
import androidx.room.Query
import io.github.alelk.pws.database.entity.SongSearchResultEntity

@Dao
actual interface SongDao : SongDaoBase {

  @Query(
    """
    SELECT
      (sn.book_id | '/' | sn.song_id) AS ${SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID},
      s.name AS ${SearchManager.SUGGEST_COLUMN_TEXT_1},
      group_concat(b.display_short_name, ' | ') AS ${SearchManager.SUGGEST_COLUMN_TEXT_2},
      (sn.book_id | '/' | sn.song_id) AS ${BaseColumns._ID}
    FROM books AS b
    INNER JOIN book_statistic AS bs ON b.id = bs.id
    INNER JOIN song_numbers AS sn ON sn.book_id = b.id
    INNER JOIN songs_fts AS s ON s.docid = sn.song_id
    WHERE songs_fts MATCH :searchText AND bs.priority > 0
    GROUP BY s.docid
    LIMIT :limit
    """
  )
  fun getSuggestionsBySongLyric(searchText: String, limit: Int?): Cursor


  @Query(
    """
    SELECT
      (sn.book_id | '/' | sn.song_id) AS ${BaseColumns._ID},
      sn.number AS song_number,
      group_concat(b.display_short_name, ', ') AS ${SearchManager.SUGGEST_COLUMN_TEXT_2},
      s.name AS ${SearchManager.SUGGEST_COLUMN_TEXT_1},
      s.rowid AS song_id,
      (sn.book_id | '/' | sn.song_id) AS ${SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID}
    FROM books AS b
    INNER JOIN book_statistic AS bs ON b.id = bs.id
    INNER JOIN song_numbers AS sn ON sn.book_id = b.id
    INNER JOIN songs_fts AS s ON s.rowid = sn.song_id
    WHERE sn.number = :songNumber AND bs.priority > 0
    GROUP BY sn.number, s.rowid
    ORDER BY bs.priority DESC
    LIMIT :limit
    """
  )
  fun getSuggestionsBySongNumber(songNumber: Int, limit: Int? = 50): Cursor


  @Query(
    """
    SELECT
      (sn.book_id | '/' | sn.song_id) AS ${SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID},
      s.name AS ${SearchManager.SUGGEST_COLUMN_TEXT_1},
      group_concat(b.display_short_name, ' | ') AS ${SearchManager.SUGGEST_COLUMN_TEXT_2},
      (sn.book_id | '/' | sn.song_id) AS ${BaseColumns._ID}
    FROM books AS b
    INNER JOIN book_statistic AS bs ON b.id = bs.id
    INNER JOIN song_numbers AS sn ON sn.book_id = b.id
    INNER JOIN songs_fts AS s ON s.docid = sn.song_id
    WHERE s.name MATCH :searchName AND bs.priority > 0
    GROUP BY s.docid
    LIMIT :limit
    """
  )
  fun getSuggestionsBySongName(searchName: String, limit: Int?): Cursor


  @Query(
    """
    SELECT
      sn.book_id AS bookId,
      sn.song_id AS songId,
      s.name AS songName,
      sn.number AS songNumber,
      b.display_name AS bookDisplayName,
      substr(s.lyric, 1, 100) AS snippet
    FROM books AS b
    INNER JOIN book_statistic AS bs ON b.id = bs.id
    INNER JOIN song_numbers AS sn ON sn.book_id = b.id
    INNER JOIN songs AS s ON s.id = sn.song_id
    WHERE sn.number = :songNumber AND bs.priority > 0
    ORDER BY bs.priority DESC
    LIMIT :limit
    """
  )
  suspend fun findBySongNumber(songNumber: Int, limit: Int?): List<SongSearchResultEntity>

  @Query(
    """
    SELECT
      b.id AS bookId,
      sn.song_id AS songId,
      s.name AS songName,
      sn.number AS songNumber,
      b.display_name AS bookDisplayName,
      snippet(songs_fts, '<b><font color=#247b34>', '</font></b>', '...') as snippet,
      matchinfo(songs_fts, 'x') as matchinfo
    FROM books AS b
    INNER JOIN book_statistic AS bs ON b.id = bs.id
    INNER JOIN song_numbers AS sn ON sn.book_id = b.id
    INNER JOIN songs_fts AS s ON s.docid = sn.song_id
    WHERE bs.priority > 0 AND songs_fts MATCH :songText
    ORDER BY matchinfo DESC
    LIMIT :limit
    """
  )
  suspend fun findBySongText(songText: String, limit: Int?): List<SongSearchResultEntity>
}