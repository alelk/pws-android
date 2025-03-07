package io.github.alelk.pws.backup.model

import io.github.alelk.pws.domain.model.Locale
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

  /** Backup metadata.
   *
   * @property createdAt timestamp of backup
   * @property defaultLocale default locale of songs
   * @property version version of backup specification
   */
  @Serializable
  data class Metadata(
    val createdAt: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
    val defaultLocale: Locale? = null,
    val source: String? = null
  ) {
    val version: Int = 2
  }
}
