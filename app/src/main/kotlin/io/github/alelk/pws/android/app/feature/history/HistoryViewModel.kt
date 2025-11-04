package io.github.alelk.pws.android.app.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.history.HistoryDao
import io.github.alelk.pws.domain.core.ids.SongNumberId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDateTime
import javax.inject.Inject

data class HistoryInfo(
  val songNumberId: SongNumberId,
  val songName: String,
  val songNumber: Int,
  val bookDisplayName: String,
  val timestamp: LocalDateTime
)

@HiltViewModel
class HistoryViewModel @Inject constructor(database: PwsDatabase) : ViewModel() {

  private val historyDao: HistoryDao = database.historyDao()
  private val booksDao = database.bookDao()
  private val songNumbersDao = database.songNumberDao()
  private val songsDao = database.songDao()

  @OptIn(ExperimentalCoroutinesApi::class)
  val historyItems =
    historyDao.getAllFlow()
      .distinctUntilChanged()
      .flatMapLatest { history ->
        val booksFlow = booksDao.getByIdsFlow(history.map { it.bookId }.distinct()).distinctUntilChanged()
        val songNumbersFlow = songNumbersDao.getByIdsFlow(history.map { it.songNumberId }).distinctUntilChanged()
        val songsFlow = songsDao.getByIdsFlow(history.map { it.songId }.distinct()).distinctUntilChanged()
        combine(booksFlow, songNumbersFlow, songsFlow) { books, songNumbers, songs ->
          history.mapNotNull { history ->
            val book = books.find { it.id == history.bookId }
            val songNumber = songNumbers.find { it.id == history.songNumberId }
            val song = songs.find { it.id == history.songId }
            if (book != null && songNumber != null && song != null)
              HistoryInfo(
                songNumberId = history.songNumberId, songName = song.name, songNumber = songNumber.number,
                bookDisplayName = book.displayName, timestamp = history.accessTimestamp
              )
            else
              null
          }
        }
      }
      .distinctUntilChanged()
      .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  suspend fun clearHistory() {
    historyDao.deleteAll()
  }
}