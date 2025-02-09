package io.github.alelk.pws.database.entity

data class SongSearchResultEntity(
  val songNumberId: Long,
  val songName: String,
  val songNumber: Int,
  val bookDisplayName: String,
  val snippet: String
)