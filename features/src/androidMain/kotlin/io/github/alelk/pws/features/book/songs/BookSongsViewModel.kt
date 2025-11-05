package io.github.alelk.pws.features.book.songs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.song.model.SongSummary
import io.github.alelk.pws.domain.song.repository.SongRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BookSongsViewModel(
  private val bookId: BookId,
  private val songRepository: SongRepository,
) : ViewModel() {
  private val _uiState = MutableStateFlow(BookSongsUiState())
  val uiState: StateFlow<BookSongsUiState> = _uiState.asStateFlow()

  init { observeSongs() }

  private fun observeSongs() {
    viewModelScope.launch {
      songRepository.observeAllInBook(bookId).collect { map ->
        _uiState.update { it.copy(songs = map.toSortedMap().map { (num, summary) -> num to summary }, isLoading = false) }
      }
    }
  }
}

data class BookSongsUiState(
  val songs: List<Pair<Int, SongSummary>> = emptyList(),
  val isLoading: Boolean = true,
)

