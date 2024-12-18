package com.alelk.pws.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.alelk.pws.database.common.entity.TagEntity
import io.github.alelk.pws.database.common.model.TagId
import kotlinx.coroutines.flow.Flow

data class SongInfo(
  val songNumberId: Long,
  val songNumber: Int,
  val bookDisplayName: String,
  val songId: Long,
  val songName: String
)

@Dao
interface TagDao {
  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(tag: TagEntity)

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(tags: List<TagEntity>)

  @Update(onConflict = OnConflictStrategy.REPLACE)
  suspend fun update(tag: TagEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun update(tags: List<TagEntity>)

  @Query("SELECT * FROM tags WHERE id = :id")
  suspend fun getById(id: TagId): TagEntity?

  @Query("SELECT * FROM tags WHERE id in (:ids)")
  suspend fun getByIds(ids: List<TagId>): List<TagEntity>

  @Query("SELECT * FROM tags ORDER BY priority, id")
  fun getAll(): Flow<List<TagEntity>>

  @Query("SELECT * FROM tags WHERE name = :name ORDER BY priority, id")
  suspend fun getAllByName(name: String): List<TagEntity>

  @Query("SELECT * FROM tags WHERE predefined = 0 ORDER BY priority, id")
  suspend fun getAllNotPredefined(): List<TagEntity>

  @Query(
    """
    SELECT snt.song_number_id as songNumberId, pn.number as songNumber, b.displayname as bookDisplayName, p._id as songId, p.name as songName
    FROM song_number_tags  snt
    INNER JOIN psalmnumbers pn on snt.song_number_id = pn._id
    INNER JOIN books b on pn.bookid = b._id
    INNER JOIN psalms p on pn.psalmid = p._id
    WHERE snt.tag_id = :tagId
    """
  )
  fun getTagSongs(tagId: TagId): Flow<List<SongInfo>>

  @Query("SELECT count(id) FROM tags")
  suspend fun count(): Int

  @Delete
  suspend fun delete(tag: TagEntity)

  @Delete
  suspend fun delete(tags: List<TagEntity>)

  @Query("DELETE FROM tags")
  suspend fun deleteAll()
}