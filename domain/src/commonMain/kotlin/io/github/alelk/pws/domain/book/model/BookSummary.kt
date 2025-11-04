package io.github.alelk.pws.domain.book.model

import io.github.alelk.pws.domain.core.Locale
import io.github.alelk.pws.domain.core.Version
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.ids.SongNumberId

data class BookSummary(
    val id: BookId,
    val version: Version,
    val locale: Locale,
    val name: String,
    val displayShortName: String,
    val displayName: String,
    val countSongs: Int,
    val firstSongNumberId: SongNumberId,
    val enabled: Boolean,
    val priority: Int,
) {
  init {
    require(name.isNotBlank()) { "book name must not be blank" }
    require(displayShortName.isNotBlank()) { "book display short name must not be blank" }
    require(displayName.isNotBlank()) { "book display name must not be blank" }
  }
}