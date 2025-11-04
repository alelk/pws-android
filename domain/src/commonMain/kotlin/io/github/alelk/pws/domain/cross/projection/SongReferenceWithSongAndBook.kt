package io.github.alelk.pws.domain.cross.projection

import io.github.alelk.pws.domain.book.model.BookSummary
import io.github.alelk.pws.domain.core.SongRefReason
import io.github.alelk.pws.domain.core.ids.SongNumberId
import io.github.alelk.pws.domain.song.model.SongSummary

data class SongReferenceWithSongAndBook(
    val reason: SongRefReason,
    val songNumberId: SongNumberId,
    val volume: Int,
    val songSummary: SongSummary,
    val bookSummary: BookSummary
)