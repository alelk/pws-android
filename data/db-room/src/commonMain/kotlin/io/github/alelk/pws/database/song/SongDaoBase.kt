package io.github.alelk.pws.database.song

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.alelk.pws.database.core.Pageable
import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.core.ids.TagId
import kotlinx.coroutines.flow.Flow

interface SongDaoBase : Pageable<SongEntity> {

  @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
  suspend fun insert(song: SongEntity)

  @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
  suspend fun insert(songs: List<SongEntity>)

  @Query("SELECT * FROM songs WHERE id = :id")
  suspend fun getById(id: SongId): SongEntity?

  @Query("SELECT * FROM songs WHERE id in (:ids)")
  suspend fun getByIds(ids: List<SongId>): List<SongEntity>

  @Query("SELECT * FROM songs ORDER BY id LIMIT :limit OFFSET :offset")
  override suspend fun getAll(limit: Int, offset: Int): List<SongEntity>

  @Query("""SELECT sn.* FROM song_numbers sn inner join songs s on sn.song_id = s.id WHERE s.edited > 0""")
  suspend fun getAllEdited(): List<SongNumberWithSongWithBookEntity>

  @Query("SELECT count(id) FROM songs")
  suspend fun count(): Int

  @Update
  suspend fun update(song: SongEntity)

  @Delete
  suspend fun delete(song: SongEntity)

  @Delete
  suspend fun delete(songs: List<SongEntity>)

  @Query("DELETE FROM songs")
  suspend fun deleteAll()

  // flow

  @Query("SELECT * FROM songs WHERE id = :id")
  fun getByIdFlow(id: SongId): Flow<SongEntity?>

  @Query("SELECT * FROM songs WHERE id IN (:ids)")
  fun getByIdsFlow(ids: List<SongId>): Flow<List<SongEntity>>

  @Query("""
    SELECT sn.* FROM song_numbers sn
    INNER JOIN songs s ON sn.song_id = s.id
    INNER JOIN song_tags st ON s.id = st.song_id
    INNER JOIN book_statistic bs ON sn.book_id = bs.id
    WHERE st.tag_id = :tagId AND bs.priority > 0
  """)
  fun getActiveTagSongsFlow(tagId: TagId): Flow<List<SongNumberWithSongWithBookEntity>>
}