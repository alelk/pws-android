package io.github.alelk.pws.domain.song.command

import io.github.alelk.pws.domain.core.NonEmptyString
import io.github.alelk.pws.domain.core.Version
import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.core.ids.TagId
import io.github.alelk.pws.domain.person.Person
import io.github.alelk.pws.domain.song.lyric.Lyric

/** Patch-like update for Song. */
data class UpdateSongCommand(
  val id: SongId,
  val version: Version,
  val name: NonEmptyString,
  val lyric: Lyric,
  val author: Person?,
  val translator: Person? = null,
  val composer: Person? = null,
  val addTags: Set<TagId> = emptySet(),
  val removeTags: Set<TagId> = emptySet(),
) {
  init {
    require(addTags.intersect(removeTags).isEmpty()) { "same tag in add and remove" }
  }
}

