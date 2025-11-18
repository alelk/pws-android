package io.github.alelk.pws.domain.book.command

import io.github.alelk.pws.domain.core.Locale
import io.github.alelk.pws.domain.core.NonEmptyString
import io.github.alelk.pws.domain.core.Version
import io.github.alelk.pws.domain.core.Year
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.person.Person

/** Command to create a Book. */
data class CreateBookCommand(
  val id: BookId,
  val version: Version = Version(1, 0),
  val locale: Locale,
  val name: NonEmptyString,
  val displayShortName: NonEmptyString,
  val displayName: NonEmptyString,
  val releaseDate: Year? = null,
  val authors: List<Person> = emptyList(),
  val creators: List<Person> = emptyList(),
  val reviewers: List<Person> = emptyList(),
  val editors: List<Person> = emptyList(),
  val description: String? = null,
  val preface: String? = null,
  val enabled: Boolean = true,
  val priority: Int = if (enabled) 10 else 0
)