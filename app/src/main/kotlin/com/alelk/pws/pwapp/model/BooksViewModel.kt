package com.alelk.pws.pwapp.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.alelk.pws.database.DatabaseProvider
import com.alelk.pws.database.dao.Book
import com.alelk.pws.database.dao.BookDao
import io.github.alelk.pws.database.common.entity.SongNumberEntity
import kotlinx.coroutines.flow.Flow

class BooksViewModel(application: Application) : AndroidViewModel(application) {
  private val bookDao: BookDao = DatabaseProvider.getDatabase(application).bookDao()

  val allActiveBooks: Flow<List<Book>> = bookDao.getAllActive()

  @Deprecated("use BookViewModel")
  fun getBookSongNumbers(bookId: Long): Flow<List<SongNumberEntity>> = bookDao.getBookSongNumbers(bookId)
}