package io.github.alelk.pws.android.app.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.alelk.pws.database.PwsDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(database: PwsDatabase) : ViewModel() {
  private val favoritesDao = database.favoriteDao()

  val allFavorites = favoritesDao.getAllFlow().distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  @Deprecated("use toggleFavorite on SongViewModel")
  suspend fun toggleFavorite(songNumberId: Long) = favoritesDao.toggleFavorite(songNumberId)
}