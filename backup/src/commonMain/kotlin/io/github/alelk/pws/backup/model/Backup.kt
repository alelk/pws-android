package io.github.alelk.pws.backup.model

import io.github.alelk.pws.backup.model.serialization.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class Backup(
  val metadata: Metadata = Metadata(),
  val songs: List<Song>? = null,
  val favorites: List<SongNumber>? = null,
  val tags: List<Tag>? = null,
  val bookPreferences: List<BookPreference>? = null,
  val settings: Map<String, String>? = null
) {

  @Serializable
  data class Metadata(
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime = LocalDateTime.now()
  ) {
    val version: Int = 1
  }
}
