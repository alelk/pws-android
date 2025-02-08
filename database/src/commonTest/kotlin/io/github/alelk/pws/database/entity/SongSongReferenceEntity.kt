package io.github.alelk.pws.database.entity

import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long

fun Arb.Companion.songSongReferenceEntity(
    id: Arb<Long?> = Arb.constant(null),
    songId: Arb<Long> = Arb.long(min = 1, max = 1_000_000),
    refSongId: Arb<Long> = Arb.long(min = 1, max = 1_000_000),
    reason: Arb<SongRefReason> = Arb.enum(),
    volume: Arb<Int> = Arb.int(min = 1, max = 100),
    priority: Arb<Int> = Arb.int(min = 1, max = 1000)
): Arb<SongSongReferenceEntity> = arbitrary {
    SongSongReferenceEntity(
        id = id.bind(),
        songId = songId.bind(),
        refSongId = refSongId.bind(),
        reason = reason.bind(),
        volume = volume.bind(),
        priority = priority.bind()
    )
}