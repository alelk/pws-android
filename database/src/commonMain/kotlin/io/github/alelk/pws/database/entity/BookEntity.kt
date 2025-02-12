package io.github.alelk.pws.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.alelk.pws.domain.model.BookId
import io.github.alelk.pws.domain.model.Locale
import io.github.alelk.pws.domain.model.Person
import io.github.alelk.pws.domain.model.Version
import io.github.alelk.pws.domain.model.Year

@Entity(tableName = "books")
data class BookEntity(
  @PrimaryKey @ColumnInfo(name = "id") val id: BookId,
  @ColumnInfo(name = "version") val version: Version,
  @ColumnInfo(name = "locale") val locale: Locale,
  @ColumnInfo(name = "name") val name: String,
  @ColumnInfo(name = "display_short_name") val displayShortName: String,
  @ColumnInfo(name = "display_name") val displayName: String,
  @ColumnInfo(name = "release_date") val releaseDate: Year? = null,
  @ColumnInfo(name = "authors") val authors: List<Person>? = null,
  @ColumnInfo(name = "creators") val creators: List<Person>? = null,
  @ColumnInfo(name = "reviewers") val reviewers: List<Person>? = null,
  @ColumnInfo(name = "editors") val editors: List<Person>? = null,
  @ColumnInfo(name = "description") val description: String? = null,
  @ColumnInfo(name = "preface") val preface: String? = null
)