package io.github.alelk.pws.android.app.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.alelk.pws.database.PwsDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alelk.pws.domain.model.SongNumberId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class FavoriteInfo(
  val songNumberId: SongNumberId,
  val songName: String,
  val songNumber: Int,
  val bookDisplayName: String,
  val position: Int
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(database: PwsDatabase) : ViewModel() {
  private val favoritesDao = database.favoriteDao()
  private val booksDao = database.bookDao()
  private val songNumbersDao = database.songNumberDao()
  private val songsDao = database.songDao()

  @OptIn(ExperimentalCoroutinesApi::class)
  val allFavorites =
    favoritesDao
      .getAllFlow()
      .distinctUntilChanged()
      .flatMapLatest { favorites ->
        val booksFlow = booksDao.getByIdsFlow(favorites.map { it.bookId }.distinct()).distinctUntilChanged()
        val songNumbersFlow = songNumbersDao.getByIdsFlow(favorites.map { it.songNumberId }).distinctUntilChanged()
        val songsFlow = songsDao.getByIdsFlow(favorites.map { it.songId }.distinct()).distinctUntilChanged()
        combine(booksFlow, songNumbersFlow, songsFlow) { books, songNumbers, songs ->
          favorites.mapNotNull { favorite ->
            val book = books.find { it.id == favorite.bookId }
            val songNumber = songNumbers.find { it.id == favorite.songNumberId }
            val song = songs.find { it.id == favorite.songId }
            if (book != null && songNumber != null && song != null)
              FavoriteInfo(
                songNumberId = favorite.songNumberId, songName = song.name, songNumber = songNumber.number,
                bookDisplayName = book.displayName, position = songNumber.number
              )
            else
              null
          }
        }
      }
      .distinctUntilChanged()
      .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}