package com.alelk.pws.database.dao

import androidx.room.*
import com.alelk.pws.database.entity.SongEntity

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
}