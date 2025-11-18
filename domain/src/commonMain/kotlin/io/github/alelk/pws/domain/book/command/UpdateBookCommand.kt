package io.github.alelk.pws.domain.book.command

import io.github.alelk.pws.domain.core.Locale
import io.github.alelk.pws.domain.core.NonEmptyString
import io.github.alelk.pws.domain.core.OptionalField
import io.github.alelk.pws.domain.core.Version
import io.github.alelk.pws.domain.core.Year
import io.github.alelk.pws.domain.core.ids.BookId

/** Patch-like update for Book fields. */
data class UpdateBookCommand(
  val id: BookId,
  val locale: Locale? = null,
  val name: NonEmptyString? = null,
  val displayShortName: NonEmptyString? = null,
  val displayName: NonEmptyString? = null,
  val releaseDate: OptionalField<Year?> = OptionalField.Unchanged,
  val description: OptionalField<String?> = OptionalField.Unchanged,
  val preface: OptionalField<String?> = OptionalField.Unchanged,
  val version: Version? = null,
  val expectedVersion: Version? = null,
  val enabled: Boolean? = null,
  val priority: Int? = null
) {

  fun hasChanges(): Boolean =
    locale != null ||
      name != null ||
      displayShortName != null ||
      displayName != null ||
      version != null ||
      enabled != null ||
      priority != null ||
      releaseDate is OptionalField.Set ||
      description is OptionalField.Set ||
      preface is OptionalField.Set
}

