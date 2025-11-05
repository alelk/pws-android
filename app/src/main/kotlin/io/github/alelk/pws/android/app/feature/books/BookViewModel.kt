package io.github.alelk.pws.android.app.feature.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.song_number.SongNumberWithSongEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alelk.pws.database.book.BookEntity
import io.github.alelk.pws.database.song_number.SongNumberEntity
import io.github.alelk.pws.domain.core.ids.BookId
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
  val songs: List<SongNumberWithSongEntity>
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class BookViewModel @Inject constructor(database: PwsDatabase) : ViewModel() {
  private val bookDao = database.bookDao()
  private val songNumberDao = database.songNumberDao()

  private val _bookId = MutableStateFlow<BookId?>(null)
  val bookId = _bookId.asStateFlow()

  fun setBookId(bookId: BookId) {
    if (_bookId.value != bookId) {
      _bookId.value = bookId
      Timber.d("book id is changed: $bookId")
    }
  }

  val book: StateFlow<BookInfo?> =
    _bookId.filterNotNull()
      .flatMapLatest { bookId ->
        val bookFlow = bookDao.getByIdFlow(bookId)
        val songNumbersFlow = songNumberDao.getBookSongsByBookIdFlow(bookId)
        combine(bookFlow, songNumbersFlow) { book, songNumbers -> book?.let { BookInfo(book, songNumbers) } }
      }
      .distinctUntilChanged()
      .mapLatest { it?.let { book -> book.copy(songs = book.songs.sortedBy { s -> s.songNumber.number }) } }
      .stateIn(viewModelScope, SharingStarted.Lazily, null)

  val songNumbers: StateFlow<List<SongNumberEntity>?> =
    book.mapLatest { it?.songs?.map { s -> s.songNumber } }.distinctUntilChanged()
      .stateIn(viewModelScope, SharingStarted.Lazily, null)
}