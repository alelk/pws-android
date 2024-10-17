package com.alelk.pws.pwapp.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.alelk.pws.database.DatabaseProvider
import com.alelk.pws.database.dao.BookStatisticWithBook
import io.github.alelk.pws.database.common.entity.BookStatisticEntity
import io.github.alelk.pws.database.common.model.BookExternalId
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

class BookStatisticViewModel(application: Application) : AndroidViewModel(application) {
  private val bookStatisticDao = DatabaseProvider.getDatabase(application).bookStatisticDao()

  val bookStatistic: Flow<List<BookStatisticWithBook>> = bookStatisticDao.getAll()

  suspend fun update(bookId: BookExternalId, updateFn: suspend (existing: BookStatisticWithBook) -> BookStatisticEntity) {
    val existing = bookStatisticDao.getByBookExternalId(bookId)
    if (existing != null) {
      val bookStatistic = updateFn(existing)
      bookStatisticDao.update(bookStatistic)
      Timber.d("book statistic updated: bookId={}, userpref={}", bookStatistic.bookId, bookStatistic.userPreference)
    } else {
      Timber.d("no book statistic item fond by book id $bookId")
    }
  }
}