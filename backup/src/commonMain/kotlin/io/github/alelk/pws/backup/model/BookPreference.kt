package io.github.alelk.pws.backup.model

import io.github.alelk.pws.backup.model.serialization.BookExternalIdSerializer
import io.github.alelk.pws.domain.model.BookExternalId
import kotlinx.serialization.Serializable

@Serializable
data class BookPreference(
  @Serializable(with = BookExternalIdSerializer::class)
  val bookId: BookExternalId,
  val preference: Int
)
