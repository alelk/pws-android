package io.github.alelk.pws.android.app.feature.books

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alelk.pws.domain.book.model.BookSummary
import io.github.alelk.pws.domain.book.query.BookQuery
import io.github.alelk.pws.domain.book.repository.BookRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class BooksViewModel @Inject constructor(bookRepository: BookRepository) : ViewModel() {

  val allActiveBooks: Flow<List<BookSummary>> = bookRepository.observeBooks(BookQuery(enabled = true))
}