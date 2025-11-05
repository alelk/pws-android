package io.github.alelk.pws.domain.book.command

import io.github.alelk.pws.domain.core.Locale
import io.github.alelk.pws.domain.core.OptionalField
import io.github.alelk.pws.domain.core.Year
import io.github.alelk.pws.domain.core.ids.BookId

/** Patch-like update for Book fields. */
data class UpdateBookCommand(
  val id: BookId,
  val locale: Locale? = null,
  val name: String? = null,
  val displayShortName: String? = null,
  val displayName: String? = null,
  val releaseDate: OptionalField<Year?> = OptionalField.Unchanged,
  val description: OptionalField<String?> = OptionalField.Unchanged,
  val preface: OptionalField<String?> = OptionalField.Unchanged,
  val expectedVersion: Long? = null
)

