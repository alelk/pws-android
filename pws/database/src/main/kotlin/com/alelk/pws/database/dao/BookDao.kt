package com.alelk.pws.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.alelk.pws.database.entity.BookEntity
import com.alelk.pws.database.model.BookExternalId
import kotlinx.coroutines.flow.Flow

data class Book(val id: Long, val externalId: BookExternalId, val name: String, val displayName: String, val firstSongNumberId: Long)

@Dao
interface BookDao : Pageable<BookEntity> {
  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(book: BookEntity): Long

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(books: List<BookEntity>): List<Long>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun update(book: BookEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun update(books: List<BookEntity>): List<Long>

  @Query("SELECT * FROM books WHERE _id = :id")
  suspend fun getById(id: Long): BookEntity?

  @Query("SELECT * FROM books WHERE _id in (:ids)")
  suspend fun getByIds(ids: List<Long>): List<BookEntity>

  @Query("SELECT * FROM books ORDER BY _id LIMIT :limit OFFSET :offset")
  override suspend fun getAll(limit: Int, offset: Int): List<BookEntity>

  @Query("SELECT * FROM books WHERE edition = :externalId")
  suspend fun getByExternalId(externalId: BookExternalId): BookEntity?

  @Query("SELECT * FROM books WHERE edition in (:externalIds)")
  suspend fun getByExternalIds(externalIds: List<BookExternalId>): List<BookEntity>

  @Transaction
  @Query(
    """
    SELECT b._id as id, b.edition as externalId, b.name as name, b.displayname as displayName, pn._id as firstSongNumberId
    FROM books b 
    INNER JOIN bookstatistic bs on bs.bookid=b._id
    INNER JOIN psalmnumbers pn on pn.bookid = b._id
    WHERE bs.userpref > 0 AND pn.number = 1
    """
  )
  fun getAllActive(): Flow<List<Book>>

  @Query("SELECT count(_id) FROM books")
  suspend fun count(): Int

  @Delete
  suspend fun delete(book: BookEntity)

  @Delete
  suspend fun delete(books: List<BookEntity>)

  @Query("DELETE FROM books")
  suspend fun deleteAll()
}
