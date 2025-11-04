package io.github.alelk.pws.database.entity

import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.core.ids.TagId
import io.github.alelk.pws.domain.core.ids.songId
import io.github.alelk.pws.domain.core.ids.tagId
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary

fun Arb.Companion.songTagEntity(
  songId: Arb<SongId> = Arb.songId(),
  tagId: Arb<TagId> = Arb.tagId()
): Arb<SongTagEntity> =
  arbitrary { SongTagEntity(songId = songId.bind(), tagId = tagId.bind()) }