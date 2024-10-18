package io.github.alelk.pws.database.dao

import androidx.room.*
import io.github.alelk.pws.database.common.entity.HistoryEntity

@Dao
interface HistoryDao {
  @Insert
  suspend fun insert(history: HistoryEntity): Long

  @Update
  suspend fun update(history: HistoryEntity)

  @Delete
  suspend fun delete(history: HistoryEntity)

  @Query("SELECT * FROM history WHERE _id = :id")
  suspend fun getById(id: Long): HistoryEntity?

  @Query("DELETE FROM history")
  suspend fun deleteAll()
}