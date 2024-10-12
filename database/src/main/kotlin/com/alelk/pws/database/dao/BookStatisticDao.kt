package com.alelk.pws.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.alelk.pws.database.entity.BookEntity
import com.alelk.pws.database.entity.BookStatisticEntity
import com.alelk.pws.database.model.BookExternalId
import kotlinx.coroutines.flow.Flow

data class BookStatisticWithBook(
  @Embedded
  val bookStatistic: BookStatisticEntity,
  @Relation(parentColumn = "bookid", entityColumn = "_id")
  val book: BookEntity,
)

@Dao
interface BookStatisticDao {
  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(bookStatistic: BookStatisticEntity): Long

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(bookStatistics: List<BookStatisticEntity>): List<Long>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun update(bookStatistic: BookStatisticEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun update(bookStatistics: List<BookStatisticEntity>): List<Long>

  @Transaction
  @Query("SELECT * FROM bookstatistic ORDER BY _id")
  fun getAll(): Flow<List<BookStatisticWithBook>>

  @Query("SELECT * FROM bookstatistic WHERE _id = :id")
  suspend fun getById(id: Long): BookStatisticEntity?

  @Query("SELECT * FROM bookstatistic WHERE bookid = :bookId")
  suspend fun getByBookId(bookId: Long): BookStatisticEntity?

  @Query("SELECT * FROM bookstatistic WHERE bookid IN (:bookIds)")
  suspend fun getByBookIds(bookIds: List<Long>): List<BookStatisticEntity>

  @Query("SELECT * FROM bookstatistic bs INNER JOIN books b on bs.bookid = b._id WHERE b.edition = :bookExternalId")
  suspend fun getByBookExternalId(bookExternalId: BookExternalId): BookStatisticWithBook?

  @Query("SELECT count(_id) FROM bookstatistic")
  suspend fun count(): Int

  @Delete
  suspend fun delete(bookStatistic: BookStatisticEntity)

  @Query("DELETE FROM bookstatistic")
  suspend fun deleteAll()
}