package com.alelk.pws.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alelk.pws.database.entity.TagEntity
import com.alelk.pws.database.model.TagId
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(tag: TagEntity)

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(tags: List<TagEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
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

  @Query("SELECT COUNT(*) FROM tags WHERE name = :name")
  suspend fun isTagNameExists(name: String): Boolean

  @Query("SELECT count(id) FROM tags")
  suspend fun count(): Int

  @Delete
  suspend fun delete(tag: TagEntity)

  @Delete
  suspend fun delete(tags: List<TagEntity>)

  @Query("DELETE FROM tags")
  suspend fun deleteAll()
}