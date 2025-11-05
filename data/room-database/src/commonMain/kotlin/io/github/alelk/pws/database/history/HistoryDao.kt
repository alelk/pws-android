package io.github.alelk.pws.database.history

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.core.ids.SongNumberId
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
  @Insert
  suspend fun insert(history: HistoryEntity): Long

  @Update
  suspend fun update(history: HistoryEntity)

  @Delete
  suspend fun delete(history: HistoryEntity)

  @Query("SELECT * FROM history WHERE id = :id")
  suspend fun getById(id: Long): HistoryEntity

  @Query("SELECT * FROM history WHERE book_id = :bookId AND song_id = :songId")
  suspend fun getById(bookId: BookId, songId: SongId): HistoryEntity?

  suspend fun getBySongNumberId(songNumberId: SongNumberId) = getById(songNumberId.bookId, songNumberId.songId)

  @Query("SELECT * FROM history ORDER by access_timestamp DESC LIMIT :countItems")
  suspend fun getLast(countItems: Int = 1): List<HistoryEntity>

  @Query("SELECT count(*) FROM history")
  suspend fun count(): Int

  @Transaction
  @Query("""SELECT * FROM history ORDER BY access_timestamp DESC""")
  fun getAllFlow(): Flow<List<HistoryEntity>>

  @Query("DELETE FROM history")
  suspend fun deleteAll()
}