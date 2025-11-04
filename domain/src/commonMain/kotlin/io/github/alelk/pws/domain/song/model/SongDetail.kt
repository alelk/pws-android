package io.github.alelk.pws.domain.song.model

import io.github.alelk.pws.domain.bible.BibleRef
import io.github.alelk.pws.domain.core.Locale
import io.github.alelk.pws.domain.core.NonEmptyString
import io.github.alelk.pws.domain.core.Version
import io.github.alelk.pws.domain.core.Year
import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.person.Person
import io.github.alelk.pws.domain.song.lyric.Lyric
import io.github.alelk.pws.domain.tonality.Tonality

data class SongDetail(
    val id: SongId,
    val version: Version,
    val locale: Locale,
    val name: NonEmptyString,
    val lyric: Lyric,
    val author: Person?,
    val translator: Person? = null,
    val composer: Person? = null,
    val tonalities: List<Tonality>? = null,
    val year: Year? = null,
    val bibleRef: BibleRef? = null,
    val edited: Boolean = false,
)