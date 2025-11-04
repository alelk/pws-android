package io.github.alelk.pws.domain.bible

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable(with = BibleRefSerializer::class)
value class BibleRef(val text: String) {
  init {
    require(text.isNotBlank()) { "bible ref is blank" }
  }

  override fun toString() = text
}