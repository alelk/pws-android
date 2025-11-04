package io.github.alelk.pws.domain.song.query

enum class SongSort {
  ById, // ascending by id
  ByIdDesc,
  ByName, // ascending by name
  ByNameDesc,
  ByNumber, // ascending by number within book
  ByNumberDesc
}