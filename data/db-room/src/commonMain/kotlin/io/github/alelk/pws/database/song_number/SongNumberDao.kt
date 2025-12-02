package io.github.alelk.pws.database.song_number

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.github.alelk.pws.database.book.BookEntity
import io.github.alelk.pws.database.song_number.SongNumberEntity
import io.github.alelk.pws.database.song_number.SongNumberWithSongEntity
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.core.ids.SongNumberId
import io.github.alelk.pws.domain.core.ids.TagId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest

@Dao
interface SongNumberDao {
  @Insert(onConflict = OnConflictStrategy.Companion.ABORT)
  suspend fun insert(number: SongNumberEntity)

  @Insert(onConflict = OnConflictStrategy.Companion.ABORT)
  suspend fun insert(numbers: List<SongNumberEntity>)

  @Update(onConflict = OnConflictStrategy.Companion.REPLACE)
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

  @Transaction
  @Query("""SELECT * FROM song_numbers WHERE book_id = :bookId AND number IN (:songNumbers) ORDER BY priority DESC""")
  suspend fun getByBookIdAndSongNumbers(bookId: BookId, songNumbers: List<Int>): List<SongNumberEntity>

  @Transaction
  @Query("""SELECT * FROM song_numbers WHERE book_id = :bookId AND number = :songNumber""")
  suspend fun getByBookIdAndSongNumber(bookId: BookId, songNumber: Int): SongNumberEntity?

  @Query("SELECT sn.* FROM song_numbers sn INNER JOIN song_tags st ON sn.song_id = st.song_id WHERE st.tag_id = :tagId ORDER BY st.tag_id, st.priority, sn.number")
  suspend fun getAllByTagId(tagId: TagId): List<SongNumberEntity>

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
  fun getByIdFlow(bookId: BookId, songId: SongId): Flow<SongNumberEntity?>

  fun getByIdFlow(songNumberId: SongNumberId) = getByIdFlow(songNumberId.bookId, songNumberId.songId)

  @Transaction
  @Query("SELECT * FROM song_numbers WHERE book_id IN (:bookIds) AND song_id IN (:songIds)")
  fun getByBookIdsAndSongIdsFlow(bookIds: Set<BookId>, songIds: Set<SongId>): Flow<List<SongNumberEntity>>

  @OptIn(ExperimentalCoroutinesApi::class)
  fun getByIdsFlow(ids: List<SongNumberId>): Flow<List<SongNumberEntity>> =
    getByBookIdsAndSongIdsFlow(ids.map { it.bookId }.toSet(), ids.map { it.songId }.toSet())
      .distinctUntilChanged()
      .mapLatest { allEntities ->
        ids.mapNotNull { id -> allEntities.find { it.bookId == id.bookId && it.songId == id.songId } }
      }

  @Transaction
  @Query("SELECT * FROM song_numbers WHERE book_id = :bookId ORDER BY number")
  fun getByBookIdFlow(bookId: BookId): Flow<List<SongNumberEntity>>

  //@Transaction
  //@Query("SELECT * FROM song_numbers WHERE book_id = :bookId AND song_id = :songId")
  //fun getSongOfBookByIdFlow(bookId: BookId, songId: SongId): Flow<SongNumberWithSongWithBookWithFavorite>

  //fun getSongOfBookByIdFlow(songNumberId: SongNumberId) = getSongOfBookByIdFlow(songNumberId.bookId, songNumberId.songId)

  @Transaction
  @Query("SELECT * FROM song_numbers WHERE book_id = :bookId ORDER BY number")
  fun getBookSongsByBookIdFlow(bookId: BookId): Flow<List<SongNumberWithSongEntity>>
}