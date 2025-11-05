package io.github.alelk.pws.features.song.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.song.model.SongDetail
import io.github.alelk.pws.domain.song.repository.SongRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SongDetailViewModel(
  private val songId: SongId,
  private val songRepository: SongRepository,
) : ViewModel() {
  private val _uiState = MutableStateFlow(SongDetailUiState())
  val uiState: StateFlow<SongDetailUiState> = _uiState.asStateFlow()

  init { observeSong() }

  private fun observeSong() {
    viewModelScope.launch {
      songRepository.observe(songId).collect { detail ->
        _uiState.update { it.copy(song = detail, isLoading = false) }
      }
    }
  }
}

data class SongDetailUiState(
  val song: SongDetail? = null,
  val isLoading: Boolean = true,
)

