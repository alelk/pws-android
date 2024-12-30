package io.github.alelk.pws.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.alelk.pws.database.common.entity.SongNumberEntity
import io.github.alelk.pws.database.common.entity.SongNumberTagEntity
import io.github.alelk.pws.domain.model.BookExternalId
import io.github.alelk.pws.domain.model.TagId

@Dao
interface SongNumberTagDao : Pageable<SongNumberTagEntity> {
  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(songNumberTag: SongNumberTagEntity)

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(songNumberTags: List<SongNumberTagEntity>)

  @Query("SELECT * FROM song_number_tags WHERE song_number_id = :songNumberId and tag_id = :tagId")
  suspend fun getById(songNumberId: Long, tagId: TagId): SongNumberTagEntity?

  @Query("SELECT * FROM song_number_tags WHERE song_number_id = :songNumberId")
  suspend fun getBySongNumberId(songNumberId: Long): List<SongNumberTagEntity>

  @Query(
    """
    SELECT snt.*, pn.* FROM song_number_tags snt 
    INNER JOIN psalmnumbers pn ON pn._id = snt.song_number_id
    INNER JOIN books b on b._id = pn.bookid
    WHERE b.edition = :bookExternalId AND snt.tag_id = :tagId
    """
  )
  suspend fun getByBookExternalIdAndTagId(bookExternalId: BookExternalId, tagId: TagId): Map<SongNumberTagEntity, SongNumberEntity>

  @Query(
    """
    SELECT snt.*, pn.* FROM song_number_tags snt 
    INNER JOIN psalmnumbers pn ON pn._id = snt.song_number_id
    INNER JOIN books b on b._id = pn.bookid
    WHERE b.edition = :bookExternalId
    """
  )
  suspend fun getByBookExternalId(bookExternalId: BookExternalId): Map<SongNumberTagEntity, SongNumberEntity>

  @Query("SELECT * FROM song_number_tags ORDER BY tag_id, song_number_id LIMIT :limit OFFSET :offset")
  override suspend fun getAll(limit: Int, offset: Int): List<SongNumberTagEntity>

  @Query("SELECT count(*) FROM song_number_tags")
  suspend fun count(): Int

  @Delete
  suspend fun delete(tag: SongNumberTagEntity)

  @Delete
  suspend fun delete(tags: List<SongNumberTagEntity>)

  @Query("DELETE FROM song_number_tags")
  suspend fun deleteAll()
}