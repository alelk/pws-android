package io.github.alelk.pws.domain.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.checkAll

class ColorTest : StringSpec({

  "convert color to string and parse it back" {
      checkAll(10, Arb.Companion.color()) { color ->
          val hex = color.toString()
          Color.parse(hex) shouldBe color
      }
  }

  "convert color to hex string" {
    Color(100, 100, 100).toString() shouldBe "#646464"
  }
})