package io.github.alelk.pws.database.book

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.alelk.pws.database.core.Pageable
import io.github.alelk.pws.database.song_number.SongNumberEntity
import io.github.alelk.pws.domain.core.Locale
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.ids.SongId
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao : Pageable<BookEntity> {

  @Query(
    """
      SELECT 
        b.id as id, b.version as version, b.locale as locale, b.name as name, b.display_short_name as display_short_name, b.display_name as display_name,
        b.release_date as release_date, b.authors as authors, b.creators as creators, b.reviewers as reviewers, b.editors as editors,
        b.description as description, b.preface as preface,
        count(sn.number) as count_songs, (sn.book_id || '/' || first(sn.song_id)), bs.priority as priority
      FROM books b
      INNER JOIN book_statistic bs on bs.id=b.id
      INNER JOIN song_numbers sn on sn.book_id=b.id
      WHERE b.id = :bookId
      GROUP BY b.id
      ORDER BY sn.number
    """
  )
  fun observeBookDetail(bookId: BookId): Flow<BookDetailProjection?>

  @Query(
    """
      SELECT 
        b.id as id, b.version as version, b.locale as locale, b.name as name, b.display_short_name as display_short_name, b.display_name as display_name,
        count(sn.number) as count_songs, (sn.book_id || '/' || first(sn.song_id)), bs.priority as priority
      FROM books b
      INNER JOIN book_statistic bs on bs.id=b.id
      INNER JOIN song_numbers sn on sn.book_id=b.id
      WHERE 
        (:locale IS NULL OR b.locale = :locale) 
        AND (:minPriority IS NULL OR bs.priority >= :minPriority) 
        AND (:maxPriority IS NULL OR bs.priority <= :maxPriority)
      GROUP BY b.id
      ORDER BY bs.priority DESC, sn.number
    """
  )
  fun observeBooksSummary(locale: Locale? = null, minPriority: Int? = null, maxPriority: Int? = null): Flow<List<BookSummaryProjection>>

  @Query(
    """
      SELECT 
        b.id as id, b.version as version, b.locale as locale, b.name as name, b.display_short_name as display_short_name, b.display_name as display_name,
        b.release_date as release_date, b.authors as authors, b.creators as creators, b.reviewers as reviewers, b.editors as editors,
        b.description as description, b.preface as preface,
        count(sn.number) as count_songs, (sn.book_id || '/' || first(sn.song_id)), bs.priority as priority
      FROM books b
      INNER JOIN book_statistic bs on bs.id=b.id
      INNER JOIN song_numbers sn on sn.book_id=b.id
      WHERE b.id = :bookId
      GROUP BY b.id
      ORDER BY sn.number
    """
  )
  suspend fun getBookDetail(bookId: BookId): BookDetailProjection?

  ///

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

  @Query("SELECT * FROM books WHERE id = :bookId")
  fun getByIdFlow(bookId: BookId): Flow<BookEntity?>

  @Query("SELECT * FROM books WHERE id in (:bookIds)")
  fun getByIdsFlow(bookIds: List<BookId>): Flow<List<BookEntity>>

  @Query("""SELECT b.* FROM books b INNER JOIN book_statistic bs on bs.id=b.id WHERE bs.priority > 0""")
  fun getAllActiveFlow(): Flow<List<BookWithSongNumbersProjection>>

  @Query(
    """
    SELECT sn.* 
    FROM books b INNER JOIN song_numbers sn ON sn.book_id = b.id 
    WHERE b.id IN (SELECT book_id FROM song_numbers WHERE song_id = :songId AND book_id = :bookId) 
    ORDER BY sn.number
    """
  )
  @Deprecated("will be removed in the next release")
  fun getBookSongNumbersBySongNumberIdFlow(songId: SongId, bookId: BookId): Flow<List<SongNumberEntity>>
}