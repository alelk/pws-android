package io.github.alelk.pws.database.common.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.alelk.pws.domain.model.Person
import io.github.alelk.pws.domain.model.Tonality
import io.github.alelk.pws.domain.model.Version
import io.github.alelk.pws.domain.model.Year
import java.util.Locale

@Entity(tableName = "psalms")
data class SongEntity(
  @PrimaryKey @ColumnInfo(name = "_id") val id: Long,
  @ColumnInfo(name = "version") val version: Version,
  @ColumnInfo(name = "locale") val locale: Locale,
  @ColumnInfo(name = "name") val name: String,
  @ColumnInfo(name = "text") val lyric: String,
  @ColumnInfo(name = "author") val author: Person? = null,
  @ColumnInfo(name = "translator") val translator: Person? = null,
  @ColumnInfo(name = "composer") val composer: Person? = null,
  @ColumnInfo(name = "tonalities") val tonalities: List<Tonality>? = null,
  @ColumnInfo(name = "year") val year: Year? = null,
  @ColumnInfo(name = "bibleref") val bibleRef: String? = null,
  @ColumnInfo(name = "edited", defaultValue = "false") val edited: Boolean = false,
)