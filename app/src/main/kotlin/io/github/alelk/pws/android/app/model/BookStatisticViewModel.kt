package io.github.alelk.pws.android.app.model

import androidx.lifecycle.ViewModel
import io.github.alelk.pws.database.PwsDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alelk.pws.database.entity.BookStatisticEntity
import io.github.alelk.pws.database.entity.BookStatisticWithBookEntity
import io.github.alelk.pws.domain.model.BookExternalId
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class BookStatisticViewModel @Inject constructor(database: PwsDatabase) : ViewModel() {
  private val bookStatisticDao = database.bookStatisticDao()

  val bookStatistic: Flow<List<BookStatisticWithBookEntity>> = bookStatisticDao.getAllBookStatisticWithBookFlow()

  suspend fun update(bookId: BookExternalId, updateFn: suspend (existing: BookStatisticWithBookEntity) -> BookStatisticEntity) {
    val existing = bookStatisticDao.getBookStatisticWithBookByBookExternalId(bookId)
    if (existing != null) {
      val bookStatistic = updateFn(existing)
      bookStatisticDao.update(bookStatistic)
      Timber.d("book statistic updated: bookId={}, userpref={}", bookStatistic.bookId, bookStatistic.userPreference)
    } else {
      Timber.d("no book statistic item fond by book id $bookId")
    }
  }
}