package io.github.alelk.pws.domain.model

import io.github.alelk.pws.domain.model.serialization.DefaultTagIdSerializer
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable(with = DefaultTagIdSerializer::class)
value class TagId(private val identifier: String) {
  init {
    require(pattern.matches(identifier)) {
      "tag id should contain only letters, digits and '-', '_' symbols; should not start with digit; should not end with '-' or '_'"
    }
  }

  companion object {
    val pattern = Regex("""^\p{L}+([\p{L}\d_-]*[\p{L}\d])?$""")

    fun parse(identifier: String): TagId = TagId(identifier)
  }

  override fun toString(): String = identifier
}

fun String.toTagId(): TagId = TagId(this)