package io.github.alelk.pws.domain.book.command

import io.github.alelk.pws.domain.core.Locale
import io.github.alelk.pws.domain.core.NonEmptyString
import io.github.alelk.pws.domain.core.Version
import io.github.alelk.pws.domain.core.Year
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.person.Person
import io.github.alelk.pws.domain.core.year
import io.github.alelk.pws.domain.core.version
import io.github.alelk.pws.domain.core.locale
import io.github.alelk.pws.domain.core.ids.bookId
import io.github.alelk.pws.domain.person.person
import io.github.alelk.pws.domain.core.nonEmptyString
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.az
import io.kotest.property.arbitrary.string

/** Arbitrary generator for CreateBookCommand */
fun Arb.Companion.createBookCommand(
  id: Arb<BookId> = Arb.bookId(),
  version: Arb<Version> = Arb.version(),
  locale: Arb<Locale> = Arb.locale(),
  name: Arb<NonEmptyString> = Arb.nonEmptyString(1..25),
  displayShortName: Arb<NonEmptyString> = name.map { NonEmptyString(it.value.take(5)) },
  displayName: Arb<NonEmptyString> = name,
  releaseDate: Arb<Year?> = Arb.year(1900, 2099).orNull(0.4),
  authors: Arb<List<Person>> = Arb.list(Arb.person(), 0..3),
  creators: Arb<List<Person>> = Arb.list(Arb.person(), 0..3),
  reviewers: Arb<List<Person>> = Arb.list(Arb.person(), 0..3),
  editors: Arb<List<Person>> = Arb.list(Arb.person(), 0..3),
  description: Arb<String?> = Arb.string(1..200, Codepoint.az()).orNull(0.5),
  preface: Arb<String?> = Arb.string(1..200, Codepoint.az()).orNull(0.5),
  enabled: Arb<Boolean> = Arb.boolean(),
  priority: Arb<Int> = Arb.int(0..20)
): Arb<CreateBookCommand> =
  arbitrary {
    val enabledValue = enabled.bind()
    CreateBookCommand(
      id = id.bind(),
      version = version.bind(),
      locale = locale.bind(),
      name = name.bind(),
      displayShortName = displayShortName.bind(),
      displayName = displayName.bind(),
      releaseDate = releaseDate.bind(),
      authors = authors.bind(),
      creators = creators.bind(),
      reviewers = reviewers.bind(),
      editors = editors.bind(),
      description = description.bind(),
      preface = preface.bind(),
      enabled = enabledValue,
      priority = priority.bind().let { p -> if (enabledValue && p == 0) 1 else p }
    )
  }
