package io.github.alelk.pws.domain.bookstatistic.model

import io.github.alelk.pws.domain.core.ids.BookId

data class BookStatisticDetail(
  val id: BookId,
  val priority: Int? = null,
  val readings: Int? = null,
  val rating: Int? = null,
)