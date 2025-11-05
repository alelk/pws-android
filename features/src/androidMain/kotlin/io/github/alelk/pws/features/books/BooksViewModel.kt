package io.github.alelk.pws.features.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.alelk.pws.domain.book.model.BookSummary
import io.github.alelk.pws.domain.book.repository.BookRepository
import io.github.alelk.pws.domain.book.query.BookSort
import io.github.alelk.pws.domain.book.query.BookQuery
import io.github.alelk.pws.domain.core.Locale
import io.github.alelk.pws.features.usecase.observeBooks
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** ViewModel for listing books (song collections). */
sealed interface BooksIntent {
  data class Search(val text: String) : BooksIntent
  data class ToggleLocale(val locale: Locale) : BooksIntent
  data class Refresh(val force: Boolean = false) : BooksIntent
}

class BooksViewModel(
  private val bookRepository: BookRepository,
) : ViewModel() {
  private val _uiState = MutableStateFlow(BooksUiState())
  val uiState: StateFlow<BooksUiState> = _uiState.asStateFlow()

  private var searchJob: Job? = null

  init { observeBooks() }

  private fun observeBooks(query: BookQuery = currentQuery(), sort: BookSort = BookSort.ByPriorityDesc) {
    viewModelScope.launch {
      bookRepository.observeBooks(query, sort)
        .onStart { _uiState.update { it.copy(isLoading = true) } }
        .catch { e -> _uiState.update { it.copy(errorMessage = e.message, isLoading = false) } }
        .collect { books -> _uiState.update { it.copy(books = applyClientSearch(books), isLoading = false) } }
    }
  }

  private fun currentQuery(): BookQuery = BookQuery(locale = _uiState.value.filterLocale, minPriority = null, maxPriority = null)

  fun dispatch(intent: BooksIntent) = when (intent) {
    is BooksIntent.Search -> onSearchChange(intent.text)
    is BooksIntent.ToggleLocale -> toggleLocale(intent.locale)
    is BooksIntent.Refresh -> observeBooks()
  }

  private fun onSearchChange(text: String) {
    _uiState.update { it.copy(search = text) }
    searchJob?.cancel()
    searchJob = viewModelScope.launch {
      delay(250) // debounce
      _uiState.update { st -> st.copy(books = applyClientSearch(st.books)) }
    }
  }

  private fun toggleLocale(locale: Locale) {
    _uiState.update { state ->
      val newLocale = if (state.filterLocale == locale) null else locale
      state.copy(filterLocale = newLocale)
    }
    observeBooks()
  }

  private fun applyClientSearch(source: List<BookSummary>): List<BookSummary> {
    val query = _uiState.value.search.trim().lowercase()
    if (query.isBlank()) return source
    return source.filter { it.displayName.value.lowercase().contains(query) || it.name.value.lowercase().contains(query) }
  }
}

// TODO Temporary: directly uses BookRepository.observeBooks; replace with domain use case when available.

data class BooksUiState(
  val books: List<BookSummary> = emptyList(),
  val search: String = "",
  val filterLocale: Locale? = null,
  val isLoading: Boolean = true,
  val errorMessage: String? = null,
)
