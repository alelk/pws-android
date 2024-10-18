package io.github.alelk.pws.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import io.github.alelk.pws.database.common.entity.FavoriteEntity

@Dao
interface FavoriteDao {
  @Insert
  suspend fun insert(favorite: FavoriteEntity): Long

  @Query("SELECT * FROM favorites WHERE _id = :id")
  suspend fun getById(id: Long): FavoriteEntity?

  @Query("SELECT * FROM favorites WHERE psalmnumberid = :songNumberId")
  suspend fun getBySongNumberId(songNumberId: Long): List<FavoriteEntity>

  @Delete
  suspend fun delete(favorite: FavoriteEntity)

  @Query("DELETE FROM favorites")
  suspend fun deleteAll()
}