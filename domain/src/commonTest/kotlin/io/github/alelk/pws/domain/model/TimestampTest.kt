package io.github.alelk.pws.domain.model

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class TimestampTest : StringSpec({
  "parse timestamp" {
    val t = Timestamp.parse("2025-02-11 15:59:45")
    t.year shouldBe Year(2025)
    t.month shouldBe Month.FEBRUARY
    t.dayOfMonth shouldBe 11
    t.hours shouldBe 15
    t.minutes shouldBe 59
    t.seconds shouldBe 45
  }

  "convert timestamp to string" {
    val t = Timestamp(Date(Year(2025), Month.FEBRUARY, 11), Time(15, 59, 45))
    t.toString() shouldBe "2025-02-11 15:59:45"
  }
})