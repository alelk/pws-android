package com.alelk.pws.pwapp.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.alelk.pws.database.DatabaseProvider
import com.alelk.pws.database.dao.Book
import com.alelk.pws.database.dao.BookDao
import com.alelk.pws.database.entity.SongNumberEntity
import kotlinx.coroutines.flow.Flow

class BooksViewModel(application: Application) : AndroidViewModel(application) {
  private val bookDao: BookDao = DatabaseProvider.getDatabase(application).bookDao()
  val allActiveBooks: LiveData<List<Book>> = bookDao.getAllActive().asLiveData()
  fun getBookSongNumbers(bookId: Long): Flow<List<SongNumberEntity>> = bookDao.getBookSongNumbers(bookId)
}