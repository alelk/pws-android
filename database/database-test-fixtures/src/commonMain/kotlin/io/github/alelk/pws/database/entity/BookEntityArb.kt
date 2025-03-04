package io.github.alelk.pws.database.entity

import io.github.alelk.pws.domain.model.locale
import io.github.alelk.pws.domain.model.BookId
import io.github.alelk.pws.domain.model.Locale
import io.github.alelk.pws.domain.model.Person
import io.github.alelk.pws.domain.model.Version
import io.github.alelk.pws.domain.model.Year
import io.github.alelk.pws.domain.model.bookId
import io.github.alelk.pws.domain.model.person
import io.github.alelk.pws.domain.model.version
import io.github.alelk.pws.domain.model.year
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.az
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.removeEdgecases
import io.kotest.property.arbitrary.string

fun Arb.Companion.bookEntity(
  id: Arb<BookId> = Arb.bookId().removeEdgecases(),
  version: Arb<Version> = Arb.version(),
  locale: Arb<Locale> = Arb.locale(),
  name: Arb<String> = Arb.string(5..40, Codepoint.az()),
  displayShortName: Arb<String> = Arb.string(3..20, Codepoint.az()),
  displayName: Arb<String> = Arb.string(5..40, Codepoint.az()),
  releaseDate: Arb<Year?> = Arb.year().orNull(),
  authors: Arb<List<Person>?> = Arb.list(Arb.person(), 0..2).orNull(0.5),
  creators: Arb<List<Person>?> = Arb.list(Arb.person(), 0..2).orNull(0.5),
  reviewers: Arb<List<Person>?> = Arb.list(Arb.person(), 0..2).orNull(0.5),
  editors: Arb<List<Person>?> = Arb.list(Arb.person(), 0..2).orNull(0.5),
  description: Arb<String?> = Arb.string(3..200, Codepoint.az()).orNull(0.5),
  preface: Arb<String?> = Arb.string(3..200, Codepoint.az()).orNull(0.5)
): Arb<BookEntity> = arbitrary {
  BookEntity(
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
    preface = preface.bind()
  )
}