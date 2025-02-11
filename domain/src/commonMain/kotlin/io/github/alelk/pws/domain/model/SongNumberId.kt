package io.github.alelk.pws.domain.model

import kotlin.jvm.JvmInline

@JvmInline
value class SongNumberId private constructor(val identifier: String) {

  constructor(bookId: BookId, songId: SongId) : this("$bookId/$songId")

  val bookId: BookId get() = BookId.parse(identifier.substringBefore('/'))
  val songId: SongId get() = SongId(identifier.substringAfter('/').toLong())

  override fun toString(): String = identifier

  companion object {
    fun parse(string: String): SongNumberId {
      val (bookId, songId) = string.split('/')
      return kotlin.runCatching {
        SongNumberId(BookId.parse(bookId), SongId(songId.toLong()))
      }.onFailure { e ->
        throw IllegalArgumentException("unable to parse song number id from string '$string': expected format 'bookId/songId': ${e.message}", e)
      }.getOrThrow()
    }
  }
}
