package io.github.alelk.pws.database.bookstatistic

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import io.github.alelk.pws.domain.bookstatistic.query.BookStatisticQuery
import io.github.alelk.pws.domain.core.ids.BookId
import kotlinx.coroutines.flow.Flow

@Dao
interface BookStatisticDao {
  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(bookStatistic: BookStatisticEntity): Long

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(bookStatistics: List<BookStatisticEntity>): List<Long>

  @Upsert
  suspend fun upsert(bookStatistics: List<BookStatisticEntity>): List<Long>

  @Upsert
  suspend fun upsert(bookStatistic: BookStatisticEntity): Long

  @Query("SELECT * FROM book_statistic WHERE id = :id")
  suspend fun getById(id: BookId): BookStatisticEntity?

  @Query("SELECT * FROM book_statistic WHERE id IN (:bookIds)")
  suspend fun getByIds(bookIds: List<BookId>): List<BookStatisticEntity>

  @Deprecated("")
  @Query("SELECT * FROM book_statistic WHERE id = :bookId")
  suspend fun getBookStatisticWithBookById(bookId: BookId): BookStatisticWithBookEntity?

  @Deprecated("")
  @Query("SELECT * FROM book_statistic bs WHERE bs.priority > 0")
  suspend fun getAllActive(): List<BookStatisticWithBookEntity>

  @Query("SELECT count(id) FROM book_statistic")
  suspend fun count(): Int

  @Delete
  suspend fun delete(bookStatistic: BookStatisticEntity)

  @Query("DELETE FROM book_statistic")
  suspend fun deleteAll()

  @Query("SELECT * FROM book_statistic WHERE id = :id")
  fun observeById(id: BookId): Flow<BookStatisticEntity?>

  @Query(
    """
    SELECT * FROM book_statistic 
    WHERE (:minPriority IS NULL OR priority >= :minPriority) AND (:maxPriority IS NULL OR priority <= :maxPriority)
    ORDER BY priority DESC"""
  )
  fun observeAll(minPriority: Int? = null, maxPriority: Int? = null): Flow<List<BookStatisticEntity>>


  @Deprecated("")
  @Transaction
  @Query("SELECT * FROM book_statistic ORDER BY id")
  fun getAllBookStatisticWithBookFlow(): Flow<List<BookStatisticWithBookEntity>>
}