package io.github.alelk.pws.domain.book.command

import io.github.alelk.pws.domain.core.Locale
import io.github.alelk.pws.domain.core.Year
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.person.Person

/** Command to create a Book. */
data class CreateBookCommand(
    val id: BookId,               // pre-generated or external natural id
    val locale: Locale,
    val name: String,
    val displayShortName: String,
    val displayName: String,
    val releaseDate: Year? = null,
    val authors: List<Person> = emptyList(),
    val creators: List<Person> = emptyList(),
    val reviewers: List<Person> = emptyList(),
    val editors: List<Person> = emptyList(),
    val description: String? = null,
    val preface: String? = null
) {
  init {
    require(name.isNotBlank()) { "book name blank" }
    require(displayShortName.isNotBlank()) { "book display short name blank" }
    require(displayName.isNotBlank()) { "book display name blank" }
  }
}