package io.github.alelk.pws.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.github.alelk.pws.database.common.entity.FavoriteEntity
import io.github.alelk.pws.domain.model.BookExternalId
import kotlinx.coroutines.flow.Flow

data class Favorite(
  val id: Long,
  val songNumber: Int,
  val songName: String,
  val bookId: BookExternalId,
  val bookDisplayName: String,
  val songNumberId: Long,
  val bookShortName: String
)

@Dao
interface FavoriteDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(favorite: FavoriteEntity): Long

  @Transaction
  @Query("DELETE FROM favorites WHERE psalmnumberid = :songNumberId")
  suspend fun deleteBySongNumberId(songNumberId: Long)

  @Transaction
  @Query("SELECT * FROM favorites WHERE psalmnumberid = :songNumberId LIMIT 1")
  suspend fun getBySongNumberId(songNumberId: Long): FavoriteEntity?

  suspend fun isFavorite(songNumberId: Long) = getBySongNumberId(songNumberId) != null

  @Transaction
  @Query(
    """
    SELECT 
      f._id as id, 
      p.name as songName, 
      pn.number as songNumber, 
      b.edition as bookId,
      b.displayname as bookDisplayName, 
      pn._id as songNumberId, 
      b.displayshortname as bookShortName
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