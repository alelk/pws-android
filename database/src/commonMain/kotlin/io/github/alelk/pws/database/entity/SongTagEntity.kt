package io.github.alelk.pws.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import io.github.alelk.pws.domain.model.SongId
import io.github.alelk.pws.domain.model.TagId

@Entity(
  tableName = "song_tags",
  primaryKeys = ["song_id", "tag_id"],
  foreignKeys = [
    ForeignKey(entity = SongEntity::class, parentColumns = ["id"], childColumns = ["song_id"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tag_id"], onDelete = ForeignKey.CASCADE),
  ],
  indices = [
    Index(name = "idx_song_number_tags_song_id", value = ["song_id"]),
    Index(name = "idx_song_number_tags_tag_id", value = ["tag_id"]),
  ]
)
data class SongTagEntity(
  @ColumnInfo(name = "song_id") val songId: SongId,
  @ColumnInfo(name = "tag_id") val tagId: TagId
)
