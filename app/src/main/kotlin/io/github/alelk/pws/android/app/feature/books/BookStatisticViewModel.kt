package io.github.alelk.pws.android.app.feature.books

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.book_statistic.BookStatisticEntity
import io.github.alelk.pws.database.book_statistic.BookStatisticWithBookEntity
import io.github.alelk.pws.domain.core.ids.BookId
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class BookStatisticViewModel @Inject constructor(database: PwsDatabase) : ViewModel() {
  private val bookStatisticDao = database.bookStatisticDao()

  val bookStatistic: Flow<List<BookStatisticWithBookEntity>> = bookStatisticDao.getAllBookStatisticWithBookFlow()

  suspend fun update(bookId: BookId, updateFn: suspend (existing: BookStatisticWithBookEntity) -> BookStatisticEntity) {
    val existing = bookStatisticDao.getBookStatisticWithBookById(bookId)
    if (existing != null) {
      val bookStatistic = updateFn(existing)
      bookStatisticDao.update(bookStatistic)
      Timber.Forest.d("book statistic updated: bookId={}, userpref={}", bookStatistic.id, bookStatistic.priority)
    } else {
      Timber.Forest.d("no book statistic item fond by book id $bookId")
    }
  }
}