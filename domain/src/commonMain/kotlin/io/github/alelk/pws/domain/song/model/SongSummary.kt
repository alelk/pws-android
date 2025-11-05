package io.github.alelk.pws.domain.song.model

import io.github.alelk.pws.domain.core.Locale
import io.github.alelk.pws.domain.core.NonEmptyString
import io.github.alelk.pws.domain.core.Version
import io.github.alelk.pws.domain.core.ids.SongId

data class SongSummary(
    val id: SongId,
    val version: Version,
    val locale: Locale,
    val name: NonEmptyString,
    val edited: Boolean = false,
)