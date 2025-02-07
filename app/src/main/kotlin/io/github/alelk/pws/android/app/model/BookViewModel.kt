package io.github.alelk.pws.android.app.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.dao.SongNumberWithSong
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alelk.pws.database.common.entity.BookEntity
import io.github.alelk.pws.database.common.entity.SongNumberEntity
import io.github.alelk.pws.domain.model.BookExternalId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import javax.inject.Inject

data class BookInfo(
  val book: BookEntity,
  val songs: List<SongNumberWithSong>
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class BookViewModel @Inject constructor(database: PwsDatabase) : ViewModel() {
  private val bookDao = database.bookDao()
  private val songNumberDao = database.songNumberDao()

  private val _bookExternalId = MutableStateFlow<BookExternalId?>(null)
  val bookExternalId = _bookExternalId.asStateFlow()

  fun setBookExternalId(bookExternalId: BookExternalId) {
    if (_bookExternalId.value != bookExternalId) {
      _bookExternalId.value = bookExternalId
      Timber.d("book external id is changed: $bookExternalId")
    }
  }

  val book: StateFlow<BookInfo?> =
    _bookExternalId.filterNotNull()
      .flatMapLatest { bookExternalId ->
        val bookFlow = bookDao.getByExternalId(bookExternalId)
        val songNumbersFlow = songNumberDao.getBookSongsByBookId(bookExternalId)
        combine(bookFlow, songNumbersFlow) { book, songNumbers -> book?.let { BookInfo(book, songNumbers) } }
      }
      .distinctUntilChanged()
      .mapLatest { it?.let { book -> book.copy(songs = book.songs.sortedBy { s -> s.songNumber.number }) } }
      .stateIn(viewModelScope, SharingStarted.Lazily, null)

  val songNumbers: StateFlow<List<SongNumberEntity>?> =
    book.mapLatest { it?.songs?.map { s -> s.songNumber } }.distinctUntilChanged()
      .stateIn(viewModelScope, SharingStarted.Lazily, null)
}