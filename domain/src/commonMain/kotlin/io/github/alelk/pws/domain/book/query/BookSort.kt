package io.github.alelk.pws.domain.book.query

import io.github.alelk.pws.domain.book.model.BookSummary

enum class BookSort {
  ByName,
  ByNameDesc,
  ByPriority,
  ByPriorityDesc
}