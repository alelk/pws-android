package io.github.alelk.pws.domain.model

import kotlin.jvm.JvmInline

@JvmInline
value class BibleRef(val text: String) {
  init {
    require(text.isNotBlank()) { "bible ref is blank" }
  }
}
