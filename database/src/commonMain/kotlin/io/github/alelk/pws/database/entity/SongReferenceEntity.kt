package io.github.alelk.pws.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import io.github.alelk.pws.domain.core.ids.SongId

enum class SongRefReason(val identifier: String) {
  Variation("variation");

  companion object {
    fun fromIdentifier(identifier: String) =
      checkNotNull(entries.firstOrNull { it.identifier == identifier }) { "unknown ${SongRefReason::class.simpleName} identifier: $identifier" }
  }
}

/** Reference between two songs. */
@Entity(
  tableName = "song_references",
  primaryKeys = ["song_id", "ref_song_id"],
  foreignKeys = [
    ForeignKey(entity = SongEntity::class, parentColumns = ["id"], childColumns = ["song_id"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = SongEntity::class, parentColumns = ["id"], childColumns = ["ref_song_id"], onDelete = ForeignKey.CASCADE)
  ],
  indices = [
    Index(name = "idx_song_references_song_id", value = ["song_id"]),
    Index(name = "idx_song_references_ref_song_id", value = ["ref_song_id"])
  ]
)
data class SongReferenceEntity(
  @ColumnInfo(name = "song_id") val songId: SongId,
  @ColumnInfo(name = "ref_song_id") val refSongId: SongId,
  @ColumnInfo(name = "reason") val reason: SongRefReason,
  @ColumnInfo(name = "volume") val volume: Int,
  @ColumnInfo(name = "priority") val priority: Int = 0
) {
  init {
    require(songId != refSongId) { "song should not reference itself" }
  }
}
