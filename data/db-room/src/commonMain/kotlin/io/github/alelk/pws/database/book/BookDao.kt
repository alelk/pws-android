package io.github.alelk.pws.database.book

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import io.github.alelk.pws.database.core.Pageable
import io.github.alelk.pws.domain.core.Locale
import io.github.alelk.pws.domain.core.ids.BookId
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao : Pageable<BookEntity> {

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(book: BookEntity)

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(books: List<BookEntity>)

  @Upsert
  suspend fun update(book: BookEntity)

  @Upsert
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

  @Query(
    """
      SELECT 
        b.id as id, b.version as version, b.locale as locale, b.name as name, b.display_short_name as display_short_name, b.display_name as display_name,
        b.release_date as release_date, b.authors as authors, b.creators as creators, b.reviewers as reviewers, b.editors as editors,
        b.description as description, b.preface as preface,
        count(sn.number) as count_songs, 
        (SELECT (sn2.book_id || '/' || sn2.song_id) FROM song_numbers sn2 WHERE sn2.book_id = b.id ORDER BY sn2.number LIMIT 1) AS first_song_number_id, 
        bs.priority as priority
      FROM books b
      INNER JOIN book_statistic bs on bs.id=b.id
      LEFT OUTER JOIN song_numbers sn on sn.book_id=b.id
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
        count(sn.number) as count_songs, 
        (SELECT (sn2.book_id || '/' || sn2.song_id) FROM song_numbers sn2 WHERE sn2.book_id = b.id ORDER BY sn2.number LIMIT 1) AS first_song_number_id, 
        bs.priority as priority
      FROM books b
      INNER JOIN book_statistic bs on bs.id=b.id
      LEFT OUTER JOIN song_numbers sn on sn.book_id=b.id
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
        count(sn.number) as count_songs, 
        (SELECT (sn2.book_id || '/' || sn2.song_id) FROM song_numbers sn2 WHERE sn2.book_id = b.id ORDER BY sn2.number LIMIT 1) AS first_song_number_id, 
        bs.priority as priority
      FROM books b
      INNER JOIN book_statistic bs on bs.id=b.id
      LEFT OUTER JOIN song_numbers sn on sn.book_id=b.id
      WHERE b.id = :bookId
      GROUP BY b.id
      ORDER BY sn.number
    """
  )
  suspend fun getBookDetail(bookId: BookId): BookDetailProjection?

  @Query(
    """
      SELECT 
        b.id as id, b.version as version, b.locale as locale, b.name as name, b.display_short_name as display_short_name, b.display_name as display_name,
        count(sn.number) as count_songs, 
        (SELECT (sn2.book_id || '/' || sn2.song_id) FROM song_numbers sn2 WHERE sn2.book_id = b.id ORDER BY sn2.number LIMIT 1) AS first_song_number_id, 
        bs.priority as priority
      FROM books b
      INNER JOIN book_statistic bs on bs.id=b.id
      LEFT OUTER JOIN song_numbers sn on sn.book_id=b.id
      WHERE 
        (:locale IS NULL OR b.locale = :locale) 
        AND (:minPriority IS NULL OR bs.priority >= :minPriority) 
        AND (:maxPriority IS NULL OR bs.priority <= :maxPriority)
      GROUP BY b.id
      ORDER BY bs.priority DESC, sn.number
    """
  )
  suspend fun getBooksSummary(locale: Locale? = null, minPriority: Int? = null, maxPriority: Int? = null): List<BookSummaryProjection>

  @Deprecated("Use observeBookDetail instead")
  @Query("SELECT * FROM books WHERE id = :bookId")
  fun getByIdFlow(bookId: BookId): Flow<BookEntity?>

  @Deprecated("Use observeBooksSummary instead")
  @Query("SELECT * FROM books WHERE id in (:bookIds)")
  fun getByIdsFlow(bookIds: List<BookId>): Flow<List<BookEntity>>
}