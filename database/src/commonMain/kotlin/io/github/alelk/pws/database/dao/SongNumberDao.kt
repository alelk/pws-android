package io.github.alelk.pws.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.github.alelk.pws.database.entity.BookEntity
import io.github.alelk.pws.database.entity.SongNumberEntity
import io.github.alelk.pws.database.entity.SongNumberWithSongEntity
import io.github.alelk.pws.domain.model.BookId
import io.github.alelk.pws.domain.model.SongId
import io.github.alelk.pws.domain.model.SongNumberId
import kotlinx.coroutines.flow.Flow

@Dao
interface SongNumberDao {
  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(number: SongNumberEntity)

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(numbers: List<SongNumberEntity>)

  @Update(onConflict = OnConflictStrategy.REPLACE)
  suspend fun update(number: SongNumberEntity)

  @Query("SELECT * FROM song_numbers WHERE book_id = :bookId AND song_id = :songId")
  suspend fun getById(bookId: BookId, songId: SongId): SongNumberEntity?

  @Query("SELECT * FROM song_numbers WHERE song_id = :songId ORDER BY priority DESC")
  suspend fun getBySongId(songId: SongId): List<SongNumberEntity>

  @Query("SELECT * FROM song_numbers WHERE song_id IN (:songIds)")
  suspend fun getBySongIds(songIds: List<SongId>): List<SongNumberEntity>

  @Query("SELECT * FROM song_numbers WHERE book_id IN (:bookIds)")
  suspend fun getByBookIds(bookIds: List<BookId>): List<SongNumberEntity>

  @Query("SELECT * FROM song_numbers n LEFT OUTER JOIN books b ON n.book_id = b.id WHERE n.song_id in (:songIds)")
  suspend fun findSongNumbersWithBooksBySongIds(songIds: List<SongId>): Map<SongNumberEntity, BookEntity>

  @Query("""SELECT * FROM song_numbers sn INNER JOIN books b ON sn.book_id = b.id WHERE sn.book_id = :bookId AND sn.number IN (:songNumbers) ORDER BY sn.priority DESC""")
  @Transaction
  suspend fun getByBookIdAndSongNumbers(bookId: BookId, songNumbers: List<Int>): Map<SongNumberEntity, BookEntity>

  suspend fun getByBookIdAndSongNumber(bookExternalId: BookId, songNumber: Int): Pair<SongNumberEntity, BookEntity>? =
    getByBookIdAndSongNumbers(bookExternalId, listOf(songNumber)).toList()
      .also { check(it.size <= 1) { "expected single song number by book external id $bookExternalId and number $songNumber" } }
      .firstOrNull()

  @Query("SELECT * FROM song_numbers ORDER BY book_id, song_id LIMIT :limit OFFSET :offset")
  suspend fun getAll(limit: Int, offset: Int = 0): List<SongNumberEntity>

  @Query("SELECT count(*) FROM song_numbers")
  suspend fun count(): Int

  @Delete
  suspend fun delete(number: SongNumberEntity)

  @Delete
  suspend fun delete(numbers: List<SongNumberEntity>)

  @Query("""DELETE FROM song_numbers WHERE book_id = :bookId AND number = :songNumber""")
  suspend fun deleteByBookIdAndSongNumber(bookId: BookId, songNumber: Int)

  @Query("DELETE FROM song_numbers")
  suspend fun deleteAll()

  // flows

  @Transaction
  @Query("SELECT * FROM song_numbers WHERE book_id = :bookId AND song_id = :songId")
  fun getByIdFlow(bookId: BookId, songId: SongId): Flow<SongNumberEntity>

  fun getByIdFrom(songNumberId: SongNumberId) = getByIdFlow(songNumberId.bookId, songNumberId.songId)

  //@Transaction
  //@Query("SELECT * FROM song_numbers WHERE book_id = :bookId AND song_id = :songId")
  //fun getSongOfBookByIdFlow(bookId: BookId, songId: SongId): Flow<SongNumberWithSongWithBookWithFavorite>

  //fun getSongOfBookByIdFlow(songNumberId: SongNumberId) = getSongOfBookByIdFlow(songNumberId.bookId, songNumberId.songId)

  @Transaction
  @Query("SELECT * FROM song_numbers WHERE book_id = :bookId ORDER BY number")
  fun getBookSongsByBookIdFlow(bookId: BookId): Flow<List<SongNumberWithSongEntity>>
}