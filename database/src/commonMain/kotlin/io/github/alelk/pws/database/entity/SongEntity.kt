package io.github.alelk.pws.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.alelk.pws.domain.model.Locale
import io.github.alelk.pws.domain.model.Person
import io.github.alelk.pws.domain.model.SongId
import io.github.alelk.pws.domain.model.Tonality
import io.github.alelk.pws.domain.model.Version
import io.github.alelk.pws.domain.model.Year

@Entity(tableName = "songs", indices = [Index(name = "idx_songs_edited", value = ["edited"])])
data class SongEntity(
  @PrimaryKey @ColumnInfo(name = "id") val id: SongId,
  @ColumnInfo(name = "version") val version: Version,
  @ColumnInfo(name = "locale") val locale: Locale,
  @ColumnInfo(name = "name") val name: String,
  @ColumnInfo(name = "lyric") val lyric: String,
  @ColumnInfo(name = "author") val author: Person? = null,
  @ColumnInfo(name = "translator") val translator: Person? = null,
  @ColumnInfo(name = "composer") val composer: Person? = null,
  @ColumnInfo(name = "tonalities") val tonalities: List<Tonality>? = null,
  @ColumnInfo(name = "year") val year: Year? = null,
  @ColumnInfo(name = "bibleref") val bibleRef: String? = null,
  @ColumnInfo(name = "edited", defaultValue = "false") val edited: Boolean = false,
)