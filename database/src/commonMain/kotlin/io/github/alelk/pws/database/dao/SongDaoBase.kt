package io.github.alelk.pws.database.dao

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.alelk.pws.database.entity.SongEntity
import io.github.alelk.pws.database.entity.SongNumberWithSongWithBook

interface SongDaoBase : Pageable1<SongEntity> {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(song: SongEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(songs: List<SongEntity>): List<Long>

  @Query("SELECT * FROM psalms WHERE _id = :id")
  suspend fun getById(id: Long): SongEntity?

  @Query("SELECT * FROM psalms WHERE _id in (:ids)")
  suspend fun getByIds(ids: List<Long>): List<SongEntity>

  @Query("SELECT * FROM psalms ORDER BY _id LIMIT :limit OFFSET :offset")
  override suspend fun getAll(limit: Int, offset: Int): List<SongEntity>

  @Query("""SELECT pn.* FROM psalmnumbers pn inner join psalms p on pn.psalmid = p._id WHERE p.edited > 0""")
  suspend fun getAllEdited(): List<SongNumberWithSongWithBook>

  @Query("SELECT count(_id) FROM psalms")
  suspend fun count(): Int

  @Update
  suspend fun update(song: SongEntity)

  @Delete
  suspend fun delete(song: SongEntity)

  @Delete
  suspend fun delete(songs: List<SongEntity>)

  @Query("DELETE FROM psalms")
  suspend fun deleteAll()

}