package io.github.alelk.pws.backup.model

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

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
    val createdAt: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
  ) {
    val version: Int = 1
  }
}
