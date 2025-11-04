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
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.ids.SongId
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao : Pageable<BookEntity> {
  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(book: BookEntity)

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(books: List<BookEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun update(book: BookEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun update(books: List<BookEntity>)

  @Query("SELECT * FROM books WHERE id = :id")
  suspend fun getById(id: BookId): BookEntity?

  @Query("SELECT * FROM books WHERE id in (:ids)")
  suspend fun getByIds(ids: List<BookId>): List<BookEntity>

  @Query("SELECT * FROM books ORDER BY id LIMIT :limit OFFSET :offset")
  override suspend fun getAll(limit: Int, offset: Int): List<BookEntity>

  @Query("SELECT count(id) FROM books")
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
    SELECT sn.* 
    FROM books b INNER JOIN song_numbers sn ON sn.book_id = b.id 
    WHERE b.id IN (SELECT book_id FROM song_numbers WHERE song_id = :songId AND book_id = :bookId) 
    ORDER BY sn.number
    """
  )
  fun getBookSongNumbersBySongNumberIdFlow(songId: SongId, bookId: BookId): Flow<List<SongNumberEntity>>

  @Query("SELECT * FROM books WHERE id = :bookId")
  fun getByIdFlow(bookId: BookId): Flow<BookEntity?>

  @Query("SELECT * FROM books WHERE id in (:bookIds)")
  fun getByIdsFlow(bookIds: List<BookId>): Flow<List<BookEntity>>

  @Transaction
  @Query("""SELECT b.* FROM books b INNER JOIN book_statistic bs on bs.id=b.id WHERE bs.priority > 0""")
  fun getAllActiveFlow(): Flow<List<BookWithSongNumbersEntity>>
}
