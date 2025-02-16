package io.github.alelk.pws.backup.model

import io.github.alelk.pws.backup.model.serialization.BookIdSerializer
import io.github.alelk.pws.backup.model.serialization.ColorSerializer
import io.github.alelk.pws.domain.model.BookId
import io.github.alelk.pws.domain.model.Color
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Tag private constructor(
  val name: String,
  @Serializable(with = ColorSerializer::class)
  val color: Color,
  @SerialName("songs")
  private val songNumbers: Map<@Serializable(with = BookIdSerializer::class) BookId, List<Numbers>>
) {
  init {
    require(name.isNotBlank()) { "tag name should not be blank" }
  }

  val songs: Set<SongNumber> get() = songNumbers.flatMap { (bookId, numbers) -> numbers.flatMap { it.get() }.map { SongNumber(bookId, it) } }.toSet()

  constructor(name: String, color: Color, songs: Set<SongNumber>) : this(
    name = name,
    color = color,
    songNumbers = songs.groupBy { it.bookId }.map { (bookId, numbers) -> bookId to Numbers(numbers.map { it.number }) }.toMap()
  )
}