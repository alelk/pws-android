package io.github.alelk.pws.database.dao

import androidx.room.*
import io.github.alelk.pws.database.entity.SongSongReferenceDetailsEntity
import io.github.alelk.pws.database.entity.SongSongReferenceEntity
import io.github.alelk.pws.domain.model.BookExternalId
import kotlinx.coroutines.flow.Flow

@Dao
interface SongSongReferenceDao {
  @Insert
  suspend fun insert(songSongReference: SongSongReferenceEntity): Long

  @Insert
  suspend fun insertAll(references: List<SongSongReferenceEntity>): List<Long>

  @Update
  suspend fun update(songSongReference: SongSongReferenceEntity)

  @Delete
  suspend fun delete(songSongReference: SongSongReferenceEntity)

  @Query("SELECT * FROM psalmpsalmreferences WHERE _id = :id")
  suspend fun getById(id: Long): SongSongReferenceEntity?

  @Query("SELECT * FROM psalmpsalmreferences WHERE _id in (:ids)")
  suspend fun getByIds(ids: List<Long>): List<SongSongReferenceEntity>

  @Query("SELECT * FROM psalmpsalmreferences WHERE psalmid = :songId ORDER BY priority DESC")
  suspend fun getBySongId(songId: Long): List<SongSongReferenceEntity>

  @Query("SELECT * FROM psalmpsalmreferences WHERE psalmid in (:songIds)")
  suspend fun getBySongIds(songIds: List<Long>): List<SongSongReferenceEntity>

  @Query("SELECT * FROM psalmpsalmreferences WHERE refpsalmid = :refSongId")
  suspend fun getByRefSongId(refSongId: Long): List<SongSongReferenceEntity>

  @Query("SELECT * FROM psalmpsalmreferences WHERE psalmid = :songId and refpsalmid = :refSongId")
  suspend fun getBySongIdAndRefSongId(songId: Long, refSongId: Long): SongSongReferenceEntity?

  @Query(
    """
    SELECT r.* FROM psalmpsalmreferences r
    INNER JOIN psalmnumbers n on n.psalmid = r.refpsalmid
    INNER JOIN books b on b._id = n.bookid
    WHERE r.psalmid = :songId and n.number = :refSongNumber and b.edition = :refSongBookExternalId
    """
  )
  suspend fun getBySongIdAndRefSongNumber(songId: Long, refSongNumber: Int, refSongBookExternalId: BookExternalId): SongSongReferenceEntity?

  @Query(
    """
    delete from psalmpsalmreferences where _id in ( 
      SELECT r._id FROM psalmpsalmreferences r
      INNER JOIN psalmnumbers n on n.psalmid = r.refpsalmid
      INNER JOIN books b on b._id = n.bookid
      WHERE r.psalmid = :songId and n.number = :refSongNumber and b.edition = :refSongBookExternalId
    )
    """
  )
  suspend fun deleteBySongIdAndRefSongNumber(songId: Long, refSongNumber: Int, refSongBookExternalId: BookExternalId)

  @Query("DELETE FROM psalmpsalmreferences")
  suspend fun deleteAll()

  @Query(
    """
    SELECT 
      r.psalmid AS songId,
      r.refpsalmid AS refSongId,
      r.reason AS refReason,
      r.volume AS volume,
      p.name AS refSongName,
      pn.number AS refSongNumber,
      pn._id AS refSongNumberId,
      b._id AS refSongNumberBookId,
      b.edition AS refSongNumberBookExternalId,
      b.displayName AS refSongNumberBookDisplayName
    FROM psalmnumbers source
    INNER JOIN psalmpsalmreferences r ON source.psalmid = r.psalmid
    INNER JOIN psalms p ON r.refpsalmid = p._id
    INNER JOIN psalmnumbers pn ON p._id = pn.psalmid
    INNER JOIN books b ON pn.bookid = b._id
    INNER JOIN bookstatistic bs ON b._id = bs.bookid
    WHERE source._id = :songNumberId AND bs.userpref > 0
    ORDER BY bs.userpref DESC, r.priority
    """
  )
  fun getBySongNumberIdFlow(songNumberId: Long): Flow<List<SongSongReferenceDetailsEntity>>
}