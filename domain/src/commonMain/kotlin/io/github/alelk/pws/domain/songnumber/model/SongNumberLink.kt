package io.github.alelk.pws.domain.songnumber.model

import io.github.alelk.pws.domain.core.ids.SongId

/** Value object used for bulk replacement. */
data class SongNumberLink(val songId: SongId, val number: Int) {
  init { require(number > 0) { "song number must be > 0" } }
}