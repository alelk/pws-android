package io.github.alelk.pws.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.github.alelk.pws.database.entity.FavoriteEntity
import io.github.alelk.pws.database.entity.FavoriteWithSongNumberWithSongWithBook
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
  @Insert
  suspend fun insert(favorite: FavoriteEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(favorite: FavoriteEntity): Long

  @Query("SELECT * FROM favorites WHERE _id = :id")
  suspend fun getById(id: Long): FavoriteEntity?

  @Transaction
  @Query("SELECT * FROM favorites WHERE psalmnumberid = :songNumberId LIMIT 1")
  suspend fun getBySongNumberId(songNumberId: Long): FavoriteEntity?

  suspend fun isFavorite(songNumberId: Long) = getBySongNumberId(songNumberId) != null

  @Delete
  suspend fun delete(favorite: FavoriteEntity)

  @Transaction
  @Query("DELETE FROM favorites WHERE psalmnumberid = :songNumberId")
  suspend fun deleteBySongNumberId(songNumberId: Long)

  @Query("DELETE FROM favorites")
  suspend fun deleteAll()

  @Transaction
  @Query("""SELECT pn.* FROM favorites f INNER JOIN psalmnumbers pn on f.psalmnumberid = pn._id ORDER BY  f.position""")
  fun getAllFlow(): Flow<List<FavoriteWithSongNumberWithSongWithBook>>

  @Transaction
  @Query("SELECT * FROM favorites ORDER BY position DESC")
  suspend fun getLast(): FavoriteEntity?

  @Transaction
  suspend fun addToFavorites(songNumberId: Long) {
    val existing = getBySongNumberId(songNumberId)
    if (existing == null) {
      val maxPosition = getLast()?.position ?: 0
      val favorite = FavoriteEntity(
        position = maxPosition + 1,
        songNumberId = songNumberId
      )
      upsert(favorite)
    }
  }

  @Transaction
  suspend fun toggleFavorite(songNumberId: Long) {
    if (isFavorite(songNumberId)) deleteBySongNumberId(songNumberId)
    else addToFavorites(songNumberId)
  }
}