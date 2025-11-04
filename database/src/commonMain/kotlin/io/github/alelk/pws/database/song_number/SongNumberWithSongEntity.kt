package io.github.alelk.pws.database.song_number

import androidx.room.Embedded
import androidx.room.Relation
import io.github.alelk.pws.database.song.SongEntity

data class SongNumberWithSongEntity(
    @Embedded
  val songNumber: SongNumberEntity,
    @Relation(parentColumn = "song_id", entityColumn = "id")
  val song: SongEntity,
)