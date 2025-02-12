package io.github.alelk.pws.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.github.alelk.pws.database.entity.BookStatisticEntity
import io.github.alelk.pws.database.entity.BookStatisticWithBookEntity
import io.github.alelk.pws.domain.model.BookId
import kotlinx.coroutines.flow.Flow

@Dao
interface BookStatisticDao {
  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(bookStatistic: BookStatisticEntity): Long

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(bookStatistics: List<BookStatisticEntity>): List<Long>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun update(bookStatistics: List<BookStatisticEntity>): List<Long>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun update(bookStatistic: BookStatisticEntity): Long

  @Query("SELECT * FROM bookstatistic WHERE id = :id")
  suspend fun getById(id: BookId): BookStatisticEntity?

  @Query("SELECT * FROM bookstatistic WHERE id IN (:bookIds)")
  suspend fun getByIds(bookIds: List<BookId>): List<BookStatisticEntity>

  @Query("SELECT * FROM bookstatistic WHERE id = :bookId")
  suspend fun getBookStatisticWithBookById(bookId: BookId): BookStatisticWithBookEntity?

  @Query("SELECT * FROM bookstatistic bs WHERE bs.priority > 0")
  suspend fun getAllActive():List<BookStatisticWithBookEntity>

  @Query("SELECT count(id) FROM bookstatistic")
  suspend fun count(): Int

  @Delete
  suspend fun delete(bookStatistic: BookStatisticEntity)

  @Query("DELETE FROM bookstatistic")
  suspend fun deleteAll()

  // flows

  @Transaction
  @Query("SELECT * FROM bookstatistic ORDER BY id")
  fun getAllBookStatisticWithBookFlow(): Flow<List<BookStatisticWithBookEntity>>
}