package io.github.alelk.pws.domain.book

import io.github.alelk.pws.domain.book.query.BookQuery
import io.github.alelk.pws.domain.core.Locale
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class BookQueryTest : StringSpec({
  "empty query reports empty" {
    BookQuery.Empty.isEmpty() shouldBe true
  }
  "non-empty query reports not empty" {
    BookQuery(locale = Locale.RU).isEmpty() shouldBe false
  }
})

