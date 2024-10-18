package com.alelk.pws.database.dao

import android.app.SearchManager
import android.database.Cursor
import android.provider.BaseColumns
import androidx.room.*
import io.github.alelk.pws.database.common.entity.SongEntity

data class SongSearchResult(
  val songNumberId: Long,
  val songName: String,
  val songNumber: Int,
  val bookDisplayName: String,
  val snippet: String
)

@Dao
interface SongDao : Pageable<SongEntity> {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(song: SongEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(songs: List<SongEntity>): List<Long>

  @Query("SELECT * FROM psalms WHERE _id = :id")
  suspend fun getById(id: Long): SongEntity?

  @Query("SELECT * FROM psalms WHERE _id in (:ids)")
  suspend fun getByIds(ids: List<Long>): List<SongEntity>

  @Query("SELECT * FROM psalms ORDER BY _id LIMIT :limit OFFSET :offset")
  override suspend fun getAll(limit: Int, offset: Int): List<SongEntity>

  @Query("SELECT count(_id) FROM psalms")
  suspend fun count(): Int

  @Update
  suspend fun update(song: SongEntity)

  @Delete
  suspend fun delete(song: SongEntity)

  @Delete
  suspend fun delete(songs: List<SongEntity>)

  @Query("DELETE FROM psalms")
  suspend fun deleteAll()

  @Query(
    """
    SELECT 
      pn._id AS ${SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID},
      p.name AS ${SearchManager.SUGGEST_COLUMN_TEXT_1},
      group_concat(b.displayshortname, ' | ') AS ${SearchManager.SUGGEST_COLUMN_TEXT_2},
      pn._id AS ${BaseColumns._ID}
    FROM books AS b
    INNER JOIN (SELECT bookid FROM bookstatistic WHERE userpref > 0 ORDER BY userpref) AS bs 
    ON b._id = bs.bookid
    INNER JOIN psalmnumbers AS pn
    ON pn.bookid = b._id
    INNER JOIN songs_fts AS p
    ON p.docid = pn.psalmid
    WHERE songs_fts MATCH :searchText
    GROUP BY p.docid
    LIMIT :limit
    """
  )
  fun getSuggestionsBySongLyric(searchText: String, limit: Int?): Cursor

  @Query(
    """
    SELECT 
      pn._id AS ${BaseColumns._ID},
      pn.number AS psalmnumber,
      group_concat(b.displayshortname, ', ') AS ${SearchManager.SUGGEST_COLUMN_TEXT_2},
      p.name AS ${SearchManager.SUGGEST_COLUMN_TEXT_1},
      p.rowid AS psalmid,
      pn._id AS ${SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID}
    FROM books AS b
    INNER JOIN psalmnumbers AS pn ON pn.bookid = b._id
    INNER JOIN songs_fts AS p ON p.rowid = pn.psalmid
    INNER JOIN (SELECT bookid, userpref FROM bookstatistic WHERE userpref > 0 ORDER BY userpref DESC) AS bs ON b._id = bs.bookid
    WHERE number = :songNumber
    GROUP BY pn.number, p.rowid
    ORDER BY bs.userpref DESC
    LIMIT :limit
    """
  )
  fun getSuggestionsBySongNumber(songNumber: Int, limit: Int? = 50): Cursor

  @Query(
    """
    SELECT 
      pn._id AS ${SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID},
      p.name AS ${SearchManager.SUGGEST_COLUMN_TEXT_1},
      group_concat(b.displayshortname, ' | ') AS ${SearchManager.SUGGEST_COLUMN_TEXT_2},
      pn._id AS ${BaseColumns._ID}
    FROM books AS b
    INNER JOIN psalmnumbers AS pn ON pn.bookid = b._id
    INNER JOIN songs_fts AS p ON p.docid = pn.psalmid
    WHERE p.name MATCH :searchName
    GROUP BY p.docid
    LIMIT :limit
    """
  )
  fun getSuggestionsBySongName(searchName: String, limit: Int?): Cursor

  @Query(
    """
    SELECT 
      pn._id AS ${BaseColumns._ID}, 
      pn._id AS songNumberId,
      p.name AS songName,
      pn.number AS songNumber,
      b.displayname AS bookDisplayName,
      substr(p.text, 1, 100) AS snippet
    FROM books AS b
    INNER JOIN (SELECT bookid, userpref FROM bookstatistic WHERE userpref > 0 ORDER BY userpref DESC) AS bs ON b._id = bs.bookid
    INNER JOIN psalmnumbers AS pn ON pn.bookid = b._id
    INNER JOIN psalms AS p ON p._id = pn.psalmid
    WHERE pn.number = :psalmNumber
    ORDER BY bs.userpref DESC
    LIMIT :limit
    """
  )
  suspend fun findBySongNumber(psalmNumber: Int, limit: Int?): List<SongSearchResult>

  @Query(
    """
    SELECT 
      pn._id AS ${BaseColumns._ID}, 
      pn._id AS songNumberId,
      p.name AS songName,
      pn.number AS songNumber,
      b.displayname AS bookDisplayName,
      snippet(songs_fts, '<b><font color=#247b34>', '</font></b>', '...') as snippet,
      matchinfo(songs_fts, 'x') as matchinfo
    FROM books AS b
    INNER JOIN (SELECT bookid, userpref FROM bookstatistic WHERE userpref > 0 ORDER BY userpref DESC) AS bs ON b._id = bs.bookid
    INNER JOIN psalmnumbers AS pn ON pn.bookid = b._id
    INNER JOIN songs_fts AS p ON p.docid = pn.psalmid
    WHERE songs_fts MATCH :songText
    ORDER BY matchinfo DESC
    LIMIT :limit
    """
  )
  suspend fun findBySongText(songText: String, limit: Int?): List<SongSearchResult>
}