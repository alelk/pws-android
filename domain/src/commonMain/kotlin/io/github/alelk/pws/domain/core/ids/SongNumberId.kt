package io.github.alelk.pws.domain.core.ids

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

@JvmInline
@Serializable(with = SongNumberIdSerializer::class)
value class SongNumberId private constructor(val identifier: String) {

  constructor(bookId: BookId, songId: SongId) : this("$bookId/$songId")

  val bookId: BookId get() = BookId.parse(identifier.substringBefore('/'))
  val songId: SongId get() = SongId(identifier.substringAfter('/').toLong())

  override fun toString(): String = identifier

  companion object {
    fun parse(string: String): SongNumberId =
      runCatching {
        val (bookId, songId) = string.split('/')
        SongNumberId(BookId.parse(bookId), SongId(songId.toLong()))
      }.getOrElse { e ->
        throw IllegalArgumentException("unable to parse song number id from string '$string': expected format 'bookId/songId': ${e.message}", e)
      }
  }
}