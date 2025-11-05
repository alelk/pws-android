package io.github.alelk.pws.domain.bookstatistic.repository

import io.github.alelk.pws.domain.bookstatistic.command.UpdateBookStatisticCommand
import io.github.alelk.pws.domain.bookstatistic.model.BookStatisticDetail
import io.github.alelk.pws.domain.bookstatistic.query.BookStatisticQuery
import io.github.alelk.pws.domain.core.ids.BookId
import kotlinx.coroutines.flow.Flow

interface BookStatisticRepository {
  fun observe(id: BookId): Flow<BookStatisticDetail?>
  fun observeMany(query: BookStatisticQuery = BookStatisticQuery.Companion.Empty): Flow<List<BookStatisticDetail>>

  suspend fun get(id: BookId): BookStatisticDetail?

  /** Apply patch and return updated statistic. */
  suspend fun update(command: UpdateBookStatisticCommand): BookStatisticDetail
}