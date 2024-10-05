package com.alelk.pws.pwapp.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.alelk.pws.database.DatabaseProvider
import com.alelk.pws.database.dao.Favorite
import kotlinx.coroutines.flow.Flow

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {
  private val favoritesDao = DatabaseProvider.getDatabase(application).favoriteDao()
  fun getFavoritesSortedByDate(): Flow<List<Favorite>> = favoritesDao.getAll()
  fun getFavoritesSortedByName(): Flow<List<Favorite>> = favoritesDao.getAll(sort = "songName")
  fun getFavoritesSortedByNumber(): Flow<List<Favorite>> = favoritesDao.getAll(sort = "songNumber")

  @Deprecated("use toggleFavorite on SongViewModel")
  suspend fun toggleFavorite(songNumberId: Long) = favoritesDao.toggleFavorite(songNumberId)
}