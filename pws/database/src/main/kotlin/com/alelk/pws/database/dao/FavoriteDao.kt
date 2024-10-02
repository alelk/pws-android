package com.alelk.pws.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.alelk.pws.database.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

data class Favorite(val id: Long, val songNumber: Int, val songName: String, val bookDisplayName: String, val songNumberId: Long)

@Dao
interface FavoriteDao {
  @Insert
  suspend fun insert(favorite: FavoriteEntity): Long

  @Query("SELECT * FROM favorites WHERE _id = :id")
  suspend fun getById(id: Long): FavoriteEntity?

  @Query("SELECT * FROM favorites WHERE psalmnumberid = :songNumberId")
  suspend fun getBySongNumberId(songNumberId: Long): List<FavoriteEntity>

  @Transaction
  @Query(
    """
    SELECT f._id as id, p.name as songName, pn.number as songNumber, b.displayname as bookDisplayName, pn._id as songNumberId
    FROM favorites f 
    INNER JOIN psalmnumbers pn on f.psalmnumberid = pn._id 
    INNER JOIN psalms p on pn.psalmid=p._id
    INNER JOIN books b on pn.bookid=b._id
    ORDER BY
    CASE
      WHEN :sort = 'songName' THEN p.name 
      WHEN :sort = 'songNumber' THEN pn.number
      ELSE f.position
    END ASC
    """
  )
  fun getAll(sort: String = "default"): Flow<List<Favorite>>

  @Delete
  suspend fun delete(favorite: FavoriteEntity)

  @Query("DELETE FROM favorites")
  suspend fun deleteAll()
}