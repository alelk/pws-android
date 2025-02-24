package io.github.alelk.pws.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.alelk.pws.database.entity.SongReferenceDetailsEntity
import io.github.alelk.pws.database.entity.SongReferenceEntity
import io.github.alelk.pws.domain.model.BookId
import io.github.alelk.pws.domain.model.SongId
import io.github.alelk.pws.domain.model.SongNumber
import kotlinx.coroutines.flow.Flow

@Dao
interface SongReferenceDao {
  @Insert
  suspend fun insert(songSongReference: SongReferenceEntity)

  @Insert
  suspend fun insertAll(references: List<SongReferenceEntity>)

  @Update
  suspend fun update(songSongReference: SongReferenceEntity)

  @Delete
  suspend fun delete(songSongReference: SongReferenceEntity)

  @Query("SELECT * FROM song_references WHERE song_id = :songId AND ref_song_id = :refSongId")
  suspend fun getById(songId: SongId, refSongId: SongId): SongReferenceEntity?

  @Query("SELECT count(*) FROM song_references")
  suspend fun count(): Int

  @Query("SELECT * FROM song_references WHERE song_id = :songId ORDER BY priority DESC")
  suspend fun getBySongId(songId: SongId): List<SongReferenceEntity>

  @Query("SELECT * FROM song_references WHERE song_id in (:songIds)")
  suspend fun getBySongIds(songIds: List<SongId>): List<SongReferenceEntity>

  @Query(
    """
    SELECT r.* FROM song_references r
    INNER JOIN song_numbers n on n.song_id = r.ref_song_id
    WHERE r.song_id = :songId and n.number = :refSongNumber and n.book_id = :refSongBookId
    """
  )
  suspend fun getBySongIdAndRefSongNumber(songId: SongId, refSongNumber: Int, refSongBookId: BookId): SongReferenceEntity?

  suspend fun getBySongIdAndRefSongNumber(songId: SongId, refSongNumber: SongNumber): SongReferenceEntity? =
    getBySongIdAndRefSongNumber(songId, refSongNumber.number, refSongNumber.bookId)

  @Query(
    """
    delete from song_references where (song_id, ref_song_id) in (
      SELECT r.song_id, r.ref_song_id FROM song_references r
      INNER JOIN song_numbers n on n.song_id = r.song_id
      WHERE r.song_id = :songId and n.number = :refSongNumber and n.book_id = :refSongBookId
    )
    """
  )
  suspend fun deleteBySongIdAndRefSongNumber(songId: Long, refSongNumber: Int, refSongBookId: BookId)

  suspend fun deleteBySongIdAndRefSongNumber(songId: Long, refSongNumber: SongNumber) =
    deleteBySongIdAndRefSongNumber(songId, refSongNumber.number, refSongNumber.bookId)

  @Query("DELETE FROM song_references")
  suspend fun deleteAll()

  // flow

  @Query(
    """
    SELECT
      r.*,
      b.*
    FROM song_references r
    INNER JOIN songs s ON r.ref_song_id = s.id
    INNER JOIN song_numbers sn ON s.id = sn.song_id
    INNER JOIN books b ON sn.book_id = b.id
    INNER JOIN book_statistic bs ON b.id = bs.id
    WHERE r.song_id = :songId AND bs.priority > 0
    ORDER BY bs.priority DESC, r.priority
    """
  )
  fun getActiveReferredSongsBySongIdFlow(songId: SongId): Flow<List<SongReferenceDetailsEntity>>
}