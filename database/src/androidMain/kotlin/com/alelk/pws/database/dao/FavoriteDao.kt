package com.alelk.pws.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.github.alelk.pws.database.common.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class Favorite(val id: Long, val songNumber: Int, val songName: String, val bookDisplayName: String, val songNumberId: Long)

@Dao
interface FavoriteDao {
  @Insert
  suspend fun insert(favorite: FavoriteEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(favorite: FavoriteEntity): Long

  @Transaction
  @Query("SELECT * FROM favorites WHERE _id = :id")
  suspend fun getById(id: Long): FavoriteEntity?

  @Transaction
  @Query("DELETE FROM favorites WHERE psalmnumberid = :songNumberId")
  suspend fun deleteBySongNumberId(songNumberId: Long)

  @Transaction
  @Query("SELECT * FROM favorites WHERE psalmnumberid = :songNumberId LIMIT 1")
  suspend fun getBySongNumberId(songNumberId: Long): FavoriteEntity?

  @Transaction
  @Query("SELECT * FROM favorites WHERE psalmnumberid = :songNumberId LIMIT 1")
  fun getBySongNumberIdFlow(songNumberId: Long): Flow<List<FavoriteEntity>>

  fun isFavoriteFlow(songNumberId: Long): Flow<Boolean> = getBySongNumberIdFlow(songNumberId).map { it.isNotEmpty() }

  suspend fun isFavorite(songNumberId: Long) = getBySongNumberId(songNumberId) != null

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

  @Transaction
  @Query("SELECT * FROM favorites ORDER BY position DESC")
  suspend fun getLast(): FavoriteEntity?

  @Transaction
  suspend fun addToFavorites(songNumberId: Long) {
    val existing = getBySongNumberId(songNumberId)
    val lastPosition = getLast()?.position
    val position = lastPosition?.plus(1) ?: 1
    val favorite = existing?.copy(position = position) ?: FavoriteEntity(position = position, songNumberId = songNumberId)
    upsert(favorite)
  }

  @Transaction
  suspend fun toggleFavorite(songNumberId: Long) {
    if (isFavorite(songNumberId)) deleteBySongNumberId(songNumberId)
    else addToFavorites(songNumberId)
  }

  @Delete
  suspend fun delete(favorite: FavoriteEntity)

  @Query("DELETE FROM favorites")
  suspend fun deleteAll()
}