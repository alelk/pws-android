package io.github.alelk.pws.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.github.alelk.pws.database.entity.SongTagEntity
import io.github.alelk.pws.domain.model.SongId
import io.github.alelk.pws.domain.model.TagId
import kotlinx.coroutines.flow.Flow

@Dao
interface SongTagDao : Pageable<SongTagEntity> {
  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(songNumberTag: SongTagEntity)

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(songNumberTags: List<SongTagEntity>)

  @Query("SELECT * FROM song_tags WHERE song_id = :songId and tag_id = :tagId")
  suspend fun getById(songId: SongId, tagId: TagId): SongTagEntity?

  @Query("SELECT * FROM song_tags WHERE song_id = :songId")
  suspend fun getBySongId(songId: SongId): List<SongTagEntity>

  @Query("SELECT * FROM song_tags ORDER BY tag_id, song_id LIMIT :limit OFFSET :offset")
  override suspend fun getAll(limit: Int, offset: Int): List<SongTagEntity>

  @Query("SELECT count(*) FROM song_tags")
  suspend fun count(): Int

  @Delete
  suspend fun delete(tag: SongTagEntity)

  @Delete
  suspend fun delete(tags: List<SongTagEntity>)

  @Query("DELETE FROM song_tags")
  suspend fun deleteAll()

}