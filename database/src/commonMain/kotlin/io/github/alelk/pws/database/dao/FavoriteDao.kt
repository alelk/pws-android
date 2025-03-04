package io.github.alelk.pws.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.github.alelk.pws.database.entity.FavoriteEntity
import io.github.alelk.pws.database.entity.SongNumberEntity
import io.github.alelk.pws.domain.model.BookId
import io.github.alelk.pws.domain.model.SongId
import io.github.alelk.pws.domain.model.SongNumberId
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
  @Insert
  suspend fun insert(favorite: FavoriteEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(favorite: FavoriteEntity)

  @Transaction
  @Query("SELECT * FROM favorites WHERE book_id = :bookId AND song_id = :songId")
  suspend fun getById(bookId: BookId, songId: SongId): FavoriteEntity?

  suspend fun getBySongNumberId(id: SongNumberId) = getById(id.bookId, id.songId)

  suspend fun isFavorite(songNumberId: SongNumberId) = getBySongNumberId(songNumberId) != null

  @Transaction
  @Query("SELECT COUNT(*) FROM favorites")
  suspend fun count(): Int

  @Delete
  suspend fun delete(favorite: FavoriteEntity)

  @Transaction
  @Query("DELETE FROM favorites WHERE book_id = :bookId AND song_id = :songId")
  suspend fun deleteById(bookId: BookId, songId: SongId)

  suspend fun deleteBySongNumberId(id: SongNumberId) = deleteById(id.bookId, id.songId)

  @Query("DELETE FROM favorites")
  suspend fun deleteAll()

  @Transaction
  @Query("SELECT * FROM favorites ORDER BY position DESC")
  suspend fun getLast(): FavoriteEntity?

  @Transaction
  suspend fun addToFavorites(songNumberId: SongNumberId) {
    val existing = getBySongNumberId(songNumberId)
    if (existing == null) {
      val maxPosition = getLast()?.position ?: 0
      val favorite = FavoriteEntity(position = maxPosition + 1, songNumberId = songNumberId)
      upsert(favorite)
    }
  }

  @Transaction
  suspend fun toggleFavorite(songNumberId: SongNumberId) {
    if (isFavorite(songNumberId)) deleteBySongNumberId(songNumberId)
    else addToFavorites(songNumberId)
  }

  // flows

  @Transaction
  @Query("SELECT * FROM favorites WHERE book_id = :bookId AND song_id = :songId")
  fun getByIdFlow(bookId: BookId, songId: SongId): Flow<FavoriteEntity?>

  fun getByIdFlow(id: SongNumberId) = getByIdFlow(id.bookId, id.songId)

  @Transaction
  @Query("""SELECT * FROM favorites ORDER BY position""")
  fun getAllFlow(): Flow<List<FavoriteEntity>>

  @Transaction
  @Query("""SELECT f.*, sn.* FROM favorites f INNER JOIN song_numbers sn on f.song_id = sn.song_id and f.book_id = sn.book_id ORDER BY position""")
  fun getAllFavoritesWithSongNumberFlow(): Flow<Map<FavoriteEntity, SongNumberEntity>>
}