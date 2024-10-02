package com.alelk.pws.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index

enum class SongRefReason(val identifier: String) {
  Variation("variation");

  companion object {
    fun fromIdentifier(identifier: String) =
      checkNotNull(entries.firstOrNull { it.identifier == identifier }) { "unknown ${SongRefReason::class.simpleName} identifier: $identifier" }
  }
}

@Entity(
  tableName = "psalmpsalmreferences",
  foreignKeys = [
    ForeignKey(entity = SongEntity::class, parentColumns = ["_id"], childColumns = ["psalmid"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = SongEntity::class, parentColumns = ["_id"], childColumns = ["refpsalmid"], onDelete = ForeignKey.CASCADE)
  ],
  indices = [
    Index(name = "idx_psalmpsalmreferences_psalmid", value = ["psalmid"]),
    Index(name = "idx_psalmpsalmreferences_refpsalmid", value = ["refpsalmid"]),
    Index(name = "idx_psalmpsalmreferences_psalmid_refpsalmid", value = ["psalmid", "refpsalmid"], unique = true),
  ]
)
data class SongSongReferenceEntity(
  @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") val id: Long? = null,
  @ColumnInfo(name = "psalmid") val songId: Long,
  @ColumnInfo(name = "refpsalmid") val refSongId: Long,
  @ColumnInfo(name = "reason") val reason: SongRefReason,
  @ColumnInfo(name = "volume") val volume: Int,
  @ColumnInfo(name = "priority") val priority: Int = 0
)
