package io.github.alelk.pws.backup.model

import io.github.alelk.pws.domain.model.BookId
import kotlinx.serialization.Serializable

@Serializable
data class BookPreference(val bookId: BookId, val preference: Int)
