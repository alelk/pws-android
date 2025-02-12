package io.github.alelk.pws.database.entity

import io.github.alelk.pws.domain.model.SongId
import io.github.alelk.pws.domain.model.TagId
import io.github.alelk.pws.domain.model.songId
import io.github.alelk.pws.domain.model.tagId
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.removeEdgecases

fun Arb.Companion.songTagEntity(
  songId: Arb<SongId> = Arb.songId(),
  tagId: Arb<TagId> = Arb.tagId()
): Arb<SongTagEntity> =
  arbitrary { SongTagEntity(songId = songId.bind(), tagId = tagId.bind()) }