package com.alelk.pws.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.alelk.pws.database.entity.BookEntity
import com.alelk.pws.database.entity.FavoriteEntity
import com.alelk.pws.database.entity.SongEntity
import com.alelk.pws.database.entity.SongNumberEntity
import com.alelk.pws.database.model.BookExternalId
import kotlinx.coroutines.flow.Flow

data class SongNumberWithSongWithBookWithFavorites(
  @Embedded
  val songNumber: SongNumberEntity,
  @Relation(parentColumn = "psalmid", entityColumn = "_id")
  val song: SongEntity,
  @Relation(parentColumn = "bookid", entityColumn = "_id")
  val book: BookEntity,
  @Relation(parentColumn = "_id", entityColumn = "psalmnumberid")
  val favorite: List<FavoriteEntity>
) {
  val bookId: Long get() = checkNotNull(book.id) { "book id cannot be null" }
  val songNumberId: Long get() = checkNotNull(songNumber.id) { "song number id cannot be null" }
}

data class SongNumberWithBook(
  @Embedded
  val songNumber: SongNumberEntity,
  @Relation(
    parentColumn = "bookid",
    entityColumn = "_id"
  )
  val book: BookEntity
)

@Dao
interface SongNumberDao {
  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(number: SongNumberEntity): Long

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(numbers: List<SongNumberEntity>): List<Long>

  @Update(onConflict = OnConflictStrategy.REPLACE)
  suspend fun update(number: SongNumberEntity): Int

  @Transaction
  @Query("SELECT * FROM psalmnumbers WHERE _id = :id")
  fun getSongOfBookById(id: Long): Flow<SongNumberWithSongWithBookWithFavorites>

  @Transaction
  @Query("SELECT * FROM psalmnumbers WHERE _id = :id")
  fun getById(id: Long): Flow<SongNumberEntity>

  @Query("SELECT * FROM psalmnumbers WHERE _id IN (:ids)")
  suspend fun getByIds(ids: List<Long>): List<SongNumberEntity>

  @Query("SELECT * FROM psalmnumbers WHERE psalmid = :songId ORDER BY priority DESC")
  suspend fun getBySongId(songId: Long): List<SongNumberEntity>

  @Query("SELECT * FROM psalmnumbers WHERE psalmid IN (:songIds)")
  suspend fun getBySongIds(songIds: List<Long>): List<SongNumberEntity>

  @Query("SELECT * FROM psalmnumbers WHERE bookid IN (:bookIds)")
  suspend fun getByBookIds(bookIds: List<Long>): List<SongNumberEntity>

  @Query("SELECT * FROM psalmnumbers n LEFT OUTER JOIN books b ON n.bookid = b._id WHERE n.psalmid in (:songIds)")
  suspend fun findSongNumbersWithBooksBySongIds(songIds: List<Long>): Map<SongNumberEntity, BookEntity>

  @Query(
    """
    select * from psalmnumbers n 
    join books b on n.bookid = b._id
    where b.edition = :bookExternalId and n.number in (:songNumbers)
    order by n.priority DESC
    """
  )
  @Transaction
  suspend fun getByBookExternalIdAndSongNumbers(bookExternalId: BookExternalId, songNumbers: List<Int>): Map<SongNumberEntity, BookEntity>

  suspend fun getByBookExternalIdAndSongNumber(bookExternalId: BookExternalId, songNumber: Int): Pair<SongNumberEntity, BookEntity>? =
    getByBookExternalIdAndSongNumbers(bookExternalId, listOf(songNumber)).toList()
      .also { check(it.size <= 1) { "expected single song number by book external id $bookExternalId and number $songNumber" } }
      .firstOrNull()

  @Query("SELECT * FROM psalmnumbers ORDER BY _id LIMIT :limit OFFSET :offset")
  suspend fun getAll(limit: Int, offset: Int = 0): List<SongNumberEntity>

  @Query("SELECT count(_id) FROM psalmnumbers")
  suspend fun count(): Int

  @Delete
  suspend fun delete(number: SongNumberEntity)

  @Delete
  suspend fun delete(numbers: List<SongNumberEntity>)

  @Query(
    """
    delete from psalmnumbers where bookid in ( 
      select b._id from psalmnumbers n
      inner join books b on n.bookid = b._id
      where n.number = :songNumber and b.edition = :bookExternalId
    )
    """
  )
  suspend fun deleteByBookExternalIdAndSongNumber(bookExternalId: BookExternalId, songNumber: Int)

  @Query("DELETE FROM psalmnumbers")
  suspend fun deleteAll()
}