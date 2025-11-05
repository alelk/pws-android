package io.github.alelk.pws.domain.core.ids

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable(with = SongIdSerializer::class)
value class SongId(val value: Long) {
  init {
    require(value >= 0) { "song id must not be negative" }
  }

  override fun toString(): String = value.toString()

  companion object {
    fun parse(string: String): SongId =
      SongId(requireNotNull(string.toLongOrNull()) { "song id must be a positive long number" })
  }
}