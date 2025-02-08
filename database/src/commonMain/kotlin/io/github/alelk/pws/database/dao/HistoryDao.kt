package io.github.alelk.pws.database.dao

import androidx.room.*
import io.github.alelk.pws.database.entity.HistoryEntity
import io.github.alelk.pws.database.entity.HistoryWithSongNumberWithSongWithBook
import kotlinx.coroutines.flow.Flow

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
  @Query("""SELECT pn.* FROM history h INNER JOIN psalmnumbers pn on h.psalmnumberid = pn._id ORDER BY h.accesstimestamp DESC""")
  fun getAllFlow(): Flow<List<HistoryWithSongNumberWithSongWithBook>>

  @Query("DELETE FROM history")
  suspend fun deleteAll()
}