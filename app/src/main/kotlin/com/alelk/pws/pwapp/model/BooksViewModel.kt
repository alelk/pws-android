package com.alelk.pws.pwapp.model

import androidx.lifecycle.ViewModel
import com.alelk.pws.database.PwsDatabase
import com.alelk.pws.database.dao.Book
import com.alelk.pws.database.dao.BookDao
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alelk.pws.database.common.entity.SongNumberEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class BooksViewModel @Inject constructor(database: PwsDatabase) : ViewModel() {
  private val bookDao: BookDao = database.bookDao()

  val allActiveBooks: Flow<List<Book>> = bookDao.getAllActive()

  @Deprecated("use BookViewModel")
  fun getBookSongNumbers(bookId: Long): Flow<List<SongNumberEntity>> = bookDao.getBookSongNumbers(bookId)
}