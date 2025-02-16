package io.github.alelk.pws.domain.model

import kotlin.jvm.JvmInline

@JvmInline
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