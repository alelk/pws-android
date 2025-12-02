package io.github.alelk.pws.domain.book.query

import io.github.alelk.pws.domain.book.model.BookSummary

val BookSort.bookSummaryComparator: Comparator<BookSummary>
  get() = when (this) {
    BookSort.ByName -> compareBy { it.name.value }
    BookSort.ByNameDesc -> compareByDescending { it.name.value }
    BookSort.ByPriority -> compareBy { it.priority }
    BookSort.ByPriorityDesc -> compareByDescending { it.priority }
  }