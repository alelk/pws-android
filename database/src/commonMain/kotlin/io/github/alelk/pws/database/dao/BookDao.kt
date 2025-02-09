package io.github.alelk.pws.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.github.alelk.pws.database.entity.BookEntity
import io.github.alelk.pws.database.entity.BookWithSongNumbersEntity
import io.github.alelk.pws.database.entity.SongNumberEntity
import io.github.alelk.pws.domain.model.BookExternalId
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao : Pageable1<BookEntity> {
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

  @Query("SELECT count(_id) FROM books")
  suspend fun count(): Int

  @Delete
  suspend fun delete(book: BookEntity)

  @Delete
  suspend fun delete(books: List<BookEntity>)

  @Query("DELETE FROM books")
  suspend fun deleteAll()

  // flows

  @Query(
    """
    SELECT pn.* 
    FROM books b INNER JOIN psalmnumbers pn ON pn.bookid = b._id 
    WHERE b._id IN (SELECT bookid FROM psalmnumbers WHERE _id = :songNumberId) 
    ORDER BY pn.number
    """
  )
  fun getBookSongNumbersBySongNumberIdFlow(songNumberId: Long): Flow<List<SongNumberEntity>>

  @Query("SELECT * FROM books WHERE edition = :externalId")
  fun getByExternalIdFlow(externalId: BookExternalId): Flow<BookEntity?>

  @Transaction
  @Query("""SELECT b.* FROM books b INNER JOIN bookstatistic bs on bs.bookid=b._id WHERE bs.userpref > 0""")
  fun getAllActiveFlow(): Flow<List<BookWithSongNumbersEntity>>
}
