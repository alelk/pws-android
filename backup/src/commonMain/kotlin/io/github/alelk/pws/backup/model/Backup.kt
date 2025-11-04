package io.github.alelk.pws.backup.model

import io.github.alelk.pws.domain.core.Locale
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime

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
  data class Metadata @OptIn(ExperimentalTime::class) constructor(
    val createdAt: LocalDateTime = kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
    val defaultLocale: Locale? = null,
    val source: String? = null
  ) {
    val version: Int = 2
  }
}
