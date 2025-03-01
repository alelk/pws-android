package io.github.alelk.pws.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.checkAll

class BookIdTest : FeatureSpec({
  feature("creating book id") {
    scenario("from valid string contains latin letters and digits") {
      BookId.parse("Text-1234").identifier shouldBe "Text-1234"
    }

    scenario("from valid string contains cyrillic") {
      BookId.parse("Текст_1234").identifier shouldBe "Текст_1234"
    }

    scenario("fails when string starts from digit") {
      shouldThrow<IllegalArgumentException> {
        BookId.parse("1Book")
      }.message shouldContain "book id should"
    }

    scenario("fails when string ends with underscore") {
      shouldThrow<IllegalArgumentException> {
        BookId.parse("Book123_")
      }.message shouldContain "book id should"
    }
  }

  feature("convert book id to string and parse it back") {
    scenario("for random book id") {
      checkAll(Arb.bookId()) { bookId ->
        val string = bookId.toString()
        val parsed = BookId.parse(string)
        parsed shouldBe bookId
      }
    }
  }
})