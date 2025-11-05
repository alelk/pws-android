package io.github.alelk.pws.android.app.feature.books

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.book.BookDao
import io.github.alelk.pws.database.book.BookWithSongNumbersEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class BooksViewModel @Inject constructor(database: PwsDatabase) : ViewModel() {
  private val bookDao: BookDao = database.bookDao()

  val allActiveBooks: Flow<List<BookWithSongNumbersEntity>> = bookDao.getAllActiveFlow()
}