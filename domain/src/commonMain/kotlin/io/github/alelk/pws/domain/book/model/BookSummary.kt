package io.github.alelk.pws.domain.book.model

import io.github.alelk.pws.domain.core.Locale
import io.github.alelk.pws.domain.core.NonEmptyString
import io.github.alelk.pws.domain.core.Version
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.ids.SongNumberId

data class BookSummary(
    val id: BookId,
    val version: Version,
    val locale: Locale,
    val name: NonEmptyString,
    val displayShortName: NonEmptyString,
    val displayName: NonEmptyString,
    val countSongs: Int,
    val firstSongNumberId: SongNumberId,
    val enabled: Boolean,
    val priority: Int,
)