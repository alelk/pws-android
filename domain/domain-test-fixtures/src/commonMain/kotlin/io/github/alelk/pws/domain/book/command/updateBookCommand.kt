package io.github.alelk.pws.domain.book.command

import io.github.alelk.pws.domain.core.Locale
import io.github.alelk.pws.domain.core.NonEmptyString
import io.github.alelk.pws.domain.core.OptionalField
import io.github.alelk.pws.domain.core.Version
import io.github.alelk.pws.domain.core.Year
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.ids.bookId
import io.github.alelk.pws.domain.core.locale
import io.github.alelk.pws.domain.core.nonEmptyString
import io.github.alelk.pws.domain.core.optionalField
import io.github.alelk.pws.domain.core.version
import io.github.alelk.pws.domain.core.year
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.orNull

/** Arbitrary for UpdateBookCommand (patch semantics). */
fun Arb.Companion.updateBookCommand(
  id: Arb<BookId> = Arb.bookId(),
  locale: Arb<Locale?> = Arb.locale().orNull(0.5),
  name: Arb<NonEmptyString?> = Arb.nonEmptyString(1..25).orNull(0.6),
  displayShortName: Arb<NonEmptyString?> = name.map { it?.let { NonEmptyString(it.value.take(5)) } },
  displayName: Arb<NonEmptyString?> = name,
  releaseDate: Arb<OptionalField<Year?>> = Arb.optionalField(Arb.year(1900, 2099).map { it as Year? }.orNull(0.2)),
  description: Arb<OptionalField<String?>> = Arb.optionalField(Arb.nonEmptyString(5..150).map { it.value }.orNull(0.5)),
  preface: Arb<OptionalField<String?>> = Arb.optionalField(Arb.nonEmptyString(5..150).map { it.value }.orNull(0.5)),
  version: Arb<Version?> = Arb.version().orNull(0.6),
  expectedVersion: Arb<Version?> = version.orNull(0.5),
  enabled: Arb<Boolean?> = Arb.boolean().orNull(0.7),
  priority: Arb<Int?> = Arb.int(0..20).orNull(0.5)
): Arb<UpdateBookCommand> = arbitrary {
  UpdateBookCommand(
    id = id.bind(),
    locale = locale.bind(),
    name = name.bind(),
    displayShortName = displayShortName.bind(),
    displayName = displayName.bind(),
    releaseDate = releaseDate.bind(),
    description = description.bind(),
    preface = preface.bind(),
    version = version.bind(),
    expectedVersion = expectedVersion.bind(),
    enabled = enabled.bind(),
    priority = priority.bind()
  )
}
