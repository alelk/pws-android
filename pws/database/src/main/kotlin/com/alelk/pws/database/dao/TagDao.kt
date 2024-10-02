package com.alelk.pws.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alelk.pws.database.entity.TagEntity
import com.alelk.pws.database.model.TagId

@Dao
interface TagDao : Pageable<TagEntity> {
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

  @Query("SELECT * FROM tags ORDER BY priority, id LIMIT :limit OFFSET :offset")
  override suspend fun getAll(limit: Int, offset: Int): List<TagEntity>

  @Query("SELECT count(id) FROM tags")
  suspend fun count(): Int

  @Delete
  suspend fun delete(tag: TagEntity)

  @Delete
  suspend fun delete(tags: List<TagEntity>)

  @Query("DELETE FROM tags")
  suspend fun deleteAll()
}