package io.github.alelk.pws.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class BookExternalIdTest : FeatureSpec({
  feature("creating book id") {
    scenario("from valid string contains latin letters and digits") {
      BookExternalId.parse("Text-1234").identifier shouldBe "Text-1234"
    }

    scenario("from valid string contains cyrillic") {
      BookExternalId.parse("Текст_1234").identifier shouldBe "Текст_1234"
    }

    scenario("fails when string starts from digit") {
      shouldThrow<IllegalArgumentException> {
        BookExternalId.parse("1Book")
      }.message shouldContain "Book external id should"
    }

    scenario("fails when string ends with underscore") {
      shouldThrow<IllegalArgumentException> {
        BookExternalId.parse("Book123_")
      }.message shouldContain "Book external id should"
    }
  }
})