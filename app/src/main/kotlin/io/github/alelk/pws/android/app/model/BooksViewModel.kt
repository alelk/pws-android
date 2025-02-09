package io.github.alelk.pws.android.app.model

import androidx.lifecycle.ViewModel
import io.github.alelk.pws.database.PwsDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alelk.pws.database.dao.BookDao
import io.github.alelk.pws.database.entity.BookWithSongNumbersEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class BooksViewModel @Inject constructor(database: PwsDatabase) : ViewModel() {
  private val bookDao: BookDao = database.bookDao()

  val allActiveBooks: Flow<List<BookWithSongNumbersEntity>> = bookDao.getAllActiveFlow()
}