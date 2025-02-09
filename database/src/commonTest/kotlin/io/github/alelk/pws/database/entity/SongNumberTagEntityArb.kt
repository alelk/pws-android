package io.github.alelk.pws.database.entity

import io.github.alelk.pws.domain.model.TagId
import io.github.alelk.pws.domain.model.tagId
import io.kotest.common.DelicateKotest
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.distinct
import io.kotest.property.arbitrary.long

@OptIn(DelicateKotest::class)
fun Arb.Companion.songNumberTagEntity(
  songNumberId: Arb<Long> = Arb.long(min = 1),
  tagId: Arb<TagId> = Arb.tagId()
): Arb<SongNumberTagEntity> =
  arbitrary { SongNumberTagEntity(songNumberId = songNumberId.bind(), tagId = tagId.bind()) }.distinct()