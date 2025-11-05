package io.github.alelk.pws.android.app.feature.books

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alelk.pws.domain.book.model.BookSummary
import io.github.alelk.pws.domain.book.query.BookQuery
import io.github.alelk.pws.domain.book.usecase.ObserveBooksUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class BooksViewModel @Inject constructor(observeBooksUseCase: ObserveBooksUseCase) : ViewModel() {

  val allActiveBooks: Flow<List<BookSummary>> = observeBooksUseCase(BookQuery(enabled = true))
}