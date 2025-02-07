package io.github.alelk.pws.android.app.model

import androidx.lifecycle.ViewModel
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.dao.BookStatisticWithBook
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alelk.pws.database.common.entity.BookStatisticEntity
import io.github.alelk.pws.domain.model.BookExternalId
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class BookStatisticViewModel @Inject constructor(database: PwsDatabase) : ViewModel() {
  private val bookStatisticDao = database.bookStatisticDao()

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