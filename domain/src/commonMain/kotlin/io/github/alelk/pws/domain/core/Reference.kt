package io.github.alelk.pws.domain.core

import io.github.alelk.pws.domain.core.ids.BookId
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable(with = SongRefReasonSerializer::class)
enum class SongRefReason(val identifier: String) {
  Variation("variation");

  companion object {
    fun fromIdentifier(identifier: String) =
      checkNotNull(entries.firstOrNull { it.identifier == identifier }) { "unknown ${SongRefReason::class.simpleName} identifier: $identifier" }
  }
}

@Serializable
@Polymorphic
sealed class Reference

@Serializable
@SerialName("bible-ref")
data class BibleRef(
  @SerialName("value")
  val text: String
) : Reference() {
  override fun toString(): String = text
}

@ConsistentCopyVisibility
@Serializable
@SerialName("song-ref")
data class SongRef internal constructor(
  val reason: SongRefReason,
  private val bookId: BookId,
  @SerialName("number")
  private val songNumber: Int,
  val volume: Int,
) : Reference() {
  constructor(reason: SongRefReason, number: SongNumber, volume: Int) : this(reason, number.bookId, number.number, volume)

  val number: SongNumber get() = SongNumber(bookId, songNumber)

  init {
    require(songNumber > 0) { "song number should be greater than 0" }
    require(volume > 0) { "volume should be greater than 0" }
    require(volume <= 100) { "volume should be less or equal than 100" }
  }
}