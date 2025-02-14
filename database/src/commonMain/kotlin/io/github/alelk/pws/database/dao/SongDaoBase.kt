package io.github.alelk.pws.database.dao

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.alelk.pws.database.entity.SongEntity
import io.github.alelk.pws.database.entity.SongNumberWithSongWithBookEntity
import io.github.alelk.pws.domain.model.SongId
import kotlinx.coroutines.flow.Flow

interface SongDaoBase : Pageable<SongEntity> {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(song: SongEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
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

}