package io.github.alelk.pws.database.song_tag

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import io.github.alelk.pws.database.song.SongEntity
import io.github.alelk.pws.database.tag.TagEntity
import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.core.ids.TagId

@Entity(
  tableName = "song_tags",
  primaryKeys = ["song_id", "tag_id"],
  foreignKeys = [
      ForeignKey(
          entity = SongEntity::class,
          parentColumns = ["id"],
          childColumns = ["song_id"],
          onDelete = ForeignKey.Companion.CASCADE
      ),
      ForeignKey(
          entity = TagEntity::class,
          parentColumns = ["id"],
          childColumns = ["tag_id"],
          onDelete = ForeignKey.Companion.CASCADE
      ),
  ],
  indices = [
      Index(name = "idx_song_number_tags_song_id", value = ["song_id"]),
      Index(name = "idx_song_number_tags_tag_id", value = ["tag_id"]),
  ]
)
data class SongTagEntity(
    @ColumnInfo(name = "song_id") val songId: SongId,
    @ColumnInfo(name = "tag_id") val tagId: TagId,
    @ColumnInfo(name = "priority") val priority: Int = 0
)