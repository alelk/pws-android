package io.github.alelk.pws.database.dao

import androidx.room.*
import io.github.alelk.pws.database.common.entity.SongSongReferenceEntity
import io.github.alelk.pws.database.common.model.BookExternalId

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
}