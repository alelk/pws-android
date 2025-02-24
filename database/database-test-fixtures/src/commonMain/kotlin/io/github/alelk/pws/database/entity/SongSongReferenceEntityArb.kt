package io.github.alelk.pws.database.entity

import io.github.alelk.pws.domain.model.SongId
import io.github.alelk.pws.domain.model.songId
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.removeEdgecases

fun Arb.Companion.songSongReferenceEntity(
    songId: Arb<SongId> = Arb.songId().removeEdgecases(),
    refSongId: Arb<SongId> = Arb.songId().removeEdgecases(),
    reason: Arb<SongRefReason> = Arb.enum(),
    volume: Arb<Int> = Arb.int(min = 1, max = 100),
    priority: Arb<Int> = Arb.int(min = 1, max = 1000)
): Arb<SongReferenceEntity> =
  arbitrary {
    val songId1 = songId.bind()
    SongReferenceEntity(
      songId = songId1,
      refSongId = refSongId.filter { it != songId1 }.bind(),
      reason = reason.bind(),
      volume = volume.bind(),
      priority = priority.bind()
    )
  }