package io.github.alelk.pws.domain.core.ids

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.checkAll

class SongIdTest : FeatureSpec({

  feature("create song id") {
    scenario("from valid long") {
      SongId(1).value shouldBe 1
    }

    scenario("from negative long (error)") {
        shouldThrow<IllegalArgumentException> {
            SongId(-1)
        }
    }
  }

  feature("convert song id to string and parse it back") {
    scenario("for random song id") {
        checkAll(Arb.Companion.songId()) { songId ->
            val string = songId.toString()
            val parsed = SongId.parse(string)
            parsed shouldBe songId
        }
    }
  }
})