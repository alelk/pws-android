package io.github.alelk.pws.database.common.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import io.github.alelk.pws.domain.model.TagId

@Entity(
  tableName = "song_number_tags",
  primaryKeys = ["song_number_id", "tag_id"],
  foreignKeys = [
    ForeignKey(entity = SongNumberEntity::class, parentColumns = ["_id"], childColumns = ["song_number_id"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tag_id"], onDelete = ForeignKey.CASCADE),
  ],
  indices = [
    Index(name = "idx_song_number_tags_song_number_id", value = ["song_number_id"]),
    Index(name = "idx_song_number_tags_tag_id", value = ["tag_id"]),
  ]
)
data class SongNumberTagEntity(
  @ColumnInfo(name = "song_number_id") val songNumberId: Long,
  @ColumnInfo(name = "tag_id") val tagId: TagId
)
