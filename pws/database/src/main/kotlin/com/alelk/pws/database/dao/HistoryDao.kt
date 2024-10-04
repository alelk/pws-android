package com.alelk.pws.database.dao

import androidx.room.*
import com.alelk.pws.database.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

data class HistoryItem(val id: Long, val songNumber: Int, val songName: String, val bookDisplayName: String, val songNumberId: Long, val timestamp: Date)

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

  @Query("SELECT * FROM history ORDER by accesstimestamp DESC LIMIT :countItems")
  suspend fun getLast(countItems: Int = 1): List<HistoryEntity>

  @Transaction
  @Query(
    """
    SELECT h._id as id, p.name as songName, pn.number as songNumber, b.displayname as bookDisplayName, pn._id as songNumberId, h.accesstimestamp as timestamp
    FROM history h 
    INNER JOIN psalmnumbers pn on h.psalmnumberid = pn._id 
    INNER JOIN psalms p on pn.psalmid=p._id
    INNER JOIN books b on pn.bookid=b._id
    ORDER BY h.accesstimestamp DESC
    """
  )
  fun getAll(): Flow<List<HistoryItem>>

  @Query("DELETE FROM history")
  suspend fun deleteAll()
}