package io.github.alelk.pws.database.song_reference

import androidx.room.Embedded
import androidx.room.Relation
import io.github.alelk.pws.database.book.BookEntity
import io.github.alelk.pws.database.song_number.SongNumberEntity
import io.github.alelk.pws.database.song.SongEntity

/** Reference between two songs. */
data class SongReferenceDetailsEntity(
    @Embedded
  val songRef: SongReferenceEntity,
    @Relation(parentColumn = "ref_song_id", entityColumn = "id")
  val song: SongEntity,
    @Relation(parentColumn = "ref_song_id", entityColumn = "song_id")
  val songNumber: SongNumberEntity,
    @Embedded
  val book: BookEntity
)