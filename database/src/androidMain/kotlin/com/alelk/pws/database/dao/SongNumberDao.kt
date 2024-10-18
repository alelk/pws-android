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
import io.github.alelk.pws.database.common.entity.BookEntity
import io.github.alelk.pws.database.common.entity.FavoriteEntity
import io.github.alelk.pws.database.common.entity.SongEntity
import io.github.alelk.pws.database.common.entity.SongNumberEntity
import io.github.alelk.pws.database.common.model.BookExternalId
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

data class SongNumberWithSong(
  @Embedded
  val songNumber: SongNumberEntity,
  @Relation(parentColumn = "psalmid", entityColumn = "_id")
  val song: SongEntity,
)

@Dao
interface SongNumberDao {
  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(number: SongNumberEntity): Long

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(numbers: List<SongNumberEntity>): List<Long>

  @Transaction
  @Query("SELECT * FROM psalmnumbers WHERE _id = :id")
  fun getSongOfBookById(id: Long): Flow<SongNumberWithSongWithBookWithFavorites>

  @Transaction
  @Query("SELECT pn.* FROM psalmnumbers pn INNER JOIN books b ON pn.bookid = b._id WHERE b.edition = :bookExternalId ORDER BY pn.number")
  fun getBookSongsByBookId(bookExternalId: BookExternalId): Flow<List<SongNumberWithSong>>

  @Transaction
  @Query("SELECT * FROM psalmnumbers WHERE _id = :id")
  fun getById(id: Long): Flow<SongNumberEntity>

  @Query("SELECT * FROM psalmnumbers WHERE _id IN (:ids)")
  suspend fun getByIds(ids: List<Long>): List<SongNumberEntity>

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

  @Query("SELECT count(_id) FROM psalmnumbers")
  suspend fun count(): Int

  @Delete
  suspend fun delete(number: SongNumberEntity)

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