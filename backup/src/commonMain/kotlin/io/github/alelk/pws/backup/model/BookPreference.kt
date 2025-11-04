package io.github.alelk.pws.backup.model

import io.github.alelk.pws.domain.core.ids.BookId
import kotlinx.serialization.Serializable

@Serializable
data class BookPreference(val bookId: BookId, val preference: Int)
