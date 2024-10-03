package com.alelk.pws.pwapp.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.alelk.pws.database.DatabaseProvider
import com.alelk.pws.database.dao.Book
import com.alelk.pws.database.dao.BookDao

class BookViewModel(application: Application) : AndroidViewModel(application) {
  private val bookDao: BookDao = DatabaseProvider.getDatabase(application).bookDao()
  val allActiveBooks: LiveData<List<Book>> = bookDao.getAllActive().asLiveData()
}