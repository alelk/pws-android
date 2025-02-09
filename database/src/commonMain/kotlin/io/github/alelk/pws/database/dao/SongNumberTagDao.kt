package io.github.alelk.pws.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.github.alelk.pws.database.entity.SongNumberEntity
import io.github.alelk.pws.database.entity.SongNumberTagEntity
import io.github.alelk.pws.database.entity.TagEntity
import io.github.alelk.pws.domain.model.BookExternalId
import io.github.alelk.pws.domain.model.TagId
import kotlinx.coroutines.flow.Flow

@Dao
interface SongNumberTagDao : Pageable1<SongNumberTagEntity> {
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
    WHERE b.edition = :bookExternalId
    """
  )
  suspend fun getByBookExternalId(bookExternalId: BookExternalId): Map<SongNumberTagEntity, SongNumberEntity>

  @Query(
    """
    SELECT snt.*, pn.* FROM song_number_tags snt 
    INNER JOIN psalmnumbers pn ON pn._id = snt.song_number_id
    INNER JOIN books b on b._id = pn.bookid
    WHERE b.edition = :bookExternalId AND snt.tag_id = :tagId
    """
  )
  suspend fun getByBookExternalIdAndTagId(bookExternalId: BookExternalId, tagId: TagId): Map<SongNumberTagEntity, SongNumberEntity>

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

  // flows

  @Transaction
  @Query("SELECT t.* FROM tags t INNER JOIN song_number_tags snt ON t.id = snt.tag_id WHERE snt.song_number_id = :songNumberId ORDER BY t.predefined, t.priority")
  fun getTagsBySongNumberIdFlow(songNumberId: Long): Flow<List<TagEntity>>
}