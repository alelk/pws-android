package com.alelk.pws.pwapp.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.alelk.pws.database.DatabaseProvider
import com.alelk.pws.database.dao.Favorite
import kotlinx.coroutines.flow.Flow

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {
  val favoritesDao = DatabaseProvider.getDatabase(application).favoriteDao()
  fun getFavoritesSortedByDate(): Flow<List<Favorite>> = favoritesDao.getAll()
  fun getFavoritesSortedByName(): Flow<List<Favorite>> = favoritesDao.getAll(sort = "songName")
  fun getFavoritesSortedByNumber(): Flow<List<Favorite>> = favoritesDao.getAll(sort = "songNumber")
  fun isFavorite(songNumberId: Long): Flow<Boolean> = favoritesDao.isFavoriteFlow(songNumberId)
  suspend fun addToFavorites(songNumberId: Long) = favoritesDao.addToFavorites(songNumberId)
  suspend fun toggleFavorite(songNumberId: Long) = favoritesDao.toggleFavorite(songNumberId)
  suspend fun deleteFromFavorites(songNumberId: Long) = favoritesDao.deleteBySongNumberId(songNumberId)
}