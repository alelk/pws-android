package io.github.alelk.pws.domain.book.model

import io.github.alelk.pws.domain.core.Locale
import io.github.alelk.pws.domain.core.NonEmptyString
import io.github.alelk.pws.domain.core.Version
import io.github.alelk.pws.domain.core.Year
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.ids.SongNumberId
import io.github.alelk.pws.domain.person.Person

data class BookDetail(
    val id: BookId,
    val version: Version,
    val locale: Locale,
    val name: NonEmptyString,
    val displayShortName: NonEmptyString,
    val displayName: NonEmptyString,
    val releaseDate: Year? = null,
    val authors: List<Person>? = null,
    val creators: List<Person>? = null,
    val reviewers: List<Person>? = null,
    val editors: List<Person>? = null,
    val description: String? = null,
    val preface: String? = null,
    val firstSongNumberId: SongNumberId,
    val countSongs: Int,
    val enabled: Boolean = true,
    val priority: Int = 0,
)