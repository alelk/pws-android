package io.github.alelk.pws.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.github.alelk.pws.database.entity.BookEntity
import io.github.alelk.pws.database.entity.BookStatisticEntity
import io.github.alelk.pws.database.entity.BookStatisticWithBookEntity
import io.github.alelk.pws.domain.model.BookExternalId
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

  @Query("SELECT * FROM bookstatistic WHERE _id = :id")
  suspend fun getById(id: Long): BookStatisticEntity?

  @Query("SELECT * FROM bookstatistic WHERE bookid = :bookId")
  suspend fun getByBookId(bookId: Long): BookStatisticEntity?

  @Query("SELECT * FROM bookstatistic WHERE bookid IN (:bookIds)")
  suspend fun getByBookIds(bookIds: List<Long>): List<BookStatisticEntity>

  @Query("SELECT * FROM bookstatistic bs JOIN books b ON bs.bookid=b._id WHERE b.edition IN (:bookExternalIds)")
  suspend fun getByBookExternalIds(bookExternalIds: List<BookExternalId>): Map<BookStatisticEntity, BookEntity>

  @Query("SELECT * FROM bookstatistic bs INNER JOIN books b on bs.bookid = b._id WHERE b.edition = :bookExternalId")
  suspend fun getBookStatisticWithBookByBookExternalId(bookExternalId: BookExternalId): BookStatisticWithBookEntity?

  @Query("SELECT * FROM bookstatistic bs INNER JOIN books b on bs.bookid = b._id WHERE bs.userpref > 0")
  suspend fun getAllActive():List<BookStatisticWithBookEntity>

  @Query("SELECT count(_id) FROM bookstatistic")
  suspend fun count(): Int

  @Delete
  suspend fun delete(bookStatistic: BookStatisticEntity)

  @Query("DELETE FROM bookstatistic")
  suspend fun deleteAll()

  // flows

  @Transaction
  @Query("SELECT * FROM bookstatistic ORDER BY _id")
  fun getAllBookStatisticWithBookFlow(): Flow<List<BookStatisticWithBookEntity>>
}