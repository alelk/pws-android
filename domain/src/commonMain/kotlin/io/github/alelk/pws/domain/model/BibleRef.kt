package io.github.alelk.pws.domain.model

import io.github.alelk.pws.domain.model.serialization.DefaultBibleRefSerializer
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable(with = DefaultBibleRefSerializer::class)
value class BibleRef(val text: String) {
  init {
    require(text.isNotBlank()) { "bible ref is blank" }
  }

  override fun toString() = text
}
