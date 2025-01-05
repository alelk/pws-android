package com.alelk.pws.pwapp.model

import androidx.lifecycle.ViewModel
import com.alelk.pws.database.PwsDatabase
import com.alelk.pws.database.dao.Favorite
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(database: PwsDatabase) : ViewModel() {
  private val favoritesDao = database.favoriteDao()
  fun getFavoritesSortedByDate(): Flow<List<Favorite>> = favoritesDao.getAll()
  fun getFavoritesSortedByName(): Flow<List<Favorite>> = favoritesDao.getAll(sort = "songName")
  fun getFavoritesSortedByNumber(): Flow<List<Favorite>> = favoritesDao.getAll(sort = "songNumber")

  @Deprecated("use toggleFavorite on SongViewModel")
  suspend fun toggleFavorite(songNumberId: Long) = favoritesDao.toggleFavorite(songNumberId)
}