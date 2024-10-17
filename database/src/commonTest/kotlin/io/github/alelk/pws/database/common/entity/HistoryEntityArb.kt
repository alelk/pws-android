package io.github.alelk.pws.database.common.entity

import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.javaDate
import io.kotest.property.arbitrary.long
import java.util.Date

fun Arb.Companion.historyEntity(
  id: Arb<Long> = Arb.long(min = 1, max = 1_000_000),
  psalmNumberId: Arb<Long> = Arb.long(min = 1, max = 1_000_000),
  accessTimestamp: Arb<Date> = Arb.javaDate()
): Arb<HistoryEntity> = arbitrary {
  HistoryEntity(
    id = id.bind(),
    songNumberId = psalmNumberId.bind(),
    accessTimestamp = accessTimestamp.bind()
  )
}