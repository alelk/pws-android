package io.github.alelk.pws.android.app.feature.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alelk.pws.domain.book.model.BookSummary
import io.github.alelk.pws.domain.book.usecase.ObserveBooksUseCase
import io.github.alelk.pws.domain.bookstatistic.usecase.UpdateBookStatisticUseCase
import io.github.alelk.pws.domain.bookstatistic.command.UpdateBookStatisticCommand
import io.github.alelk.pws.domain.core.ids.BookId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
class BooksViewModel @Inject constructor(
  observeBooks: ObserveBooksUseCase,
  private val updateBookStatistic: UpdateBookStatisticUseCase,
) : ViewModel() {

  companion object {
    private const val DEFAULT_ENABLED_PRIORITY = 1
    private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
  }

  private val booksFlow: Flow<List<BookSummary>> = observeBooks()

  private val booksState: StateFlow<List<BookSummary>> =
    booksFlow.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), initialValue = emptyList())

  val allBooks: Flow<List<BookSummary>> = booksFlow
  val allActiveBooks: Flow<List<BookSummary>> = allBooks.map { books -> books.filter { it.enabled } }

  private val _lastError = MutableStateFlow<Throwable?>(null)
  val lastError: StateFlow<Throwable?> = _lastError

  private val updateMutex = Mutex()

  fun setBookEnabled(bookId: BookId, enabled: Boolean) {
    viewModelScope.launch {
      updateMutex.withLock {
        val current = booksState.value.firstOrNull { it.id == bookId }
        if (current != null && current.enabled == enabled) return@withLock

        val newPriority = if (enabled) current?.priority?.takeIf { it > 0 } ?: DEFAULT_ENABLED_PRIORITY else 0

        runCatching {
          updateBookStatistic(UpdateBookStatisticCommand(id = bookId, priority = newPriority))
        }.onFailure { err ->
          _lastError.value = err
        }
      }
    }
  }
}