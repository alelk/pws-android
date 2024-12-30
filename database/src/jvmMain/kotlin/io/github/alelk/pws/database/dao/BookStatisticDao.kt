package io.github.alelk.pws.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.alelk.pws.database.common.entity.BookEntity
import io.github.alelk.pws.database.common.entity.BookStatisticEntity
import io.github.alelk.pws.domain.model.BookExternalId

@Dao
interface BookStatisticDao {
  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(bookStatistic: BookStatisticEntity): Long

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(bookStatistics: List<BookStatisticEntity>): List<Long>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun update(bookStatistics: List<BookStatisticEntity>): List<Long>

  @Query("SELECT * FROM bookstatistic WHERE _id = :id")
  suspend fun getById(id: Long): BookStatisticEntity?

  @Query("SELECT * FROM bookstatistic WHERE bookid = :bookId")
  suspend fun getByBookId(bookId: Long): BookStatisticEntity?

  @Query("SELECT * FROM bookstatistic WHERE bookid IN (:bookIds)")
  suspend fun getByBookIds(bookIds: List<Long>): List<BookStatisticEntity>

  @Query("SELECT * FROM bookstatistic bs JOIN books b ON bs.bookid=b._id WHERE b.edition IN (:bookExternalIds)")
  suspend fun getByBookExternalIds(bookExternalIds: List<BookExternalId>): Map<BookStatisticEntity, BookEntity>

  @Query("SELECT count(_id) FROM bookstatistic")
  suspend fun count(): Int

  @Delete
  suspend fun delete(bookStatistic: BookStatisticEntity)

  @Query("DELETE FROM bookstatistic")
  suspend fun deleteAll()
}