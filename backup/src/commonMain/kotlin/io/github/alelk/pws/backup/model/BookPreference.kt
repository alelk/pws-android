package io.github.alelk.pws.backup.model

import io.github.alelk.pws.backup.model.serialization.BookIdSerializer
import io.github.alelk.pws.domain.model.BookId
import kotlinx.serialization.Serializable

@Serializable
data class BookPreference(
  @Serializable(with = BookIdSerializer::class)
  val bookId: BookId,
  val preference: Int
)
