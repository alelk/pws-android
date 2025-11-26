package io.github.alelk.pws.domain.songnumber.model

import io.github.alelk.pws.domain.core.ids.SongId
import kotlinx.serialization.Serializable

@Serializable
data class SongNumberLink(val songId: SongId, val number: Int) {
  init { require(number > 0) { "song number must be > 0" } }
}